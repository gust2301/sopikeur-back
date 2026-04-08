package sn.sopikeur.repo.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.sopikeur.entity.order.OrderExpenseEntity;
import sn.sopikeur.entity.order.OrderExpenseKind;

public interface OrderExpenseRepository extends JpaRepository<OrderExpenseEntity, Long> {
    List<OrderExpenseEntity> findByOrderIdOrderByExpenseDateDescIdDesc(Long orderId);

    Optional<OrderExpenseEntity> findByIdAndOrderId(Long id, Long orderId);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM OrderExpenseEntity e
        WHERE e.order.id = :orderId
          AND e.kind = :kind
        """)
    BigDecimal sumAmountsByOrderIdAndKind(@Param("orderId") Long orderId, @Param("kind") OrderExpenseKind kind);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM OrderExpenseEntity e
        WHERE e.kind = :kind
          AND (:startDate IS NULL OR e.expenseDate >= :startDate)
          AND (:endDate IS NULL OR e.expenseDate <= :endDate)
        """)
    BigDecimal sumAmountsByKindAndExpenseDateBetween(
        @Param("kind") OrderExpenseKind kind,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
