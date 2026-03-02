package sn.sopikeur.dto.request.publicapi.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePaymentSessionRequest {

    /** Provider de paiement : STRIPE | WAVE | ORANGE_MONEY */
    @NotBlank
    private String provider;

    /** Objectif du paiement : DEPOSIT | FULL | BALANCE */
    @NotBlank
    private String purpose;
}
