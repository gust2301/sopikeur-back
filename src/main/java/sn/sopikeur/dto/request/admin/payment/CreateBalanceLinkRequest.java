package sn.sopikeur.dto.request.admin.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBalanceLinkRequest {

    /** Provider de paiement : STRIPE | WAVE | ORANGE_MONEY */
    @NotBlank
    private String provider;

    /** Objectif : BALANCE (solde restant) ou DEPOSIT */
    @NotBlank
    private String purpose;
}
