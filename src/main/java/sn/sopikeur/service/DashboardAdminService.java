package sn.sopikeur.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.sopikeur.dto.response.admin.DashboardSummaryDto;
import sn.sopikeur.dto.response.admin.DashboardStatsDto;
import sn.sopikeur.dto.response.admin.DashboardStatsDto.RecentOrderDto;
import sn.sopikeur.entity.leads.ContactStatus;
import sn.sopikeur.entity.leads.PreorderStatus;
import sn.sopikeur.entity.leads.QuoteStatus;
import sn.sopikeur.entity.order.OrderExpenseKind;
import sn.sopikeur.entity.order.OrderItem;
import sn.sopikeur.entity.order.OrderStatus;
import sn.sopikeur.repo.ContactMessageRepository;
import sn.sopikeur.repo.PreorderRequestRepository;
import sn.sopikeur.repo.ProductRepository;
import sn.sopikeur.repo.QuoteRequestRepository;
import sn.sopikeur.repo.order.OrderExpenseRepository;
import sn.sopikeur.repo.order.OrderRepository;

@Service
@RequiredArgsConstructor
public class DashboardAdminService {
    private static final List<OrderStatus> ACTIVE_ORDER_STATUSES =
        List.copyOf(EnumSet.of(OrderStatus.PENDING_CONFIRMATION, OrderStatus.CONFIRMED));
    private static final List<OrderStatus> REVENUE_STATUSES =
        List.copyOf(EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.FULFILLED));

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final QuoteRequestRepository quoteRequestRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final PreorderRequestRepository preorderRequestRepository;
    private final OrderExpenseRepository orderExpenseRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDto getStats() {
        long totalProducts    = productRepository.count();
        long totalOrders      = orderRepository.countByStatusNot(OrderStatus.CANCELLED);
        long pendingOrders    = orderRepository.countByStatus(OrderStatus.PENDING_CONFIRMATION);
        long pendingQuotes    = quoteRequestRepository.countByStatus(QuoteStatus.NEW);
        long pendingContacts  = contactMessageRepository.countByStatus(ContactStatus.NEW);
        long pendingPreorders = preorderRequestRepository.countByStatus(PreorderStatus.NEW);
        var  totalRevenue     = safe(orderRepository.sumAmountTotalByStatusInAndCreatedAtBetween(REVENUE_STATUSES, null, null));

        List<RecentOrderDto> recentOrders = orderRepository
            .findByStatusInOrderByCreatedAtDesc(ACTIVE_ORDER_STATUSES, PageRequest.of(0, 5))
            .stream()
            .map(o -> {
                BigDecimal totalAmount = o.getItems().stream()
                    .map(OrderItem::getLineTotalSnapshot)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                return new RecentOrderDto(
                    o.getId(),
                    o.getOrderNumber() != null ? o.getOrderNumber() : o.getPublicId(),
                    o.getFullName(),
                    o.getEmail(),
                    o.getPhone(),
                    o.getStatus() != null ? o.getStatus().name() : null,
                    totalAmount,
                    o.getCreatedAt() != null ? o.getCreatedAt().toString() : null
                );
            })
            .toList();

        return new DashboardStatsDto(
            totalProducts,
            totalOrders,
            pendingOrders,
            pendingQuotes,
            pendingContacts,
            pendingPreorders,
            totalRevenue,
            recentOrders
        );
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary(LocalDate startDate, LocalDate endDate, Authentication authentication) {
        OffsetDateTime startAt = startDate != null ? startDate.atStartOfDay().atOffset(ZoneOffset.UTC) : null;
        OffsetDateTime endAt = endDate != null ? endDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC) : null;

        long ordersCount = orderRepository.countByStatusInAndCreatedAtBetween(REVENUE_STATUSES, startAt, endAt);
        BigDecimal grossRevenue = safe(orderRepository.sumAmountTotalByStatusInAndCreatedAtBetween(REVENUE_STATUSES, startAt, endAt));
        BigDecimal paidRevenue = safe(orderRepository.sumAmountPaidByStatusInAndCreatedAtBetween(REVENUE_STATUSES, startAt, endAt));
        BigDecimal actualCosts = safe(orderExpenseRepository.sumAmountsByKindAndExpenseDateBetween(OrderExpenseKind.ACTUAL, startDate, endDate));
        BigDecimal forecastCosts = safe(orderExpenseRepository.sumAmountsByKindAndExpenseDateBetween(OrderExpenseKind.FORECAST, startDate, endDate));
        BigDecimal grossMargin = grossRevenue.subtract(actualCosts);
        BigDecimal netEstimated = grossMargin.subtract(forecastCosts);

        if (isSuperAdmin(authentication)) {
            return DashboardSummaryDto.builder()
                .ordersCount(ordersCount)
                .grossRevenue(grossRevenue)
                .paidRevenue(paidRevenue)
                .actualCosts(actualCosts)
                .forecastCosts(forecastCosts)
                .grossMargin(grossMargin)
                .netEstimated(netEstimated)
                .build();
        }

        return DashboardSummaryDto.builder()
            .ordersCount(ordersCount)
            .grossRevenue(null)
            .paidRevenue(null)
            .actualCosts(null)
            .forecastCosts(null)
            .grossMargin(null)
            .netEstimated(netEstimated)
            .build();
    }

    private boolean isSuperAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_SUPER_ADMIN"::equals);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
