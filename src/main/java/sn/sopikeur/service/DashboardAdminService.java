package sn.sopikeur.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.sopikeur.dto.response.admin.DashboardStatsDto;
import sn.sopikeur.dto.response.admin.DashboardStatsDto.RecentOrderDto;
import sn.sopikeur.entity.leads.ContactStatus;
import sn.sopikeur.entity.leads.PreorderStatus;
import sn.sopikeur.entity.leads.QuoteStatus;
import sn.sopikeur.entity.order.OrderItem;
import sn.sopikeur.entity.order.OrderStatus;
import sn.sopikeur.repo.ContactMessageRepository;
import sn.sopikeur.repo.PreorderRequestRepository;
import sn.sopikeur.repo.ProductRepository;
import sn.sopikeur.repo.QuoteRequestRepository;
import sn.sopikeur.repo.order.OrderItemRepository;
import sn.sopikeur.repo.order.OrderRepository;

@Service
@RequiredArgsConstructor
public class DashboardAdminService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final QuoteRequestRepository quoteRequestRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final PreorderRequestRepository preorderRequestRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDto getStats() {
        long totalProducts    = productRepository.count();
        long totalOrders      = orderRepository.count();
        long pendingOrders    = orderRepository.countByOrderStatus(OrderStatus.SUBMITTED);
        long pendingQuotes    = quoteRequestRepository.countByStatus(QuoteStatus.NEW);
        long pendingContacts  = contactMessageRepository.countByStatus(ContactStatus.NEW);
        long pendingPreorders = preorderRequestRepository.countByStatus(PreorderStatus.NEW);
        var  totalRevenue     = orderItemRepository.sumRevenue();

        List<RecentOrderDto> recentOrders = orderRepository
            .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5))
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
                    o.getOrderStatus() != null ? o.getOrderStatus().name() : o.getLegacyStatus(),
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
}
