package sn.sopikeur.controller.publicapi;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.service.StripeService;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final StripeService stripeService;

    /**
     * Endpoint Stripe Webhook — lit le body brut pour la vérification de signature.
     * Doit être déclaré en permitAll() dans SecurityConfig (chemin /webhooks/stripe).
     */
    @PostMapping(value = "/stripe", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleStripeWebhook(
        HttpServletRequest httpRequest,
        @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader
    ) {
        if (sigHeader == null) {
            log.warn("Stripe webhook call without Stripe-Signature header");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing Stripe-Signature header");
        }

        byte[] rawBody;
        try {
            rawBody = httpRequest.getInputStream().readAllBytes();
        } catch (IOException e) {
            log.error("Failed to read webhook body: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to read body");
        }

        try {
            stripeService.processWebhookEvent(rawBody, sigHeader);
            return ResponseEntity.ok("OK");
        } catch (IllegalArgumentException e) {
            // Signature invalide
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Webhook processing error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal error");
        }
    }
}
