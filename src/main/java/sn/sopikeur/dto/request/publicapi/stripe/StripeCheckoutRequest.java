package sn.sopikeur.dto.request.publicapi.stripe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class StripeCheckoutRequest {

    /** publicId de la commande */
    @NotBlank
    private String orderId;

    /** Ex: "DEPOSIT" */
    @NotBlank
    private String purpose;

    /** Montant en XOF (devise zéro-décimale, ex: 50000 = 50 000 XOF) */
    @NotNull
    @Positive
    private Long amountXof;

    /** Libellé affiché sur la page Stripe Checkout */
    private String description;
}
