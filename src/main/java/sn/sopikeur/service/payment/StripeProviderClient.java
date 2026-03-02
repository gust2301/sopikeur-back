package sn.sopikeur.service.payment;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sn.sopikeur.config.AppProperties;
import sn.sopikeur.dto.payment.ProviderCheckoutResultDto;
import sn.sopikeur.dto.payment.ProviderPaymentStatusDto;
import sn.sopikeur.dto.payment.WebhookVerificationResultDto;
import sn.sopikeur.entity.payment.PaymentIntentEntity;
import sn.sopikeur.entity.payment.PaymentIntentStatus;
import sn.sopikeur.entity.payment.PaymentProvider;

import java.util.Map;

/**
 * Implémentation PaymentProviderClient pour Stripe.
 * Extrait la logique de création de session et de vérification webhook de StripeService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StripeProviderClient implements PaymentProviderClient {

    private final AppProperties props;

    @PostConstruct
    public void initStripe() {
        Stripe.apiKey = props.getStripe().getSecretKey();
        log.info("StripeProviderClient initialized");
    }

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.STRIPE;
    }

    @Override
    public ProviderCheckoutResultDto createCheckout(PaymentIntentEntity intent) {
        AppProperties.Stripe cfg = props.getStripe();

        String name = intent.getPurpose() != null
                ? "Paiement Sopikeur — " + intent.getPurpose().name()
                : "Paiement Sopikeur";

        String orderPublicId = intent.getOrder() != null ? intent.getOrder().getPublicId() : "";

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
                                                    .setUnitAmount(intent.getAmount())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(name)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .putMetadata("order_id", orderPublicId)
                    .putMetadata("purpose", intent.getPurpose() != null ? intent.getPurpose().name() : "")
                    .build();

            Session session = Session.create(params);

            log.info("Stripe checkout session created: sessionId={} orderId={}", session.getId(), orderPublicId);
            return new ProviderCheckoutResultDto(session.getUrl(), session.getId(), session.getPaymentIntent());

        } catch (StripeException e) {
            log.error("Stripe error creating session: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la création de la session Stripe: " + e.getMessage(), e);
        }
    }

    @Override
    public WebhookVerificationResultDto verifyWebhook(String rawPayload, Map<String, String> headers) {
        AppProperties.Stripe cfg = props.getStripe();
        String sigHeader = headers.get("stripe-signature");

        if (sigHeader == null) {
            log.warn("Missing Stripe-Signature header");
            return new WebhookVerificationResultDto(false, null, null, null, null);
        }

        Event event;
        try {
            event = Webhook.constructEvent(rawPayload, sigHeader, cfg.getWebhookSecret(), cfg.getWebhookToleranceSeconds());
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            return new WebhookVerificationResultDto(false, null, null, null, null);
        }

        log.info("Stripe webhook verified: type={} id={}", event.getType(), event.getId());

        // Extract session id from the event
        String providerCheckoutId = extractSessionId(event);
        PaymentIntentStatus status = mapEventTypeToStatus(event.getType());

        return new WebhookVerificationResultDto(true, event.getId(), event.getType(), providerCheckoutId, status);
    }

    @Override
    public ProviderPaymentStatusDto fetchStatus(PaymentIntentEntity intent) {
        if (intent.getStripeSessionId() == null) {
            return new ProviderPaymentStatusDto(intent.getStatus(), null);
        }
        try {
            Session session = Session.retrieve(intent.getStripeSessionId());
            PaymentIntentStatus status = "paid".equals(session.getPaymentStatus())
                    ? PaymentIntentStatus.SUCCEEDED : PaymentIntentStatus.PENDING;
            return new ProviderPaymentStatusDto(status, session.getPaymentIntent());
        } catch (StripeException e) {
            log.warn("Could not fetch Stripe session status for {}: {}", intent.getPublicId(), e.getMessage());
            return new ProviderPaymentStatusDto(intent.getStatus(), null);
        }
    }

    /** Extrait l'ID de session Stripe depuis un Event. Retourne null si impossible. */
    public String extractSessionId(Event event) {
        if (event.getType() == null || !event.getType().startsWith("checkout.session")) {
            return null;
        }
        var obj = event.getDataObjectDeserializer().getObject();
        if (obj.isPresent() && obj.get() instanceof Session s) {
            return s.getId();
        }
        try {
            String rawJson = event.getDataObjectDeserializer().getRawJson();
            if (rawJson != null && !rawJson.isBlank()) {
                com.google.gson.JsonObject data =
                        com.google.gson.JsonParser.parseString(rawJson).getAsJsonObject();
                return data.get("id").getAsString();
            }
        } catch (Exception e) {
            log.warn("Could not extract sessionId from Stripe event {}: {}", event.getId(), e.getMessage());
        }
        return null;
    }

    /** Extrait le stripePaymentIntentId depuis un Event checkout.session.completed. */
    public String extractStripePaymentIntentId(Event event) {
        var obj = event.getDataObjectDeserializer().getObject();
        if (obj.isPresent() && obj.get() instanceof Session s) {
            return s.getPaymentIntent();
        }
        try {
            String rawJson = event.getDataObjectDeserializer().getRawJson();
            if (rawJson != null && !rawJson.isBlank()) {
                com.google.gson.JsonObject data =
                        com.google.gson.JsonParser.parseString(rawJson).getAsJsonObject();
                if (data.has("payment_intent")) {
                    return data.get("payment_intent").getAsString();
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract paymentIntentId from Stripe event: {}", e.getMessage());
        }
        return null;
    }

    private PaymentIntentStatus mapEventTypeToStatus(String eventType) {
        if (eventType == null) return null;
        return switch (eventType) {
            case "checkout.session.completed" -> PaymentIntentStatus.SUCCEEDED;
            case "checkout.session.expired"   -> PaymentIntentStatus.EXPIRED;
            default -> null;
        };
    }
}
