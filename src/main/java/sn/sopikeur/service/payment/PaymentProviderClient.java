package sn.sopikeur.service.payment;

import sn.sopikeur.dto.payment.ProviderCheckoutResultDto;
import sn.sopikeur.dto.payment.ProviderPaymentStatusDto;
import sn.sopikeur.dto.payment.WebhookVerificationResultDto;
import sn.sopikeur.entity.payment.PaymentIntentEntity;
import sn.sopikeur.entity.payment.PaymentProvider;

import java.util.Map;

/**
 * Interface commune à tous les providers de paiement (Stripe, Wave, Orange Money).
 * Chaque implémentation est un @Component Spring et est auto-wire dans PaymentOrchestratorService.
 */
public interface PaymentProviderClient {

    /** Identifiant du provider que ce client gère. */
    PaymentProvider getProvider();

    /**
     * Crée une session de checkout chez le provider.
     * L'entité intent est déjà persistée avec amount, currency, purpose, order.
     * @return l'URL de redirection + les identifiants provider pour mise à jour de l'intent.
     */
    ProviderCheckoutResultDto createCheckout(PaymentIntentEntity intent);

    /**
     * Vérifie la signature et parse le payload d'un webhook.
     * @param rawPayload  corps brut de la requête HTTP (pour vérification HMAC)
     * @param headers     headers HTTP reçus
     * @return résultat de vérification avec statut à appliquer
     */
    WebhookVerificationResultDto verifyWebhook(String rawPayload, Map<String, String> headers);

    /**
     * Interroge directement l'API provider pour connaître le statut actuel d'un paiement.
     * Utilisé pour le polling ou la réconciliation.
     */
    ProviderPaymentStatusDto fetchStatus(PaymentIntentEntity intent);
}
