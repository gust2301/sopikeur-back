package sn.sopikeur.repo.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.payment.PaymentEventEntity;

public interface PaymentEventRepository extends JpaRepository<PaymentEventEntity, Long> {
    boolean existsByStripeEventId(String stripeEventId);
}
