package sn.sopikeur.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import sn.sopikeur.config.AppProperties;
import sn.sopikeur.dto.payment.ProviderCheckoutResultDto;
import sn.sopikeur.dto.payment.ProviderPaymentStatusDto;
import sn.sopikeur.dto.payment.WebhookVerificationResultDto;
import sn.sopikeur.entity.payment.PaymentIntentEntity;
import sn.sopikeur.entity.payment.PaymentIntentStatus;
import sn.sopikeur.entity.payment.PaymentProvider;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

/**
 * Implémentation PaymentProviderClient pour Wave Sénégal.
 * API : https://api.wave.com/v1/checkout/sessions
 * Doc : https://docs.wave.com/
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WaveProviderClient implements PaymentProviderClient {

    private static final String WAVE_API_URL = "https://api.wave.com/v1/checkout/sessions";
    private static final String WAVE_SIGNATURE_HEADER = "wave-signature";

    private final AppProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.WAVE;
    }

    @Override
    public ProviderCheckoutResultDto createCheckout(PaymentIntentEntity intent) {
        AppProperties.Wave cfg = props.getWave();

        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank() || cfg.getApiKey().startsWith("placeholder")) {
            log.warn("Wave API key not configured — returning placeholder checkout URL");
            return new ProviderCheckoutResultDto(
                    "https://wave.com/checkout/placeholder?ref=" + intent.getPublicId(),
                    "wave_placeholder_" + intent.getPublicId(),
                    null
            );
        }

        Map<String, Object> body = Map.of(
                "currency", "XOF",
                "amount", String.valueOf(intent.getAmount()),
                "client_reference", intent.getPublicId(),
                "success_url", cfg.getSuccessUrl(),
                "error_url", cfg.getCancelUrl()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(cfg.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    WAVE_API_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    JsonNode.class
            );

            JsonNode responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("Wave API returned empty response");
            }

            String waveSessionId = responseBody.path("id").asText();
            String waveLaunchUrl = responseBody.path("wave_launch_url").asText();

            log.info("Wave checkout session created: waveSessionId={} orderId={}", waveSessionId, intent.getPublicId());
            return new ProviderCheckoutResultDto(waveLaunchUrl, waveSessionId, null);

        } catch (Exception e) {
            log.error("Wave API error creating checkout: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la création de la session Wave: " + e.getMessage(), e);
        }
    }

    @Override
    public WebhookVerificationResultDto verifyWebhook(String rawPayload, Map<String, String> headers) {
        AppProperties.Wave cfg = props.getWave();

        String signature = headers.get(WAVE_SIGNATURE_HEADER);
        if (signature == null) {
            log.warn("Missing Wave-Signature header");
            return new WebhookVerificationResultDto(false, null, null, null, null);
        }

        // Vérification HMAC-SHA256
        if (!cfg.getWebhookSecret().startsWith("placeholder")) {
            try {
                String expectedSig = computeHmacSha256(rawPayload, cfg.getWebhookSecret());
                if (!expectedSig.equals(signature)) {
                    log.warn("Wave webhook signature mismatch");
                    return new WebhookVerificationResultDto(false, null, null, null, null);
                }
            } catch (Exception e) {
                log.error("Wave signature verification error: {}", e.getMessage());
                return new WebhookVerificationResultDto(false, null, null, null, null);
            }
        } else {
            log.warn("Wave webhook secret not configured — skipping signature verification (dev mode)");
        }

        // Parse payload JSON
        try {
            JsonNode payload = objectMapper.readTree(rawPayload);
            String eventType    = payload.path("type").asText();
            String eventId      = payload.path("id").asText(null);
            String checkoutId   = payload.path("data").path("checkout_id").asText(null);
            // Fallback : client_reference contient notre publicId
            if (checkoutId == null || checkoutId.isBlank()) {
                checkoutId = payload.path("data").path("id").asText(null);
            }

            PaymentIntentStatus status = mapWaveEventToStatus(eventType,
                    payload.path("data").path("payment_status").asText(""));

            log.info("Wave webhook parsed: type={} checkoutId={} status={}", eventType, checkoutId, status);
            return new WebhookVerificationResultDto(true, eventId, eventType, checkoutId, status);

        } catch (Exception e) {
            log.error("Wave webhook payload parsing error: {}", e.getMessage());
            return new WebhookVerificationResultDto(false, null, null, null, null);
        }
    }

    @Override
    public ProviderPaymentStatusDto fetchStatus(PaymentIntentEntity intent) {
        // Wave Checkout API n'expose pas d'endpoint GET pour le statut session.
        // Le statut est mis à jour exclusivement via webhook.
        return new ProviderPaymentStatusDto(intent.getStatus(), intent.getProviderPaymentRef());
    }

    private PaymentIntentStatus mapWaveEventToStatus(String eventType, String paymentStatus) {
        if ("checkout.session.completed".equals(eventType) || "succeeded".equalsIgnoreCase(paymentStatus)) {
            return PaymentIntentStatus.SUCCEEDED;
        } else if ("checkout.session.failed".equals(eventType) || "failed".equalsIgnoreCase(paymentStatus)) {
            return PaymentIntentStatus.FAILED;
        } else if ("checkout.session.expired".equals(eventType)) {
            return PaymentIntentStatus.EXPIRED;
        }
        return PaymentIntentStatus.PENDING;
    }

    private String computeHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
