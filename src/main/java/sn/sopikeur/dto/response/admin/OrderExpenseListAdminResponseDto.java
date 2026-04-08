package sn.sopikeur.dto.response.admin;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

@Builder
public record OrderExpenseListAdminResponseDto(
    List<OrderExpenseAdminResponseDto> items,
    BigDecimal actualCosts,
    BigDecimal forecastCosts
) {
}
