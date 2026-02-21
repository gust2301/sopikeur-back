package sn.sopikeur.dto.request.publicapi.commerce;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommerceItemCreateRequest {
    @NotBlank
    private String productId;
    @NotBlank
    private String sku;
    @DecimalMin(value = "0.01")
    private Double qty;
    @NotNull
    private ProductUnit unit;
}
