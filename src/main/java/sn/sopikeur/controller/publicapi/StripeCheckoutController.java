package sn.sopikeur.controller.publicapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.dto.request.publicapi.stripe.StripeCheckoutRequest;
import sn.sopikeur.dto.response.publicapi.stripe.StripeCheckoutResponse;
import sn.sopikeur.service.StripeService;

@RestController
@RequiredArgsConstructor
public class StripeCheckoutController {

    private final StripeService stripeService;

    /** Legacy endpoint — creates a checkout session from an explicit request body. */
    @PostMapping(ApiConstants.V1 + "/checkout/stripe/deposit")
    public ResponseEntity<StripeCheckoutResponse> createDepositSession(
        @Valid @RequestBody StripeCheckoutRequest request
    ) {
        return ResponseEntity.ok(stripeService.createCheckoutSession(request));
    }

    /**
     * New endpoint — creates a Stripe Checkout session for an existing order,
     * computing the amount from the order's payment plan (DEPOSIT_50 or FULL_ONLINE).
     */
    @PostMapping(ApiConstants.V1 + "/orders/{publicId}/payments/stripe")
    public ResponseEntity<StripeCheckoutResponse> createOrderPaymentSession(
        @PathVariable String publicId
    ) {
        return ResponseEntity.ok(stripeService.createOrderPaymentSession(publicId));
    }
}
