package sn.sopikeur.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.sopikeur.common.error.NotFoundException;
import sn.sopikeur.common.pagination.PageResponse;
import sn.sopikeur.config.TrackingProperties;
import sn.sopikeur.dto.request.admin.AddOrderItemRequest;
import sn.sopikeur.dto.request.admin.MarkOrderDeliveredRequest;
import sn.sopikeur.dto.request.admin.MarkOrderInstalledRequest;
import sn.sopikeur.dto.request.admin.UpdateOrderDeliveryRequest;
import sn.sopikeur.dto.request.admin.UpdateOrderDetailsRequest;
import sn.sopikeur.dto.response.admin.OrderAdminResponseDto;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.order.OrderItem;
import sn.sopikeur.entity.order.OrderPaymentEntity;
import sn.sopikeur.entity.order.OrderStatus;
import sn.sopikeur.entity.order.PaymentStatus;
import sn.sopikeur.repo.ProductRepository;
import sn.sopikeur.repo.order.OrderItemRepository;
import sn.sopikeur.repo.order.OrderPaymentRepository;
import sn.sopikeur.repo.order.OrderRepository;

@Service
@RequiredArgsConstructor
public class OrderAdminService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;
    private final TrackingProperties trackingProperties;

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
        return toDto(findOrder(id));
    }

    @Transactional
    public OrderAdminResponseDto addItem(Long orderId, AddOrderItemRequest request) {
        OrderEntity order = findOrder(orderId);
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new NotFoundException("Produit introuvable"));

        BigDecimal unitPrice = product.getPrice();
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setSkuSnapshot(product.getSku() != null ? product.getSku() : product.getSlug());
        item.setUnit(product.getUnit() != null ? product.getUnit() : "piece");
        item.setQty(request.getQuantity());
        item.setUnitPriceSnapshot(unitPrice);
        item.setLineTotalSnapshot(lineTotal);
        orderItemRepository.save(item);

        return getById(orderId);
    }

    @Transactional
    public OrderAdminResponseDto updateStatus(Long id, String statusParam) {
        OrderEntity order = findOrder(id);
        order.setStatus(OrderStatus.fromValue(statusParam));
        return toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderAdminResponseDto updateDetails(Long id, UpdateOrderDetailsRequest request) {
        OrderEntity order = findOrder(id);

        order.setFullName(trimToNull(request.getFullName()));
        order.setPhone(trimToNull(request.getPhone()));
        order.setEmail(trimToNull(request.getEmail()));
        applyDeliveryFields(order, request.getCityZone(), request.getDeliveryCity(), request.getDeliveryZone(), request.getDeliveryAddress());
        order.setNote(trimToNull(request.getNote()));
        applyDeliveryEta(order, request.getDeliveryEtaDate() != null ? request.getDeliveryEtaDate() : request.getExpectedDeliveryDate());
        order.setDeliveryNote(trimToNull(request.getDeliveryNote()));
        applyInstallationRequested(order, request.getInstallationRequested());
        applyInstallationEta(order, request.getInstallationEtaDate() != null ? request.getInstallationEtaDate() : request.getInstallationDate());
        order.setInstallationNote(trimToNull(request.getInstallationNote()));
        order.setInternalNote(trimToNull(request.getInternalNote()));

        return toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderAdminResponseDto updateDelivery(Long id, UpdateOrderDeliveryRequest request) {
        OrderEntity order = findOrder(id);

        applyDeliveryFields(order, request.getCityZone(), request.getDeliveryCity(), request.getDeliveryZone(), request.getDeliveryAddress());
        applyDeliveryEta(order, request.getDeliveryEtaDate());
        order.setDeliveryNote(trimToNull(request.getDeliveryNote()));
        applyInstallationRequested(order, request.getInstallationRequested());
        applyInstallationEta(order, request.getInstallationEtaDate());
        order.setInstallationNote(trimToNull(request.getInstallationNote()));
        order.setInternalNote(trimToNull(request.getInternalNote()));

        return toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderAdminResponseDto markDelivered(Long id, MarkOrderDeliveredRequest request) {
        OrderEntity order = findOrder(id);
        LocalDateTime deliveredAt = request != null && request.getDeliveredAt() != null ? request.getDeliveredAt() : LocalDateTime.now();
        order.setDeliveredAt(deliveredAt);
        if (request != null && trimToNull(request.getNote()) != null) {
            order.setDeliveryNote(trimToNull(request.getNote()));
        }
        return toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderAdminResponseDto markInstalled(Long id, MarkOrderInstalledRequest request) {
        OrderEntity order = findOrder(id);
        if (!resolveInstallationRequested(order)) {
            throw new IllegalArgumentException("Installation non demandee pour cette commande");
        }

        LocalDateTime installedAt = request != null && request.getInstalledAt() != null ? request.getInstalledAt() : LocalDateTime.now();
        order.setInstalledAt(installedAt);
        if (request != null && trimToNull(request.getNote()) != null) {
            order.setInstallationNote(trimToNull(request.getNote()));
        }
        return toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderAdminResponseDto recordPayment(Long id, BigDecimal amountPaid) {
        OrderEntity order = orderRepository.findWithLockById(id)
            .orElseThrow(() -> new NotFoundException("Commande introuvable"));

        if (amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du paiement doit etre strictement positif.");
        }

        BigDecimal total = order.getAmountTotal();
        if (total == null) {
            total = order.getItems().stream()
                .map(OrderItem::getLineTotalSnapshot)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal currentPaid = order.getAmountPaid() != null ? order.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal newPaid = currentPaid.add(amountPaid);

        if (newPaid.compareTo(total) > 0) {
            throw new IllegalArgumentException("Le montant saisi depasse le restant du de la commande.");
        }

        order.setAmountPaid(newPaid);
        order.setAmountDue(total.subtract(newPaid).max(BigDecimal.ZERO));

        OrderPaymentEntity payment = new OrderPaymentEntity();
        payment.setOrder(order);
        payment.setAmount(amountPaid);
        payment.setMethod(order.getPaymentMethodSelected());
        payment.setCreatedBy(resolveActor());
        orderPaymentRepository.save(payment);

        if (newPaid.compareTo(BigDecimal.ZERO) <= 0) {
            order.setPaymentStatus(PaymentStatus.UNPAID);
        } else if (newPaid.compareTo(total) >= 0) {
            order.setPaymentStatus(PaymentStatus.PAID);
        } else {
            order.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        }

        return toDto(orderRepository.save(order));
    }

    private OrderEntity findOrder(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Commande introuvable"));
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }
        return authentication.getName();
    }

    private OrderAdminResponseDto toDto(OrderEntity order) {
        List<OrderItem> rawItems = order.getItems();
        DeliveryDetails deliveryDetails = parseDeliveryDetails(order.getDeliveryJson());

        BigDecimal totalAmount = rawItems.stream()
            .map(OrderItem::getLineTotalSnapshot)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal apiTotalAmount = order.getAmountTotal() != null ? order.getAmountTotal() : totalAmount;
        boolean installationRequested = resolveInstallationRequested(order);

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

        String reference = order.getOrderNumber() != null ? order.getOrderNumber() : order.getPublicId();

        return OrderAdminResponseDto.builder()
            .id(order.getId())
            .publicId(order.getPublicId())
            .orderNumber(order.getOrderNumber())
            .reference(reference)
            .status(order.getStatus() != null ? order.getStatus().name() : null)
            .customer(OrderAdminResponseDto.CustomerDto.builder()
                .fullName(order.getFullName())
                .phone(order.getPhone())
                .email(order.getEmail())
                .build())
            .customerName(order.getFullName())
            .customerEmail(order.getEmail())
            .customerPhone(order.getPhone())
            .delivery(OrderAdminResponseDto.DeliveryDto.builder()
                .cityZone(order.getCityZone())
                .city(deliveryDetails.city())
                .zone(deliveryDetails.zone())
                .address(deliveryDetails.address())
                .needsInstallation(order.isNeedsInstallation())
                .note(order.getNote())
                .expectedDeliveryDate(resolveDeliveryEta(order))
                .deliveryEtaDate(resolveDeliveryEta(order))
                .deliveryNote(order.getDeliveryNote())
                .installationRequested(installationRequested)
                .installationDate(resolveInstallationEta(order))
                .installationEtaDate(resolveInstallationEta(order))
                .installationNote(order.getInstallationNote())
                .deliveredAt(order.getDeliveredAt())
                .installedAt(order.getInstalledAt())
                .internalNote(order.getInternalNote())
                .deliveryJson(order.getDeliveryJson())
                .build())
            .items(items)
            .amounts(OrderAdminResponseDto.AmountsDto.builder()
                .totalAmount(apiTotalAmount)
                .amountPaid(order.getAmountPaid())
                .amountDue(order.getAmountDue())
                .depositAmount(order.getDepositAmount())
                .build())
            .totalAmount(apiTotalAmount)
            .amountPaid(order.getAmountPaid())
            .amountDue(order.getAmountDue())
            .depositAmount(order.getDepositAmount())
            .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
            .paymentPlan(order.getPaymentPlan() != null ? order.getPaymentPlan().name() : null)
            .paymentMethodSelected(order.getPaymentMethodSelected())
            .timestamps(OrderAdminResponseDto.TimestampsDto.builder()
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build())
            .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null)
            .trackingUrl(buildTrackingUrl(order.getPublicId()))
            .build();
    }

    private void applyDeliveryFields(OrderEntity order, String cityZone, String deliveryCity, String deliveryZone, String deliveryAddress) {
        order.setCityZone(resolveCityZone(cityZone, deliveryCity, deliveryZone));
        order.setDeliveryJson(buildDeliveryJson(deliveryCity, deliveryZone, deliveryAddress));
    }

    private void applyDeliveryEta(OrderEntity order, LocalDate etaDate) {
        order.setDeliveryEtaDate(etaDate);
        order.setExpectedDeliveryDate(etaDate);
    }

    private void applyInstallationEta(OrderEntity order, LocalDate etaDate) {
        order.setInstallationEtaDate(etaDate);
        order.setInstallationDate(etaDate);
    }

    private void applyInstallationRequested(OrderEntity order, Boolean requested) {
        if (requested == null) {
            return;
        }
        order.setInstallationRequested(requested);
        order.setNeedsInstallation(requested);
        if (!requested) {
            order.setInstallationEtaDate(null);
            order.setInstallationDate(null);
            order.setInstallationNote(null);
            order.setInstalledAt(null);
        }
    }

    private boolean resolveInstallationRequested(OrderEntity order) {
        return order.getInstallationRequested() != null ? order.getInstallationRequested() : order.isNeedsInstallation();
    }

    private LocalDate resolveDeliveryEta(OrderEntity order) {
        return order.getDeliveryEtaDate() != null ? order.getDeliveryEtaDate() : order.getExpectedDeliveryDate();
    }

    private LocalDate resolveInstallationEta(OrderEntity order) {
        return order.getInstallationEtaDate() != null ? order.getInstallationEtaDate() : order.getInstallationDate();
    }

    private String buildTrackingUrl(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return null;
        }
        String baseUrl = trackingProperties.baseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        return baseUrl.endsWith("/") ? baseUrl + "suivi/" + publicId : baseUrl + "/suivi/" + publicId;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveCityZone(String explicitCityZone, String deliveryCity, String deliveryZone) {
        String cityZone = trimToNull(explicitCityZone);
        if (cityZone != null) {
            return cityZone;
        }

        String city = trimToNull(deliveryCity);
        String zone = trimToNull(deliveryZone);
        if (city == null) {
            return null;
        }
        return zone == null ? city : city + " - " + zone;
    }

    private String buildDeliveryJson(String deliveryCity, String deliveryZone, String deliveryAddress) {
        DeliveryDetails details = new DeliveryDetails(
            trimToNull(deliveryCity),
            trimToNull(deliveryZone),
            trimToNull(deliveryAddress)
        );

        if (details.city() == null && details.zone() == null && details.address() == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(new DeliveryPayload(details.city(), details.zone(), details.address()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Impossible de serialiser deliveryJson", e);
        }
    }

    private DeliveryDetails parseDeliveryDetails(String deliveryJson) {
        if (deliveryJson == null || deliveryJson.isBlank()) {
            return new DeliveryDetails(null, null, null);
        }

        try {
            DeliveryPayload payload = objectMapper.readValue(deliveryJson, DeliveryPayload.class);
            return new DeliveryDetails(
                trimToNull(payload.city()),
                trimToNull(payload.area()),
                trimToNull(payload.address())
            );
        } catch (Exception ignored) {
            return new DeliveryDetails(null, null, null);
        }
    }

    private record DeliveryPayload(String city, String area, String address) {
    }

    private record DeliveryDetails(String city, String zone, String address) {
    }
}
