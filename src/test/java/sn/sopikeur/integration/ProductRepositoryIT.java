package sn.sopikeur.integration;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.catalog.ProductStatus;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.entity.stock.StockItem;
import sn.sopikeur.repo.ProductRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
class ProductRepositoryIT {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("sopikeur_repo_it")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        seedProduct("IT-PANEL-HEX-100", "itfilter Hex Acoustic Panel", ProductType.PANEL, 20, 4, false);
        seedProduct("IT-PANEL-PRE-001", "itfilter Preorder Panel", ProductType.PANEL, 0, 0, true);
        seedProduct("IT-SPC-OAK-200", "itfilter Oak SPC Flooring", ProductType.SPC, 8, 8, false);
        seedProduct("IT-PANEL-NOSTOCK", "itfilter Panel Missing Stock", ProductType.PANEL, null, null, null);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void search_filtersByQueryOnNameAndSku() {
        Page<Product> byName = productRepository.search(
            null,
            "hex acoustic",
            "ALL",
            null,
            PageRequest.of(0, 20)
        );
        Page<Product> bySku = productRepository.search(
            null,
            "it-spc-oak",
            "ALL",
            null,
            PageRequest.of(0, 20)
        );

        assertThat(byName.getContent()).extracting(Product::getSku).contains("IT-PANEL-HEX-100");
        assertThat(bySku.getContent()).extracting(Product::getSku).contains("IT-SPC-OAK-200");
    }

    @Test
    void search_inStockReturnsOnlyAvailableProducts() {
        Page<Product> page = productRepository.search(null, "itfilter", "IN_STOCK", null, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Product::getSku).containsExactly("IT-PANEL-HEX-100");
    }

    @Test
    void search_preorderReturnsPreorderAllowedEvenWhenNoQuantity() {
        Page<Product> page = productRepository.search(null, "itfilter", "PREORDER", null, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Product::getSku).containsExactly("IT-PANEL-PRE-001");
    }

    @Test
    void search_combinesTypeQueryAndStock() {
        Page<Product> page = productRepository.search(
            ProductType.PANEL,
            "itfilter",
            "PREORDER",
            null,
            PageRequest.of(0, 20)
        );

        assertThat(page.getContent()).extracting(Product::getSku).containsExactly("IT-PANEL-PRE-001");
    }

    private void seedProduct(String sku, String name, ProductType type, Integer quantity, Integer reserved, Boolean preorderAllowed) {
        Product product = new Product();
        product.setSku(sku);
        product.setSlug(sku.toLowerCase());
        product.setName(name);
        product.setPrice(BigDecimal.valueOf(10000));
        product.setUnit("m²");
        product.setStatus(ProductStatus.ACTIVE);
        product.setType(type);
        product.setFeatured(false);
        entityManager.persist(product);

        if (quantity != null && reserved != null && preorderAllowed != null) {
            StockItem stockItem = new StockItem();
            stockItem.setProduct(product);
            stockItem.setQuantity(quantity);
            stockItem.setReserved(reserved);
            stockItem.setPreorderAllowed(preorderAllowed);
            entityManager.persist(stockItem);
        }
    }
}
