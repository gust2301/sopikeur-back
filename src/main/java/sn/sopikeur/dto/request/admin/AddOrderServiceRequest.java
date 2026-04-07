package sn.sopikeur.dto.request.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class AddOrderServiceRequest {
    private Long serviceTypeId;

    private Long productId;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal unitPrice;

    private String note;
}
