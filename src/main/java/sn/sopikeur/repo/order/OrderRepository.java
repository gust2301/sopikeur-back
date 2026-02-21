package sn.sopikeur.repo.order;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.order.OrderEntity;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
}
