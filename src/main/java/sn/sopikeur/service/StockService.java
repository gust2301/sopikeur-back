package sn.sopikeur.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.sopikeur.common.error.NotFoundException;
import sn.sopikeur.dto.request.admin.CreateStockMovementRequest;
import sn.sopikeur.dto.request.admin.UpdateStockRequest;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.stock.StockItem;
import sn.sopikeur.entity.stock.StockMovement;
import sn.sopikeur.entity.stock.StockMovementType;
import sn.sopikeur.repo.ProductRepository;
import sn.sopikeur.repo.StockItemRepository;
import sn.sopikeur.repo.StockMovementRepository;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductRepository productRepository;
    private final StockItemRepository stockItemRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional
    public StockItem updateStock(Long productId, UpdateStockRequest request) {
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
        return stockItemRepository.save(stockItem);
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
        return item;
    }
}
