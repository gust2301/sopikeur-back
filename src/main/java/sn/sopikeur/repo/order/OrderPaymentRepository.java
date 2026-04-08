package sn.sopikeur.repo.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sn.sopikeur.entity.order.OrderPaymentEntity;

public interface OrderPaymentRepository extends JpaRepository<OrderPaymentEntity, Long> {
    List<OrderPaymentEntity> findByOrderIdOrderByCreatedAtDescIdDesc(Long orderId);

    Optional<OrderPaymentEntity> findByIdAndOrderId(Long id, Long orderId);

    long countByOrderId(Long orderId);

    @Query("select coalesce(sum(p.amount), 0) from OrderPaymentEntity p where p.order.id = :orderId")
    BigDecimal sumAmountsByOrderId(Long orderId);
}
