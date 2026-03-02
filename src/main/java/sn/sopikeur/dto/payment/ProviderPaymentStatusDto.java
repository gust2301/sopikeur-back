package sn.sopikeur.dto.payment;

import sn.sopikeur.entity.payment.PaymentIntentStatus;

/**
 * Statut d'un paiement retourné par polling direct sur l'API provider.
 */
public record ProviderPaymentStatusDto(
        PaymentIntentStatus status,
        String providerPaymentRef
) {}
