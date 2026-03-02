package sn.sopikeur.dto.payment;

/**
 * Résultat de la création d'un checkout chez un provider de paiement.
 */
public record ProviderCheckoutResultDto(
        /** URL vers laquelle rediriger l'utilisateur pour payer. */
        String checkoutUrl,
        /** Identifiant de la session côté provider (Wave checkout id, OM payToken, Stripe session.id). */
        String providerCheckoutId,
        /** Référence complémentaire (Stripe paymentIntentId, Wave transaction id, etc.) */
        String providerPaymentRef
) {}
