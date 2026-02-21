package sn.sopikeur.repo;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.catalog.ProductType;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);

    Optional<Product> findBySlug(String slug);

    @Query(
        value = """
            SELECT DISTINCT p
            FROM Product p
            LEFT JOIN StockItem s ON s.product = p
            WHERE (:type IS NULL OR p.type = :type)
              AND (:featured IS NULL OR p.featured = :featured)
              AND (
                :q IS NULL
                OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(p.descriptionShort, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
              AND (
                :stock = 'ALL'
                OR (:stock = 'IN_STOCK' AND COALESCE(s.quantity, 0) - COALESCE(s.reserved, 0) > 0)
                OR (:stock = 'PREORDER' AND COALESCE(s.preorderAllowed, false) = true)
              )
            """,
        countQuery = """
            SELECT COUNT(DISTINCT p)
            FROM Product p
            LEFT JOIN StockItem s ON s.product = p
            WHERE (:type IS NULL OR p.type = :type)
              AND (:featured IS NULL OR p.featured = :featured)
              AND (
                :q IS NULL
                OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(p.descriptionShort, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
              AND (
                :stock = 'ALL'
                OR (:stock = 'IN_STOCK' AND COALESCE(s.quantity, 0) - COALESCE(s.reserved, 0) > 0)
                OR (:stock = 'PREORDER' AND COALESCE(s.preorderAllowed, false) = true)
              )
            """
    )
    Page<Product> search(
        @Param("type") ProductType type,
        @Param("q") String q,
        @Param("stock") String stock,
        @Param("featured") Boolean featured,
        Pageable pageable
    );
}
