package sn.sopikeur.unit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.sopikeur.common.error.StockConflictException;
import sn.sopikeur.dto.request.admin.UpdateStockRequest;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.stock.StockItem;
import sn.sopikeur.repo.ProductRepository;
import sn.sopikeur.repo.StockItemRepository;
import sn.sopikeur.repo.StockMovementRepository;
import sn.sopikeur.service.StockService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private StockItemRepository stockItemRepository;
    @Mock private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private StockService stockService;

    @Test
    void updateStock_shouldRejectReservedGreaterThanQuantity() {
        Product p = new Product(); p.setId(1L);
        StockItem item = new StockItem(); item.setProduct(p); item.setQuantity(5); item.setReserved(1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(stockItemRepository.findByProductId(1L)).thenReturn(Optional.of(item));

        UpdateStockRequest request = new UpdateStockRequest();
        request.setQuantity(4);
        request.setReserved(6);

        assertThatThrownBy(() -> stockService.updateStock(1L, request)).isInstanceOf(StockConflictException.class);
    }
}
