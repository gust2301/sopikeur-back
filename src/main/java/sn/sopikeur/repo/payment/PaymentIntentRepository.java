package sn.sopikeur.repo.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.payment.PaymentIntentEntity;

import java.util.List;
import java.util.Optional;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntentEntity, Long> {
    Optional<PaymentIntentEntity> findByPublicId(String publicId);
    Optional<PaymentIntentEntity> findByStripeSessionId(String stripeSessionId);
    List<PaymentIntentEntity> findByOrderId(Long orderId);

    /**
     * Recherche par provider + identifiant de session provider (pour traitement webhook Wave/OM).
     * Pour Stripe, utilisez findByStripeSessionId pour la compatibilité existante.
     */
    Optional<PaymentIntentEntity> findByProviderAndProviderCheckoutId(String provider, String providerCheckoutId);
}
