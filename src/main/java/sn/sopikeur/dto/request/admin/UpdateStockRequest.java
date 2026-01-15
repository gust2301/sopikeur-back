package sn.sopikeur.dto.request.admin;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateStockRequest {
    @Min(value = 0, message = "La quantité doit être positive")
    private Integer quantity;

    @Min(value = 0, message = "La quantité réservée doit être positive")
    private Integer reserved;
}
