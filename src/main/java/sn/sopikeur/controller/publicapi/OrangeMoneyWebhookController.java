package sn.sopikeur.controller.publicapi;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.sopikeur.service.payment.PaymentOrchestratorService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Récepteur de webhooks Orange Money (notif_url).
 * Orange Money envoie un POST JSON sur cette URL après confirmation du paiement.
 * Exposé sans authentification (permitAll) — la vérification est faite dans OrangeMoneyProviderClient.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class OrangeMoneyWebhookController {

    private final PaymentOrchestratorService orchestrator;

    @PostMapping("/orange-money")
    public ResponseEntity<String> handleOrangeMoneyWebhook(HttpServletRequest httpRequest) {
        byte[] rawBody;
        try {
            rawBody = httpRequest.getInputStream().readAllBytes();
        } catch (IOException e) {
            log.error("Failed to read Orange Money webhook body: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to read body");
        }

        String rawPayload = new String(rawBody, StandardCharsets.UTF_8);
        Map<String, String> headers = extractHeaders(httpRequest);

        try {
            orchestrator.processWebhookEvent("ORANGE_MONEY", rawPayload, headers);
            return ResponseEntity.ok("OK");
        } catch (IllegalArgumentException e) {
            log.warn("Orange Money webhook rejected: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Orange Money webhook processing error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal error");
        }
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                headers.put(name.toLowerCase(), request.getHeader(name));
            }
        }
        return headers;
    }
}
