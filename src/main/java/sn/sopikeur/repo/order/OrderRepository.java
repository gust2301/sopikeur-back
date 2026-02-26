package sn.sopikeur.repo.order;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.order.OrderStatus;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<OrderEntity> findByStatus(OrderStatus status, Pageable pageable);
    long countByStatus(OrderStatus status);
}
