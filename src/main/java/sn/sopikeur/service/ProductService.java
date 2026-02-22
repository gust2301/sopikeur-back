package sn.sopikeur.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.sopikeur.common.error.NotFoundException;
import sn.sopikeur.dto.request.publicapi.ProductSearchRequest;
import sn.sopikeur.dto.request.publicapi.StockFilter;
import sn.sopikeur.dto.response.publicapi.ProductDetailResponse;
import sn.sopikeur.dto.response.publicapi.ProductSearchResponse;
import sn.sopikeur.dto.response.publicapi.ProductSummaryResponse;
import sn.sopikeur.entity.catalog.Product;
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
    public ProductSearchResponse listProducts(ProductSearchRequest request) {
        int safePage = Math.max(request.getPage(), 1);
        int safeSize = Math.min(Math.max(request.getSize(), 1), 100);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by("createdAt").descending());

        String query = normalizeQuery(request.getQ());
        StockFilter stockFilter = request.getStock() == null ? StockFilter.ALL : request.getStock();

        Page<Product> products = productRepository.search(
            request.getType(),
            query,
            stockFilter.name(),
            request.getFeatured(),
            pageable
        );

        return ProductSearchResponse.builder()
            .products(products.getContent().stream().map(this::toSummary).toList())
            .total(products.getTotalElements())
            .page(safePage)
            .size(safeSize)
            .build();
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
        if (stockItem.get().isPreorderAllowed()) {
            return StockStatus.PREORDER;
        }
        return StockStatus.OUT_OF_STOCK;
    }

    private String normalizeQuery(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
