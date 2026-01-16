package sn.sopikeur.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.dto.request.admin.CreateStockMovementRequest;
import sn.sopikeur.dto.request.admin.UpdateStockRequest;
import sn.sopikeur.entity.stock.StockItem;
import sn.sopikeur.entity.stock.StockMovement;
import sn.sopikeur.service.StockService;

@RestController
@RequestMapping(ApiConstants.V1_ADMIN)
@RequiredArgsConstructor
public class StockAdminController {

    private final StockService stockService;

    @PatchMapping("/products/{id}/stock")
    public ResponseEntity<StockItem> updateStock(
        @PathVariable Long id,
        @Valid @RequestBody UpdateStockRequest request
    ) {
        return ResponseEntity.ok(stockService.updateStock(id, request));
    }

    @PostMapping("/stock/movements")
    public ResponseEntity<StockMovement> createMovement(@Valid @RequestBody CreateStockMovementRequest request) {
        return ResponseEntity.ok(stockService.createMovement(request));
    }
}
