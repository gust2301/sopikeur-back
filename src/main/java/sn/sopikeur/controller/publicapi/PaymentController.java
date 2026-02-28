package sn.sopikeur.controller.publicapi;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.common.error.NotFoundException;
import sn.sopikeur.dto.response.publicapi.stripe.PaymentStatusResponse;
import sn.sopikeur.entity.payment.PaymentIntentEntity;
import sn.sopikeur.repo.payment.PaymentIntentRepository;

@RestController
@RequestMapping(ApiConstants.V1 + "/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentIntentRepository paymentIntentRepository;

    @GetMapping("/{paymentIntentId}")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
        @PathVariable String paymentIntentId
    ) {
        PaymentIntentEntity pi = paymentIntentRepository.findByPublicId(paymentIntentId)
            .orElseThrow(() -> new NotFoundException("PaymentIntent introuvable: " + paymentIntentId));

        PaymentStatusResponse response = PaymentStatusResponse.builder()
            .id(pi.getPublicId())
            .stripeSessionId(pi.getStripeSessionId())
            .stripePaymentIntentId(pi.getStripePaymentIntentId())
            .status(pi.getStatus().name())
            .purpose(pi.getPurpose().name())
            .amount(pi.getAmount())
            .currency(pi.getCurrency())
            .orderId(pi.getOrder() != null ? pi.getOrder().getPublicId() : null)
            .createdAt(pi.getCreatedAt())
            .updatedAt(pi.getUpdatedAt())
            .build();

        return ResponseEntity.ok(response);
    }
}
