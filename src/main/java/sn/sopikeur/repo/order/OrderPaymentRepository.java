package sn.sopikeur.repo.order;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.order.OrderPaymentEntity;

public interface OrderPaymentRepository extends JpaRepository<OrderPaymentEntity, Long> {
}
