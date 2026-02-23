package sn.sopikeur.repo;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.entity.stock.StockItem;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {
    Optional<StockItem> findByProductId(Long productId);

    @Query("""
        SELECT s FROM StockItem s
        WHERE (:productId IS NULL OR s.product.id = :productId)
          AND (:type IS NULL OR s.product.type = :type)
          AND (:inStock IS NULL OR (:inStock = TRUE AND (s.quantity - s.reserved) > 0) OR (:inStock = FALSE AND (s.quantity - s.reserved) <= 0))
          AND (:preorderAllowed IS NULL OR s.preorderAllowed = :preorderAllowed)
          AND (:q IS NULL OR LOWER(s.product.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(s.product.sku) LIKE LOWER(CONCAT('%', :q, '%')))
    """)
    Page<StockItem> search(
        @Param("productId") Long productId,
        @Param("type") ProductType type,
        @Param("q") String q,
        @Param("inStock") Boolean inStock,
        @Param("preorderAllowed") Boolean preorderAllowed,
        Pageable pageable
    );
}
