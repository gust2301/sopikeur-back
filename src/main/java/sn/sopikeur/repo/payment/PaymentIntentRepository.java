package sn.sopikeur.repo.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.payment.PaymentIntentEntity;

import java.util.Optional;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntentEntity, Long> {
    Optional<PaymentIntentEntity> findByPublicId(String publicId);
    Optional<PaymentIntentEntity> findByStripeSessionId(String stripeSessionId);
    java.util.List<PaymentIntentEntity> findByOrderId(Long orderId);
}
