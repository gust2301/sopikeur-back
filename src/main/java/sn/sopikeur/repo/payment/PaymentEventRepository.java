package sn.sopikeur.repo.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.payment.PaymentEventEntity;

public interface PaymentEventRepository extends JpaRepository<PaymentEventEntity, Long> {
    boolean existsByStripeEventId(String stripeEventId);

    /**
     * Vérification d'idempotency provider-agnostique.
     * Utilise la contrainte UNIQUE (provider, provider_event_id) de la table payment_events.
     */
    boolean existsByProviderAndProviderEventId(String provider, String providerEventId);
}
