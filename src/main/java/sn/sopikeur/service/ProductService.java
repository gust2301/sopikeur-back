package sn.sopikeur.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.sopikeur.common.error.NotFoundException;
import sn.sopikeur.dto.response.publicapi.ProductDetailResponse;
import sn.sopikeur.dto.response.publicapi.ProductSummaryResponse;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.entity.stock.StockItem;
import sn.sopikeur.entity.stock.StockStatus;
import sn.sopikeur.mapper.ProductMapper;
import sn.sopikeur.repo.ProductRepository;
import sn.sopikeur.repo.StockItemRepository;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StockItemRepository stockItemRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> listProducts(ProductType type, Boolean featured) {
        List<Product> products;
        if (type != null && featured != null) {
            products = productRepository.findByTypeAndFeatured(type, featured);
        } else if (type != null) {
            products = productRepository.findByType(type);
        } else if (featured != null) {
            products = productRepository.findByFeatured(featured);
        } else {
            products = productRepository.findAll();
        }
        return products.stream()
            .map(this::toSummary)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProduct(String slug) {
        Product product = productRepository.findBySlug(slug)
            .orElseThrow(() -> new NotFoundException("Produit introuvable"));
        return productMapper.toDetail(product, resolveStockStatus(product.getId()));
    }

    @Transactional(readOnly = true)
    public ProductSummaryResponse toSummary(Product product) {
        return productMapper.toSummary(product, resolveStockStatus(product.getId()));
    }

    @Transactional(readOnly = true)
    public StockStatus resolveStockStatus(Long productId) {
        Optional<StockItem> stockItem = stockItemRepository.findByProductId(productId);
        if (stockItem.isEmpty()) {
            return StockStatus.OUT_OF_STOCK;
        }
        int available = stockItem.get().getQuantity() - stockItem.get().getReserved();
        if (available > 0) {
            return StockStatus.IN_STOCK;
        }
        if (stockItem.get().getQuantity() == 0 && stockItem.get().isPreorderAllowed()) {
            return StockStatus.PREORDER;
        }
        return StockStatus.OUT_OF_STOCK;
    }
}
