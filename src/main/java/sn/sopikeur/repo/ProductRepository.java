package sn.sopikeur.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.catalog.ProductType;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);

    Optional<Product> findBySlug(String slug);

    List<Product> findByType(ProductType type);

    List<Product> findByTypeAndFeatured(ProductType type, boolean featured);

    List<Product> findByFeatured(boolean featured);
}
