package sn.sopikeur.repo.order;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.order.OrderStatus;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<OrderEntity> findByOrderStatus(OrderStatus orderStatus, Pageable pageable);
    long countByOrderStatus(OrderStatus orderStatus);
    java.util.Optional<OrderEntity> findByPublicId(String publicId);
}
