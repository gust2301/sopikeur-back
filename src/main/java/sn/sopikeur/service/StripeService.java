package sn.sopikeur.service;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import sn.sopikeur.common.error.NotFoundException;
import sn.sopikeur.config.AppProperties;
import sn.sopikeur.dto.request.publicapi.stripe.StripeCheckoutRequest;
import sn.sopikeur.dto.response.publicapi.stripe.StripeCheckoutResponse;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.order.OrderPaymentPlan;
import sn.sopikeur.entity.payment.PaymentEventEntity;
import sn.sopikeur.entity.payment.PaymentIntentEntity;
import sn.sopikeur.entity.payment.PaymentIntentStatus;
import sn.sopikeur.entity.payment.PaymentPurpose;
import sn.sopikeur.entity.payment.PaymentStatus;
import sn.sopikeur.repo.order.OrderRepository;
import sn.sopikeur.repo.payment.PaymentEventRepository;
import sn.sopikeur.repo.payment.PaymentIntentRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    private final AppProperties props;
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final OrderRepository orderRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = props.getStripe().getSecretKey();
        log.info("Stripe SDK initialized (currency={})", props.getStripe().getCurrency());
    }

    // ─── Création session Checkout (à partir d'un StripeCheckoutRequest) ──────

    @Transactional
    public StripeCheckoutResponse createCheckoutSession(StripeCheckoutRequest request) {
        OrderEntity order = orderRepository.findByPublicId(request.getOrderId())
            .orElseThrow(() -> new NotFoundException("Commande introuvable: " + request.getOrderId()));

        AppProperties.Stripe cfg = props.getStripe();
        String name = (request.getDescription() != null && !request.getDescription().isBlank())
            ? request.getDescription()
            : "Paiement Sopikeur";

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(cfg.getSuccessUrl())
                .setCancelUrl(cfg.getCancelUrl())
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(cfg.getCurrency())
                                .setUnitAmount(request.getAmountXof())
                                .setProductData(
                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(name)
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .putMetadata("order_id", request.getOrderId())
                .putMetadata("purpose", request.getPurpose())
                .build();

            Session session = Session.create(params);

            PaymentIntentEntity pi = new PaymentIntentEntity();
            pi.setPublicId(UUID.randomUUID().toString());
            pi.setStripeSessionId(session.getId());
            pi.setOrder(order);
            pi.setAmount(request.getAmountXof());
            pi.setCurrency(cfg.getCurrency());
            pi.setPurpose(PaymentPurpose.valueOf(request.getPurpose().toUpperCase(Locale.ROOT)));
            pi.setStatus(PaymentIntentStatus.PENDING);
            pi.setCheckoutUrl(session.getUrl());
            paymentIntentRepository.save(pi);

            log.info("Checkout session created: stripeSessionId={} orderId={}", session.getId(), request.getOrderId());

            return StripeCheckoutResponse.builder()
                .checkoutUrl(session.getUrl())
                .paymentIntentId(pi.getPublicId())
                .build();

        } catch (StripeException e) {
            log.error("Stripe error creating session: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la création de la session Stripe: " + e.getMessage(), e);
        }
    }

    // ─── Création session Checkout depuis le publicId de la commande ──────────

    /**
     * Creates a Stripe Checkout session for an existing order based on its payment plan.
     * DEPOSIT_50 → charges 50% of the order total.
     * FULL_ONLINE → charges 100% of the order total.
     * CASH_ON_DELIVERY → returns HTTP 400 (no online payment needed).
     */
    @Transactional
    public StripeCheckoutResponse createOrderPaymentSession(String orderPublicId) {
        OrderEntity order = orderRepository.findByPublicId(orderPublicId)
            .orElseThrow(() -> new NotFoundException("Commande introuvable: " + orderPublicId));

        OrderPaymentPlan plan = order.getPaymentPlan() != null
            ? order.getPaymentPlan() : OrderPaymentPlan.CASH_ON_DELIVERY;

        BigDecimal amountToCharge;
        PaymentPurpose purpose;
        String description;

        switch (plan) {
            case DEPOSIT_50 -> {
                BigDecimal dep = order.getDepositAmount();
                if (dep == null && order.getAmountTotal() != null) {
                    dep = order.getAmountTotal()
                        .multiply(BigDecimal.valueOf(0.5))
                        .setScale(0, RoundingMode.HALF_UP);
                } else if (dep == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Le montant total de la commande est manquant");
                }
                amountToCharge = dep;
                purpose = PaymentPurpose.DEPOSIT;
                description = "Acompte 50% — " + coalesce(order.getOrderNumber(), order.getPublicId());
            }
            case FULL_ONLINE -> {
                if (order.getAmountTotal() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Le montant total de la commande est manquant");
                }
                amountToCharge = order.getAmountTotal();
                purpose = PaymentPurpose.FULL;
                description = "Paiement — " + coalesce(order.getOrderNumber(), order.getPublicId());
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Ce plan de paiement ne requiert pas de paiement en ligne");
        }

        StripeCheckoutRequest req = new StripeCheckoutRequest();
        req.setOrderId(order.getPublicId());
        req.setPurpose(purpose.name());
        req.setAmountXof(amountToCharge.longValue());
        req.setDescription(description);
        return createCheckoutSession(req);
    }

    // ─── Traitement webhook ───────────────────────────────────────────────────

    @Transactional
    public void processWebhookEvent(byte[] rawBody, String sigHeader) {
        AppProperties.Stripe cfg = props.getStripe();
        String payload = new String(rawBody, StandardCharsets.UTF_8);

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, cfg.getWebhookSecret(), cfg.getWebhookToleranceSeconds());
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid Stripe signature");
        }

        log.info("Stripe webhook received: type={} id={}", event.getType(), event.getId());

        // Idempotency : si l'event est déjà traité, on ignore
        if (paymentEventRepository.existsByStripeEventId(event.getId())) {
            log.info("Event {} already processed, skipping", event.getId());
            return;
        }

        handleEvent(event);
        saveEventRecord(event, payload);
    }

    // ─── Handlers par type d'event ────────────────────────────────────────────

    private void handleEvent(Event event) {
        switch (event.getType()) {
            case "checkout.session.completed" -> {
                Session session = extractSession(event);
                if (session == null) return;
                paymentIntentRepository.findByStripeSessionId(session.getId()).ifPresent(pi -> {
                    pi.setStatus(PaymentIntentStatus.SUCCEEDED);
                    if (session.getPaymentIntent() != null) {
                        pi.setStripePaymentIntentId(session.getPaymentIntent());
                    }
                    paymentIntentRepository.save(pi);

                    // Mise à jour des montants et du statut de paiement de la commande
                    if (pi.getOrder() != null) {
                        OrderEntity order = pi.getOrder();
                        BigDecimal paid = BigDecimal.valueOf(pi.getAmount());
                        BigDecimal currentPaid = order.getAmountPaid() != null ? order.getAmountPaid() : BigDecimal.ZERO;
                        BigDecimal newAmountPaid = currentPaid.add(paid);
                        order.setAmountPaid(newAmountPaid);

                        BigDecimal total = order.getAmountTotal() != null ? order.getAmountTotal() : paid;
                        BigDecimal newAmountDue = total.subtract(newAmountPaid).max(BigDecimal.ZERO);
                        order.setAmountDue(newAmountDue);

                        // Determine payment status
                        if (pi.getPurpose() == PaymentPurpose.FULL
                                || newAmountDue.compareTo(BigDecimal.ZERO) == 0) {
                            order.setPaymentStatus(PaymentStatus.PAID);
                        } else {
                            order.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
                        }

                        // Keep legacy status in sync
                        if (order.getOrderStatus() != null) {
                            order.setLegacyStatus(order.getOrderStatus().name());
                        }

                        orderRepository.save(order);
                        log.info("Order {} payment updated: amountPaid={} amountDue={} paymentStatus={}",
                            order.getPublicId(), newAmountPaid, newAmountDue, order.getPaymentStatus());
                    }
                });
            }
            case "checkout.session.expired" -> {
                Session session = extractSession(event);
                if (session == null) return;
                paymentIntentRepository.findByStripeSessionId(session.getId()).ifPresent(pi -> {
                    pi.setStatus(PaymentIntentStatus.EXPIRED);
                    paymentIntentRepository.save(pi);
                    log.info("PaymentIntent {} expired (stripeSession={})", pi.getPublicId(), session.getId());
                });
            }
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private void saveEventRecord(Event event, String rawPayload) {
        String sessionId = extractSessionId(event);
        if (sessionId == null) return;

        paymentIntentRepository.findByStripeSessionId(sessionId).ifPresent(pi -> {
            PaymentEventEntity rec = new PaymentEventEntity();
            rec.setPaymentIntent(pi);
            rec.setStripeEventId(event.getId());
            rec.setEventType(event.getType());
            rec.setPayload(rawPayload);
            rec.setProcessedAt(OffsetDateTime.now());
            paymentEventRepository.save(rec);
        });
    }

    /**
     * Extrait la Session Stripe depuis l'event.
     *
     * 1. Tente la désérialisation standard du SDK (rapide, sans appel réseau).
     *    Peut échouer silencieusement si la version API Stripe du dashboard
     *    ne correspond pas à celle compilée dans le SDK Java.
     *
     * 2. Fallback : lit l'ID depuis le JSON brut, puis appelle Session.retrieve()
     *    sur l'API Stripe — toujours fiable quelle que soit la version.
     */
    private Session extractSession(Event event) {
        // Tentative 1 : désérialisation SDK standard
        var obj = event.getDataObjectDeserializer().getObject();
        if (obj.isPresent() && obj.get() instanceof Session s) {
            return s;
        }

        // Tentative 2 : JSON brut → Session.retrieve() (robuste aux écarts de version)
        try {
            String rawJson = event.getDataObjectDeserializer().getRawJson();
            if (rawJson == null || rawJson.isBlank()) {
                log.warn("Stripe event {} : objet non désérialisable et JSON brut absent", event.getId());
                return null;
            }
            com.google.gson.JsonObject data =
                com.google.gson.JsonParser.parseString(rawJson).getAsJsonObject();
            String sessionId = data.get("id").getAsString();
            log.info("extractSession: fallback Session.retrieve({}) (event={})", sessionId, event.getId());
            return Session.retrieve(sessionId);
        } catch (StripeException e) {
            log.warn("extractSession: Session.retrieve échoué pour event {}: {}", event.getId(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("extractSession: parsing JSON brut échoué pour event {}: {}", event.getId(), e.getMessage());
            return null;
        }
    }

    private String extractSessionId(Event event) {
        if (event.getType() != null && event.getType().startsWith("checkout.session")) {
            Session s = extractSession(event);
            return s != null ? s.getId() : null;
        }
        return null;
    }

    private String coalesce(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }
}
