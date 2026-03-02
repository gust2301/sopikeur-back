package sn.sopikeur.service.payment;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import sn.sopikeur.common.error.NotFoundException;
import sn.sopikeur.dto.payment.ProviderCheckoutResultDto;
import sn.sopikeur.dto.payment.WebhookVerificationResultDto;
import sn.sopikeur.dto.response.publicapi.stripe.StripeCheckoutResponse;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.order.OrderPaymentPlan;
import sn.sopikeur.entity.order.OrderStatus;
import sn.sopikeur.entity.payment.PaymentEventEntity;
import sn.sopikeur.entity.payment.PaymentIntentEntity;
import sn.sopikeur.entity.payment.PaymentIntentStatus;
import sn.sopikeur.entity.payment.PaymentProvider;
import sn.sopikeur.entity.payment.PaymentPurpose;
import sn.sopikeur.entity.payment.PaymentStatus;
import sn.sopikeur.repo.order.OrderRepository;
import sn.sopikeur.repo.payment.PaymentEventRepository;
import sn.sopikeur.repo.payment.PaymentIntentRepository;
import sn.sopikeur.service.NotificationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orchestrateur de paiement multi-provider.
 * Route les appels au bon PaymentProviderClient selon le provider demandé.
 * Centralise la logique de mise à jour des commandes après paiement confirmé.
 */
@Slf4j
@Service
public class PaymentOrchestratorService {

    private final Map<PaymentProvider, PaymentProviderClient> providerClients;
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    public PaymentOrchestratorService(
            List<PaymentProviderClient> clients,
            PaymentIntentRepository paymentIntentRepository,
            PaymentEventRepository paymentEventRepository,
            OrderRepository orderRepository,
            NotificationService notificationService
    ) {
        this.providerClients = clients.stream()
                .collect(Collectors.toMap(PaymentProviderClient::getProvider, Function.identity()));
        this.paymentIntentRepository = paymentIntentRepository;
        this.paymentEventRepository  = paymentEventRepository;
        this.orderRepository         = orderRepository;
        this.notificationService     = notificationService;
        log.info("PaymentOrchestratorService initialized with providers: {}", this.providerClients.keySet());
    }

    // ─── Création session de paiement ─────────────────────────────────────────

    /**
     * Crée une session de paiement pour une commande existante.
     * Calcule le montant selon le plan de paiement et le purpose demandé.
     *
     * @param orderPublicId publicId de la commande
     * @param providerName  STRIPE | WAVE | ORANGE_MONEY
     * @param purposeStr    DEPOSIT | FULL | BALANCE
     */
    @Transactional
    public StripeCheckoutResponse createOrderPayment(String orderPublicId, String providerName, String purposeStr) {
        OrderEntity order = orderRepository.findByPublicId(orderPublicId)
                .orElseThrow(() -> new NotFoundException("Commande introuvable: " + orderPublicId));

        PaymentProvider provider = parseProvider(providerName);
        PaymentPurpose purpose   = parsePurpose(purposeStr);

        long amountToCharge = computeAmount(order, purpose);

        // Créer le PaymentIntentEntity
        PaymentIntentEntity intent = new PaymentIntentEntity();
        intent.setPublicId(UUID.randomUUID().toString());
        intent.setProvider(provider.name());
        intent.setOrder(order);
        intent.setAmount(amountToCharge);
        intent.setCurrency("XOF");
        intent.setPurpose(purpose);
        intent.setStatus(PaymentIntentStatus.PENDING);
        intent = paymentIntentRepository.save(intent);

        // Appeler le provider
        PaymentProviderClient client = getClient(provider);
        ProviderCheckoutResultDto result = client.createCheckout(intent);

        // Mettre à jour l'intent avec les identifiants provider
        intent.setCheckoutUrl(result.checkoutUrl());
        intent.setProviderCheckoutId(result.providerCheckoutId());
        intent.setProviderPaymentRef(result.providerPaymentRef());

        // Pour Stripe : conserver la compatibilité stripeSessionId
        if (provider == PaymentProvider.STRIPE) {
            intent.setStripeSessionId(result.providerCheckoutId());
            intent.setStripePaymentIntentId(result.providerPaymentRef());
        }

        paymentIntentRepository.save(intent);

        log.info("Payment session created: provider={} intentId={} orderId={} amount={}",
                provider, intent.getPublicId(), orderPublicId, amountToCharge);

        return StripeCheckoutResponse.builder()
                .checkoutUrl(result.checkoutUrl())
                .paymentIntentId(intent.getPublicId())
                .build();
    }

    // ─── Traitement webhook ───────────────────────────────────────────────────

    /**
     * Reçoit et traite un webhook de paiement pour un provider donné.
     * Garantit l'idempotency via (provider, provider_event_id).
     */
    @Transactional
    public void processWebhookEvent(String providerName, String rawPayload, Map<String, String> headers) {
        PaymentProvider provider = parseProvider(providerName);
        PaymentProviderClient client = getClient(provider);

        WebhookVerificationResultDto result = client.verifyWebhook(rawPayload, headers);

        if (!result.valid()) {
            log.warn("Invalid webhook from provider {}", providerName);
            throw new IllegalArgumentException("Invalid webhook signature for provider: " + providerName);
        }

        if (result.providerEventId() == null) {
            log.warn("Webhook from {} has no event ID — skipping idempotency check", providerName);
        } else if (paymentEventRepository.existsByProviderAndProviderEventId(providerName, result.providerEventId())) {
            log.info("Webhook event {}/{} already processed — skipping", providerName, result.providerEventId());
            return;
        }

        // Trouver le PaymentIntent
        if (result.providerCheckoutId() == null) {
            log.warn("Webhook from {} has no providerCheckoutId — cannot find PaymentIntent", providerName);
            return;
        }

        Optional<PaymentIntentEntity> intentOpt = paymentIntentRepository
                .findByProviderAndProviderCheckoutId(providerName, result.providerCheckoutId());

        // Fallback Stripe : utiliser stripeSessionId
        if (intentOpt.isEmpty() && provider == PaymentProvider.STRIPE) {
            intentOpt = paymentIntentRepository.findByStripeSessionId(result.providerCheckoutId());
        }

        if (intentOpt.isEmpty()) {
            log.warn("No PaymentIntent found for provider={} checkoutId={}", providerName, result.providerCheckoutId());
            return;
        }

        PaymentIntentEntity intent = intentOpt.get();

        // Mettre à jour l'intent
        if (result.status() != null) {
            intent.setStatus(result.status());
        }

        paymentIntentRepository.save(intent);

        // Mettre à jour la commande
        if (intent.getOrder() != null && result.status() != null) {
            updateOrderAfterPayment(intent, result.status());
        }

        // Sauvegarder l'événement (audit)
        saveWebhookEvent(intent, providerName, result.providerEventId(), result.eventType(), rawPayload);
    }

    // ─── Utilitaires internes ─────────────────────────────────────────────────

    private void updateOrderAfterPayment(PaymentIntentEntity intent, PaymentIntentStatus newStatus) {
        OrderEntity order = intent.getOrder();

        if (newStatus == PaymentIntentStatus.SUCCEEDED) {
            BigDecimal paid = BigDecimal.valueOf(intent.getAmount());
            BigDecimal currentPaid = order.getAmountPaid() != null ? order.getAmountPaid() : BigDecimal.ZERO;
            BigDecimal newAmountPaid = currentPaid.add(paid);
            order.setAmountPaid(newAmountPaid);

            BigDecimal total = order.getAmountTotal() != null ? order.getAmountTotal() : paid;
            BigDecimal newAmountDue = total.subtract(newAmountPaid).max(BigDecimal.ZERO);
            order.setAmountDue(newAmountDue);

            if (intent.getPurpose() == PaymentPurpose.FULL
                    || newAmountDue.compareTo(BigDecimal.ZERO) == 0) {
                order.setPaymentStatus(PaymentStatus.PAID);
            } else {
                order.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
            }

            if (order.getOrderStatus() == OrderStatus.DRAFT_PENDING_PAYMENT) {
                order.setOrderStatus(OrderStatus.SUBMITTED);
                order.setLegacyStatus("SUBMITTED");
                log.info("Draft order {} promoted to SUBMITTED after {} payment",
                        order.getPublicId(), intent.getProvider());
            }

            orderRepository.save(order);
            log.info("Order {} updated: amountPaid={} amountDue={} paymentStatus={}",
                    order.getPublicId(), newAmountPaid, newAmountDue, order.getPaymentStatus());

            // Notifier l'admin
            if (OrderStatus.SUBMITTED.equals(order.getOrderStatus())) {
                try {
                    notificationService.notifyOrderCreated(order);
                } catch (Exception e) {
                    log.warn("Notification failed for order {}: {}", order.getPublicId(), e.getMessage());
                }
            }

        } else if (newStatus == PaymentIntentStatus.EXPIRED || newStatus == PaymentIntentStatus.FAILED) {
            if (order.getOrderStatus() == OrderStatus.DRAFT_PENDING_PAYMENT) {
                order.setOrderStatus(OrderStatus.CANCELED);
                order.setLegacyStatus("CANCELED");
                orderRepository.save(order);
                log.info("Draft order {} canceled ({} {})", order.getPublicId(), intent.getProvider(), newStatus);
            }
        }
    }

    private void saveWebhookEvent(PaymentIntentEntity intent, String provider,
                                   String eventId, String eventType, String rawPayload) {
        PaymentEventEntity event = new PaymentEventEntity();
        event.setPaymentIntent(intent);
        event.setProvider(provider);
        event.setProviderEventId(eventId);
        event.setEventType(eventType != null ? eventType : "unknown");
        event.setPayload(rawPayload);
        event.setProcessedAt(OffsetDateTime.now());

        // Pour Stripe : conserver stripeEventId pour la compatibilité
        if (PaymentProvider.STRIPE.name().equals(provider) && eventId != null) {
            event.setStripeEventId(eventId);
        }

        paymentEventRepository.save(event);
    }

    private long computeAmount(OrderEntity order, PaymentPurpose purpose) {
        OrderPaymentPlan plan = order.getPaymentPlan() != null
                ? order.getPaymentPlan() : OrderPaymentPlan.CASH_ON_DELIVERY;

        return switch (purpose) {
            case DEPOSIT -> {
                BigDecimal dep = order.getDepositAmount();
                if (dep == null && order.getAmountTotal() != null) {
                    dep = order.getAmountTotal()
                            .multiply(BigDecimal.valueOf(0.5))
                            .setScale(0, RoundingMode.HALF_UP);
                } else if (dep == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Montant total de la commande manquant");
                }
                yield dep.longValue();
            }
            case FULL -> {
                if (order.getAmountTotal() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Montant total de la commande manquant");
                }
                yield order.getAmountTotal().longValue();
            }
            case BALANCE -> {
                BigDecimal due = order.getAmountDue();
                if (due == null || due.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Aucun solde restant à encaisser pour cette commande");
                }
                yield due.longValue();
            }
        };
    }

    private PaymentProviderClient getClient(PaymentProvider provider) {
        PaymentProviderClient client = providerClients.get(provider);
        if (client == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provider de paiement non supporté: " + provider);
        }
        return client;
    }

    private PaymentProvider parseProvider(String name) {
        try {
            return PaymentProvider.valueOf(name.toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provider inconnu: " + name + ". Valeurs acceptées: STRIPE, WAVE, ORANGE_MONEY");
        }
    }

    private PaymentPurpose parsePurpose(String name) {
        try {
            return PaymentPurpose.valueOf(name.toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Purpose inconnu: " + name + ". Valeurs acceptées: DEPOSIT, FULL, BALANCE");
        }
    }
}
