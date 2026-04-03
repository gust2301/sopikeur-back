package sn.sopikeur.repo.order;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.order.OrderStatus;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<OrderEntity> findByStatus(OrderStatus status, Pageable pageable);
    long countByStatus(OrderStatus status);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<OrderEntity> findByPublicId(String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OrderEntity> findWithLockById(Long id);
}
