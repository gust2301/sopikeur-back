package sn.sopikeur.controller.publicapi;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.sopikeur.service.payment.PaymentOrchestratorService;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Récepteur de webhooks Wave.
 * Exposé sans authentification (permitAll) — la vérification de signature est faite dans WaveProviderClient.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WaveWebhookController {

    private final PaymentOrchestratorService orchestrator;

    @PostMapping(value = "/wave", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleWaveWebhook(HttpServletRequest httpRequest) {
        byte[] rawBody;
        try {
            rawBody = httpRequest.getInputStream().readAllBytes();
        } catch (IOException e) {
            log.error("Failed to read Wave webhook body: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to read body");
        }

        String rawPayload = new String(rawBody, java.nio.charset.StandardCharsets.UTF_8);
        Map<String, String> headers = extractHeaders(httpRequest);

        try {
            orchestrator.processWebhookEvent("WAVE", rawPayload, headers);
            return ResponseEntity.ok("OK");
        } catch (IllegalArgumentException e) {
            log.warn("Wave webhook rejected: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Wave webhook processing error: {}", e.getMessage(), e);
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
