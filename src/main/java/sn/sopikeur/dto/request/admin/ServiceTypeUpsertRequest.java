package sn.sopikeur.dto.request.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ServiceTypeUpsertRequest {
    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private String unit;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal defaultPrice;

    private Boolean active;
}
