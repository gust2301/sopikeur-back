package sn.sopikeur.dto.response.admin;

import java.math.BigDecimal;
import java.util.List;

public record DashboardStatsDto(
    long totalProducts,
    long totalOrders,
    long pendingQuotes,
    long pendingContacts,
    long pendingPreorders,
    BigDecimal totalRevenue,
    List<RecentOrderDto> recentOrders
) {
    public record RecentOrderDto(
        Long id,
        String reference,
        String customerName,
        String customerEmail,
        String customerPhone,
        String status,
        BigDecimal totalAmount,
        String createdAt
    ) {}
}
