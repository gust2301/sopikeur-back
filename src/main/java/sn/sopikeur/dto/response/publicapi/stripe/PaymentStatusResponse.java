package sn.sopikeur.dto.response.publicapi.stripe;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class PaymentStatusResponse {
    private String id;
    /** Provider de paiement : STRIPE | WAVE | ORANGE_MONEY */
    private String provider;
    private String stripeSessionId;
    private String stripePaymentIntentId;
    private String status;
    private String purpose;
    private Long amount;
    private String currency;
    private String orderId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
