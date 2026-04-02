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
import sn.sopikeur.dto.request.admin.AddOrderItemRequest;
import sn.sopikeur.dto.response.admin.OrderAdminResponseDto;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.order.OrderItem;
import sn.sopikeur.entity.order.OrderStatus;
import sn.sopikeur.repo.ProductRepository;
import sn.sopikeur.repo.order.OrderItemRepository;
import sn.sopikeur.repo.order.OrderRepository;

@Service
@RequiredArgsConstructor
public class OrderAdminService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public PageResponse<OrderAdminResponseDto> list(int page, int size, String statusParam) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<OrderEntity> result;
        if (statusParam != null && !statusParam.isBlank()) {
            try {
                OrderStatus status = OrderStatus.fromValue(statusParam);
                result = orderRepository.findByStatus(status, pageable);
            } catch (IllegalArgumentException e) {
                result = orderRepository.findAll(pageable);
            }
        } else {
            result = orderRepository.findAll(pageable);
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
    public OrderAdminResponseDto addItem(Long orderId, AddOrderItemRequest request) {
        OrderEntity order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Commande introuvable"));
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new NotFoundException("Produit introuvable"));

        BigDecimal unitPrice = product.getPrice();
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setSkuSnapshot(product.getSku() != null ? product.getSku() : product.getSlug());
        item.setUnit(product.getUnit() != null ? product.getUnit() : "pièce");
        item.setQty(request.getQuantity());
        item.setUnitPriceSnapshot(unitPrice);
        item.setLineTotalSnapshot(lineTotal);
        orderItemRepository.save(item);

        return getById(orderId);
    }

    @Transactional
    public OrderAdminResponseDto updateStatus(Long id, String statusParam) {
        OrderEntity order = orderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Commande introuvable"));
        order.setStatus(OrderStatus.fromValue(statusParam));
        return toDto(orderRepository.save(order));
    }

    private OrderAdminResponseDto toDto(OrderEntity o) {
        List<OrderItem> rawItems = o.getItems();

        BigDecimal totalAmount = rawItems.stream()
            .map(OrderItem::getLineTotalSnapshot)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

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

        String reference = o.getOrderNumber() != null ? o.getOrderNumber() : o.getPublicId();
        String createdAtStr = o.getCreatedAt() != null ? o.getCreatedAt().toString() : null;

        return OrderAdminResponseDto.builder()
            // identité
            .id(o.getId())
            .publicId(o.getPublicId())
            .orderNumber(o.getOrderNumber())
            .reference(reference)
            .status(o.getStatus() != null ? o.getStatus().name() : null)
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
                .build())
            .totalAmount(totalAmount)
            // dates (nested + plat)
            .timestamps(OrderAdminResponseDto.TimestampsDto.builder()
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build())
            .createdAt(createdAtStr)
            .build();
    }
}
