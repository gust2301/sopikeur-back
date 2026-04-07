package sn.sopikeur.dto.request.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import sn.sopikeur.entity.order.OrderExpenseKind;

@Data
public class OrderExpenseUpsertRequest {
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal amount;

    @NotBlank
    private String category;

    @NotNull
    private OrderExpenseKind kind;

    @NotNull
    private LocalDate date;

    private String note;
}
