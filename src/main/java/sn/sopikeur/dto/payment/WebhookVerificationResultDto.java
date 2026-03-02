package sn.sopikeur.dto.payment;

import sn.sopikeur.entity.payment.PaymentIntentStatus;

/**
 * Résultat de la vérification d'un webhook provider.
 */
public record WebhookVerificationResultDto(
        /** La signature est valide. */
        boolean valid,
        /** Identifiant unique de l'événement (pour idempotency). */
        String providerEventId,
        /** Type d'événement (ex: checkout.session.completed). */
        String eventType,
        /** Identifiant de session côté provider pour retrouver le PaymentIntent. */
        String providerCheckoutId,
        /** Statut à appliquer au PaymentIntent suite à cet événement. */
        PaymentIntentStatus status
) {}
