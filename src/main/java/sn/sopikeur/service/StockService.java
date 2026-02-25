package sn.sopikeur.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.sopikeur.common.error.NotFoundException;
import sn.sopikeur.common.error.StockConflictException;
import sn.sopikeur.common.pagination.PageResponse;
import sn.sopikeur.dto.request.admin.CreateStockMovementRequest;
import sn.sopikeur.dto.request.admin.UpdateStockRequest;
import sn.sopikeur.dto.response.admin.StockItemResponseDto;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.entity.stock.StockItem;
import sn.sopikeur.entity.stock.StockMovement;
import sn.sopikeur.entity.stock.StockMovementType;
import sn.sopikeur.mapper.StockMapper;
import sn.sopikeur.repo.ProductRepository;
import sn.sopikeur.repo.StockItemRepository;
import sn.sopikeur.repo.StockMovementRepository;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductRepository productRepository;
    private final StockItemRepository stockItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockMapper stockMapper;

    @Transactional(readOnly = true)
    public PageResponse<StockItemResponseDto> list(Long productId, ProductType type, String q, Boolean inStock, Boolean preorderAllowed, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        var result = stockItemRepository.search(productId, type, q, inStock, preorderAllowed,
            PageRequest.of(safePage - 1, safeSize, Sort.by("product.id").ascending()));
        return PageResponse.<StockItemResponseDto>builder()
            .items(result.getContent().stream().map(stockMapper::toDto).toList())
            .page(safePage).size(safeSize).total(result.getTotalElements()).totalPages(result.getTotalPages())
            .build();
    }

    @Transactional
    public StockItemResponseDto updateStock(Long productId, UpdateStockRequest request) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NotFoundException("Produit introuvable"));
        StockItem stockItem = stockItemRepository.findByProductId(productId)
            .orElseGet(() -> createStockItem(product));
        if (request.getQuantity() != null) {
            stockItem.setQuantity(request.getQuantity());
        }
        if (request.getReserved() != null) {
            stockItem.setReserved(request.getReserved());
        }
        if (stockItem.getReserved() > stockItem.getQuantity()) {
            throw new StockConflictException("reserved doit être inférieur ou égal à quantity");
        }
        if (request.getPreorderAllowed() != null) {
            stockItem.setPreorderAllowed(request.getPreorderAllowed());
        }
        return stockMapper.toDto(stockItemRepository.save(stockItem));
    }

    @Transactional
    public StockMovement createMovement(CreateStockMovementRequest request) {
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new NotFoundException("Produit introuvable"));
        StockItem stockItem = stockItemRepository.findByProductId(product.getId())
            .orElseGet(() -> createStockItem(product));

        if (request.getMovementType() == StockMovementType.ADJUST) {
            stockItem.setQuantity(request.getQuantity());
        } else if (request.getMovementType() == StockMovementType.IN) {
            stockItem.setQuantity(stockItem.getQuantity() + request.getQuantity());
        } else if (request.getMovementType() == StockMovementType.OUT) {
            stockItem.setQuantity(Math.max(0, stockItem.getQuantity() - request.getQuantity()));
        }
        stockItemRepository.save(stockItem);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(request.getMovementType());
        movement.setQuantity(request.getQuantity());
        movement.setNote(request.getNote());
        return stockMovementRepository.save(movement);
    }

    private StockItem createStockItem(Product product) {
        StockItem item = new StockItem();
        item.setProduct(product);
        item.setQuantity(0);
        item.setReserved(0);
        item.setPreorderAllowed(false);
        return item;
    }
}
