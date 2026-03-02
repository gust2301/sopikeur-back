package sn.sopikeur.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.sopikeur.common.error.NotFoundException;
import sn.sopikeur.common.pagination.PageResponse;
import sn.sopikeur.dto.response.admin.OrderAdminResponseDto;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.order.OrderItem;
import sn.sopikeur.entity.order.OrderStatus;
import sn.sopikeur.entity.payment.PaymentIntentEntity;
import sn.sopikeur.entity.payment.PaymentIntentStatus;
import sn.sopikeur.repo.order.OrderRepository;
import sn.sopikeur.repo.payment.PaymentIntentRepository;

@Service
@RequiredArgsConstructor
public class OrderAdminService {

    private final OrderRepository orderRepository;
    private final PaymentIntentRepository paymentIntentRepository;

    @Transactional(readOnly = true)
    public PageResponse<OrderAdminResponseDto> list(int page, int size, String statusParam) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<OrderEntity> result;
        if (statusParam != null && !statusParam.isBlank()) {
            try {
                OrderStatus status = OrderStatus.valueOf(statusParam.toUpperCase());
                // Ne jamais exposer les drafts en backoffice, même si explicitement demandé
                if (status == OrderStatus.DRAFT_PENDING_PAYMENT) {
                    result = Page.empty(pageable);
                } else {
                    result = orderRepository.findByOrderStatus(status, pageable);
                }
            } catch (IllegalArgumentException e) {
                result = orderRepository.findByOrderStatusNot(OrderStatus.DRAFT_PENDING_PAYMENT, pageable);
            }
        } else {
            result = orderRepository.findByOrderStatusNot(OrderStatus.DRAFT_PENDING_PAYMENT, pageable);
        }

        return PageResponse.<OrderAdminResponseDto>builder()
            .items(result.getContent().stream().map(this::toDto).toList())
            .page(safePage + 1)
            .size(safeSize)
            .total(result.getTotalElements())
            .totalPages(result.getTotalPages())
            .build();
    }

    @Transactional(readOnly = true)
    public OrderAdminResponseDto getById(Long id) {
        OrderEntity order = orderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Commande introuvable"));
        return toDto(order);
    }

    @Transactional
    public OrderAdminResponseDto updateStatus(Long id, String statusParam) {
        OrderEntity order = orderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Commande introuvable"));
        OrderStatus newStatus = OrderStatus.valueOf(statusParam.toUpperCase());
        order.setOrderStatus(newStatus);
        order.setLegacyStatus(statusParam.toUpperCase());
        return toDto(orderRepository.save(order));
    }

    private OrderAdminResponseDto toDto(OrderEntity o) {
        List<OrderItem> rawItems = o.getItems();

        // Fall back to summing items if amountTotal is not set (legacy orders)
        BigDecimal itemsTotal = rawItems.stream()
            .map(OrderItem::getLineTotalSnapshot)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = o.getAmountTotal() != null ? o.getAmountTotal() : itemsTotal;

        // Fetch payment intents first to compute accurate amountPaid
        List<PaymentIntentEntity> paymentIntents = paymentIntentRepository.findByOrderId(o.getId());

        // Recalculate amountPaid from SUCCEEDED intents; fall back to stored field
        BigDecimal paidFromIntents = paymentIntents.stream()
            .filter(p -> PaymentIntentStatus.SUCCEEDED.equals(p.getStatus()))
            .map(p -> BigDecimal.valueOf(p.getAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal storedPaid = o.getAmountPaid() != null ? o.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal amountPaid = paidFromIntents.compareTo(BigDecimal.ZERO) > 0 ? paidFromIntents : storedPaid;
        BigDecimal amountDue  = totalAmount.subtract(amountPaid).max(BigDecimal.ZERO);

        List<OrderAdminResponseDto.ItemDto> items = rawItems.stream()
            .map(i -> OrderAdminResponseDto.ItemDto.builder()
                .productId(i.getProduct().getId())
                .productName(i.getProduct().getName())
                .sku(i.getSkuSnapshot())
                .unit(i.getUnit())
                .quantity(i.getQty())
                .unitPrice(i.getUnitPriceSnapshot())
                .lineTotal(i.getLineTotalSnapshot())
                .build())
            .toList();

        // Payment history
        List<OrderAdminResponseDto.PaymentIntentDto> payments = paymentIntents.stream()
            .map(p -> OrderAdminResponseDto.PaymentIntentDto.builder()
                .publicId(p.getPublicId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .purpose(p.getPurpose() != null ? p.getPurpose().name() : null)
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .checkoutUrl(p.getCheckoutUrl())
                .createdAt(p.getCreatedAt())
                .build())
            .toList();

        String reference    = o.getOrderNumber() != null ? o.getOrderNumber() : o.getPublicId();
        String createdAtStr = o.getCreatedAt() != null ? o.getCreatedAt().toString() : null;
        // Effective status: prefer new orderStatus field, fall back to legacy status string
        String effectiveStatus = o.getOrderStatus() != null
            ? o.getOrderStatus().name()
            : o.getLegacyStatus();

        return OrderAdminResponseDto.builder()
            // identité
            .id(o.getId())
            .publicId(o.getPublicId())
            .orderNumber(o.getOrderNumber())
            .reference(reference)
            // status (backward compat field = same as orderStatus)
            .status(effectiveStatus)
            .orderStatus(effectiveStatus)
            .paymentStatus(o.getPaymentStatus() != null ? o.getPaymentStatus().name() : "UNPAID")
            .paymentPlan(o.getPaymentPlan() != null ? o.getPaymentPlan().name() : "CASH_ON_DELIVERY")
            .paymentMethodSelected(o.getPaymentMethodSelected())
            // client (nested + plat pour rétrocompat liste)
            .customer(OrderAdminResponseDto.CustomerDto.builder()
                .fullName(o.getFullName())
                .phone(o.getPhone())
                .email(o.getEmail())
                .build())
            .customerName(o.getFullName())
            .customerEmail(o.getEmail())
            .customerPhone(o.getPhone())
            // livraison
            .delivery(OrderAdminResponseDto.DeliveryDto.builder()
                .cityZone(o.getCityZone())
                .needsInstallation(o.isNeedsInstallation())
                .note(o.getNote())
                .deliveryJson(o.getDeliveryJson())
                .build())
            // articles
            .items(items)
            // montants (nested + plat)
            .amounts(OrderAdminResponseDto.AmountsDto.builder()
                .totalAmount(totalAmount)
                .amountPaid(amountPaid)
                .amountDue(amountDue)
                .depositAmount(o.getDepositAmount())
                .build())
            .totalAmount(totalAmount)
            .amountPaid(amountPaid)
            .amountDue(amountDue)
            .depositAmount(o.getDepositAmount())
            // dates (nested + plat)
            .timestamps(OrderAdminResponseDto.TimestampsDto.builder()
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build())
            .createdAt(createdAtStr)
            // paiements
            .payments(payments)
            .build();
    }
}
