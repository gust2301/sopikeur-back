package sn.sopikeur.controller.publicapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.dto.request.publicapi.payment.CreatePaymentSessionRequest;
import sn.sopikeur.dto.response.publicapi.stripe.StripeCheckoutResponse;
import sn.sopikeur.service.payment.PaymentOrchestratorService;

/**
 * Endpoint unifié pour la création de sessions de paiement multi-provider.
 * Remplace StripeCheckoutController pour les nouveaux appels front.
 * StripeCheckoutController est conservé pour la compatibilité ascendante.
 */
@RestController
@RequiredArgsConstructor
public class OrderPaymentController {

    private final PaymentOrchestratorService orchestrator;

    /**
     * Crée une session de paiement pour une commande, avec le provider de son choix.
     * POST /api/v1/orders/{publicId}/payments
     * Body: { "provider": "WAVE", "purpose": "DEPOSIT" }
     * Response: { "checkoutUrl": "...", "paymentIntentId": "..." }
     */
    @PostMapping(ApiConstants.V1 + "/orders/{publicId}/payments")
    public ResponseEntity<StripeCheckoutResponse> createPaymentSession(
            @PathVariable String publicId,
            @Valid @RequestBody CreatePaymentSessionRequest request
    ) {
        StripeCheckoutResponse response = orchestrator.createOrderPayment(
                publicId,
                request.getProvider(),
                request.getPurpose()
        );
        return ResponseEntity.ok(response);
    }
}
