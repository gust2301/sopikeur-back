package sn.sopikeur.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import sn.sopikeur.config.AppProperties;
import sn.sopikeur.dto.payment.ProviderCheckoutResultDto;
import sn.sopikeur.dto.payment.ProviderPaymentStatusDto;
import sn.sopikeur.dto.payment.WebhookVerificationResultDto;
import sn.sopikeur.entity.payment.PaymentIntentEntity;
import sn.sopikeur.entity.payment.PaymentIntentStatus;
import sn.sopikeur.entity.payment.PaymentProvider;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Implémentation PaymentProviderClient pour Orange Money (WebPay Sénégal).
 * API : https://api.orange.com/orange-money-webpay/{country}/v1/webpayment
 * OAuth2 : https://api.orange.com/oauth/v3/token (client_credentials)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrangeMoneyProviderClient implements PaymentProviderClient {

    private static final String OM_TOKEN_URL     = "https://api.orange.com/oauth/v3/token";
    private static final String OM_WEBPAY_URL    = "https://api.orange.com/orange-money-webpay/%s/v1/webpayment";

    private final AppProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Cache du token OAuth2
    private volatile String cachedToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.ORANGE_MONEY;
    }

    @Override
    public ProviderCheckoutResultDto createCheckout(PaymentIntentEntity intent) {
        AppProperties.OrangeMoney cfg = props.getOrangeMoney();

        if (cfg.getClientId() == null || cfg.getClientId().startsWith("placeholder")) {
            log.warn("Orange Money credentials not configured — returning placeholder checkout URL");
            return new ProviderCheckoutResultDto(
                    "https://orangemoney.sn/checkout/placeholder?ref=" + intent.getPublicId(),
                    "om_placeholder_" + intent.getPublicId(),
                    null
            );
        }

        String accessToken = getAccessToken(cfg);
        String orderPublicId = intent.getPublicId();

        Map<String, Object> body = Map.of(
                "merchant_key",  cfg.getMerchantKey(),
                "currency",      "ORA",
                "order_id",      orderPublicId,
                "amount",        intent.getAmount(),
                "return_url",    cfg.getReturnUrl(),
                "cancel_url",    cfg.getCancelUrl(),
                "notif_url",     cfg.getNotifUrl(),
                "lang",          "fr",
                "reference",     orderPublicId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String url = String.format(OM_WEBPAY_URL, cfg.getCountry().toLowerCase());
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    JsonNode.class
            );

            JsonNode responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("Orange Money API returned empty response");
            }

            String paymentUrl = responseBody.path("payment_url").asText();
            String payToken   = responseBody.path("pay_token").asText();

            log.info("Orange Money checkout created: payToken={} orderId={}", payToken, orderPublicId);
            return new ProviderCheckoutResultDto(paymentUrl, payToken, null);

        } catch (Exception e) {
            log.error("Orange Money API error creating checkout: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la création du paiement Orange Money: " + e.getMessage(), e);
        }
    }

    @Override
    public WebhookVerificationResultDto verifyWebhook(String rawPayload, Map<String, String> headers) {
        // Orange Money envoie un POST sur notif_url avec le résultat du paiement.
        // Vérification via le notif_token retourné lors de la création du paiement.
        try {
            JsonNode payload = objectMapper.readTree(rawPayload);

            String status    = payload.path("status").asText("");
            String txnId     = payload.path("txnid").asText(null);
            String orderId   = payload.path("order_id").asText(null);
            String notifToken = payload.path("notif_token").asText(null);
            // Orange Money utilise "x_reference" comme orderId dans certaines versions
            if (orderId == null || orderId.isBlank()) {
                orderId = payload.path("x_reference").asText(null);
            }

            PaymentIntentStatus piStatus = mapOmStatusToIntentStatus(status);

            // L'eventId est le txnId + orderId pour idempotency
            String eventId = txnId != null ? txnId : (orderId + "_" + status);

            log.info("Orange Money webhook received: status={} orderId={} txnId={}", status, orderId, txnId);
            return new WebhookVerificationResultDto(true, eventId, "payment." + status.toLowerCase(), orderId, piStatus);

        } catch (Exception e) {
            log.error("Orange Money webhook parsing error: {}", e.getMessage());
            return new WebhookVerificationResultDto(false, null, null, null, null);
        }
    }

    @Override
    public ProviderPaymentStatusDto fetchStatus(PaymentIntentEntity intent) {
        // Orange Money ne fournit pas de polling direct dans WebPay.
        // Le statut est mis à jour via notif_url (webhook).
        return new ProviderPaymentStatusDto(intent.getStatus(), intent.getProviderPaymentRef());
    }

    private synchronized String getAccessToken(AppProperties.OrangeMoney cfg) {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }

        String credentials = Base64.getEncoder().encodeToString(
                (cfg.getClientId() + ":" + cfg.getClientSecret()).getBytes()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + credentials);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                OM_TOKEN_URL,
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                JsonNode.class
        );

        JsonNode body = response.getBody();
        if (body == null || !body.has("access_token")) {
            throw new RuntimeException("Orange Money OAuth2 token response invalide");
        }

        cachedToken = body.path("access_token").asText();
        long expiresIn = body.path("expires_in").asLong(3000); // 50 min par défaut
        tokenExpiry = Instant.now().plusSeconds(expiresIn - 60); // marge de 60s

        log.info("Orange Money OAuth2 token refreshed, expires in {}s", expiresIn);
        return cachedToken;
    }

    private PaymentIntentStatus mapOmStatusToIntentStatus(String omStatus) {
        return switch (omStatus.toUpperCase()) {
            case "SUCCESS", "SUCCESSFULL" -> PaymentIntentStatus.SUCCEEDED;
            case "FAILED", "FAIL"         -> PaymentIntentStatus.FAILED;
            case "CANCEL", "CANCELLED"    -> PaymentIntentStatus.FAILED;
            default                       -> PaymentIntentStatus.PENDING;
        };
    }
}
