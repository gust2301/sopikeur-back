package sn.sopikeur.repo.order;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sn.sopikeur.entity.order.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("SELECT COALESCE(SUM(i.lineTotalSnapshot), 0) FROM OrderItem i")
    BigDecimal sumRevenue();
}
