package sn.sopikeur.repo.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.order.OrderStatus;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<OrderEntity> findByStatusInOrderByCreatedAtDesc(List<OrderStatus> statuses, Pageable pageable);
    Page<OrderEntity> findByStatus(OrderStatus status, Pageable pageable);
    long countByStatus(OrderStatus status);
    long countByStatusNot(OrderStatus status);

    @EntityGraph(attributePaths = {"items", "items.product", "items.serviceType"})
    Optional<OrderEntity> findByPublicId(String publicId);

    @EntityGraph(attributePaths = {"items", "items.product", "items.serviceType"})
    Optional<OrderEntity> findDetailedById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OrderEntity> findWithLockById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items", "items.product", "items.serviceType"})
    Optional<OrderEntity> findDetailedWithLockById(Long id);

    @Query("SELECT COALESCE(SUM(o.amountPaid), 0) FROM OrderEntity o")
    BigDecimal sumPaidAmount();

    @Query("""
        SELECT COUNT(o)
        FROM OrderEntity o
        WHERE (:startAt IS NULL OR o.createdAt >= :startAt)
          AND (:endAt IS NULL OR o.createdAt < :endAt)
        """)
    long countByCreatedAtBetween(
        @org.springframework.data.repository.query.Param("startAt") OffsetDateTime startAt,
        @org.springframework.data.repository.query.Param("endAt") OffsetDateTime endAt
    );

    @Query("""
        SELECT COALESCE(SUM(o.amountTotal), 0)
        FROM OrderEntity o
        WHERE (:startAt IS NULL OR o.createdAt >= :startAt)
          AND (:endAt IS NULL OR o.createdAt < :endAt)
        """)
    BigDecimal sumAmountTotalByCreatedAtBetween(
        @org.springframework.data.repository.query.Param("startAt") OffsetDateTime startAt,
        @org.springframework.data.repository.query.Param("endAt") OffsetDateTime endAt
    );

    @Query("""
        SELECT COUNT(o)
        FROM OrderEntity o
        WHERE o.status IN :statuses
          AND (:startAt IS NULL OR o.createdAt >= :startAt)
          AND (:endAt IS NULL OR o.createdAt < :endAt)
        """)
    long countByStatusInAndCreatedAtBetween(
        @org.springframework.data.repository.query.Param("statuses") List<OrderStatus> statuses,
        @org.springframework.data.repository.query.Param("startAt") OffsetDateTime startAt,
        @org.springframework.data.repository.query.Param("endAt") OffsetDateTime endAt
    );

    @Query("""
        SELECT COALESCE(SUM(o.amountTotal), 0)
        FROM OrderEntity o
        WHERE o.status IN :statuses
          AND (:startAt IS NULL OR o.createdAt >= :startAt)
          AND (:endAt IS NULL OR o.createdAt < :endAt)
        """)
    BigDecimal sumAmountTotalByStatusInAndCreatedAtBetween(
        @org.springframework.data.repository.query.Param("statuses") List<OrderStatus> statuses,
        @org.springframework.data.repository.query.Param("startAt") OffsetDateTime startAt,
        @org.springframework.data.repository.query.Param("endAt") OffsetDateTime endAt
    );

    @Query("""
        SELECT COALESCE(SUM(o.amountPaid), 0)
        FROM OrderEntity o
        WHERE o.status IN :statuses
          AND (:startAt IS NULL OR o.createdAt >= :startAt)
          AND (:endAt IS NULL OR o.createdAt < :endAt)
        """)
    BigDecimal sumAmountPaidByStatusInAndCreatedAtBetween(
        @org.springframework.data.repository.query.Param("statuses") List<OrderStatus> statuses,
        @org.springframework.data.repository.query.Param("startAt") OffsetDateTime startAt,
        @org.springframework.data.repository.query.Param("endAt") OffsetDateTime endAt
    );
}
