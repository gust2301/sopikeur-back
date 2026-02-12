package sn.sopikeur.dto.request.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import sn.sopikeur.entity.stock.StockMovementType;

@Data
public class CreateStockMovementRequest {
    @NotNull(message = "Produit requis")
    private Long productId;

    @NotNull(message = "Type requis")
    private StockMovementType movementType;

    @Min(value = 1, message = "Quantité minimale 1")
    private int quantity;

    private String note;
}
