package sn.sopikeur.repo.order;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.order.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
