package sn.sopikeur.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.sopikeur.common.pagination.PageResponse;
import sn.sopikeur.dto.request.admin.CreateStockMovementRequest;
import sn.sopikeur.dto.request.admin.UpdateStockRequest;
import sn.sopikeur.dto.response.admin.StockItemResponseDto;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.entity.stock.StockMovement;
import sn.sopikeur.service.StockService;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class StockAdminController {

    private final StockService stockService;

    @GetMapping("/stocks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR','SALES')")
    public PageResponse<StockItemResponseDto> list(
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) ProductType type,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) Boolean inStock,
        @RequestParam(required = false) Boolean preorderAllowed,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) { return stockService.list(productId, type, q, inStock, preorderAllowed, page, size); }

    @PutMapping("/stocks/{productId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR','SALES')")
    public ResponseEntity<StockItemResponseDto> updateStock(@PathVariable Long productId, @Valid @RequestBody UpdateStockRequest request) {
        return ResponseEntity.ok(stockService.updateStock(productId, request));
    }

    @PostMapping("/stock/movements")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR')")
    public ResponseEntity<StockMovement> createMovement(@Valid @RequestBody CreateStockMovementRequest request) {
        return ResponseEntity.ok(stockService.createMovement(request));
    }
}
