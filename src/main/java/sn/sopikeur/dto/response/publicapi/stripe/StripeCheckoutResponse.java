package sn.sopikeur.dto.response.publicapi.stripe;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StripeCheckoutResponse {
    private String checkoutUrl;
    private String paymentIntentId;
}
