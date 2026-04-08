package sn.sopikeur.dto.response.admin;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record DashboardSummaryDto(
    Long ordersCount,
    BigDecimal grossRevenue,
    BigDecimal paidRevenue,
    BigDecimal actualCosts,
    BigDecimal forecastCosts,
    BigDecimal grossMargin,
    BigDecimal netEstimated
) {
}
