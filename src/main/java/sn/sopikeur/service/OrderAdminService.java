package sn.sopikeur.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
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
import sn.sopikeur.dto.request.admin.AddOrderServiceRequest;
import sn.sopikeur.dto.request.admin.MarkOrderDeliveredRequest;
import sn.sopikeur.dto.request.admin.MarkOrderInstalledRequest;
import sn.sopikeur.dto.request.admin.UpdateOrderItemRequest;
import sn.sopikeur.dto.request.admin.UpdateOrderDeliveryRequest;
import sn.sopikeur.dto.request.admin.UpdateOrderDetailsRequest;
import sn.sopikeur.dto.response.admin.OrderAdminResponseDto;
import sn.sopikeur.dto.response.admin.OrderPaymentAdminResponseDto;
import sn.sopikeur.entity.auth.AdminUserEntity;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.entity.catalog.ServiceTypeEntity;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.order.OrderItem;
import sn.sopikeur.entity.order.OrderLineType;
import sn.sopikeur.entity.order.OrderPaymentEntity;
import sn.sopikeur.entity.order.OrderStatus;
import sn.sopikeur.entity.order.PaymentStatus;
import sn.sopikeur.repo.AdminUserRepository;
import sn.sopikeur.repo.ProductRepository;
import sn.sopikeur.repo.ServiceTypeRepository;
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
    private final ServiceTypeRepository serviceTypeRepository;
    private final AdminUserRepository adminUserRepository;
    private final ObjectMapper objectMapper;
    private final TrackingProperties trackingProperties;
    private final DocumentNumberService documentNumberService;
    private final ProductPricingService productPricingService;

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
            .items(result.getContent().stream().map(order -> toDto(order, false)).toList())
            .page(safePage + 1)
            .size(safeSize)
            .total(result.getTotalElements())
            .totalPages(result.getTotalPages())
            .build();
    }

    @Transactional(readOnly = true)
    public OrderAdminResponseDto getById(Long id) {
        OrderEntity order = orderRepository.findDetailedById(id)
            .orElseThrow(() -> new NotFoundException("Commande introuvable"));
        return toDto(order, true);
    }

    @Transactional
    public OrderAdminResponseDto addItem(Long orderId, AddOrderItemRequest request) {
        OrderEntity order = findOrderWithItems(orderId);
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new NotFoundException("Produit introuvable"));
        BigDecimal currentTotal = resolvePersistedTotal(order);

        BigDecimal unitPrice = productPricingService.resolveEffectivePrice(product);
        BigDecimal quantity = BigDecimal.valueOf(request.getQuantity());
        BigDecimal lineTotal = unitPrice.multiply(quantity);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setLineType(OrderLineType.PRODUCT);
        item.setProduct(product);
        item.setDisplayName(product.getName());
        item.setSkuSnapshot(product.getSku() != null ? product.getSku() : product.getSlug());
        item.setUnit(product.getUnit() != null ? product.getUnit() : "piece");
        item.setQty(quantity);
        item.setUnitPriceSnapshot(unitPrice);
        item.setLineTotalSnapshot(lineTotal);
        orderItemRepository.save(item);

        order.getItems().add(item);
        syncFinancials(order, currentTotal.add(lineTotal), resolvePaidAmount(order.getId()));
        orderRepository.save(order);

        return toDto(order, true);
    }

    @Transactional
    public OrderAdminResponseDto addService(Long orderId, AddOrderServiceRequest request) {
        OrderEntity order = findOrderWithItems(orderId);
        BigDecimal currentTotal = resolvePersistedTotal(order);
        BigDecimal legacyInstallationFallback = resolveLegacyInstallationFallback(order);
        Product serviceProduct = resolveServiceProduct(request);
        ServiceTypeEntity legacyServiceType = resolveLegacyServiceType(request);

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("La quantite doit etre superieure a zero.");
        }
        if (request.getUnitPrice() == null || request.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix unitaire doit etre strictement positif.");
        }
        if (isInstallationService(serviceProduct, legacyServiceType) && !resolveInstallationRequested(order)) {
            throw new IllegalArgumentException("Impossible d'ajouter un service d'installation si la pose n'est pas demandee.");
        }

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setLineType(OrderLineType.SERVICE);
        item.setProduct(serviceProduct);
        item.setServiceType(legacyServiceType);
        item.setDisplayName(serviceProduct.getName());
        item.setSkuSnapshot(blankFallback(serviceProduct.getSku(), serviceProduct.getSlug()));
        item.setUnit(trimToNull(serviceProduct.getUnit()) != null ? trimToNull(serviceProduct.getUnit()) : "service");
        BigDecimal quantity = BigDecimal.valueOf(request.getQuantity());
        item.setQty(quantity);
        item.setUnitPriceSnapshot(request.getUnitPrice());
        item.setLineTotalSnapshot(request.getUnitPrice().multiply(quantity));
        item.setLineNote(trimToNull(request.getNote()));
        orderItemRepository.save(item);

        order.getItems().add(item);
        syncDerivedInstallationAmount(order);
        BigDecimal newTotal = currentTotal.add(item.getLineTotalSnapshot());
        if (isInstallationService(serviceProduct, legacyServiceType)) {
            newTotal = currentTotal.subtract(legacyInstallationFallback).add(item.getLineTotalSnapshot());
        }
        syncFinancials(order, newTotal, resolvePaidAmount(order.getId()));
        orderRepository.save(order);

        return toDto(order, true);
    }

    @Transactional
    public OrderAdminResponseDto updateItem(Long orderId, Long itemId, UpdateOrderItemRequest request) {
        OrderEntity order = findOrderWithItems(orderId);
        OrderItem item = orderItemRepository.findByIdAndOrderId(itemId, orderId)
            .orElseThrow(() -> new NotFoundException("Ligne de commande introuvable"));

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La quantite doit etre superieure a zero.");
        }
        if (request.getUnitPrice() == null || request.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le prix unitaire ne peut pas etre negatif.");
        }

        item.setQty(request.getQuantity());
        item.setUnitPriceSnapshot(request.getUnitPrice());
        item.setLineTotalSnapshot(request.getUnitPrice().multiply(request.getQuantity()));
        item.setLineNote(trimToNull(request.getNote()));
        orderItemRepository.save(item);

        syncDerivedInstallationAmount(order);
        syncFinancials(order, computeItemsTotal(order), resolvePaidAmount(order.getId()));
        orderRepository.save(order);

        return toDto(order, true);
    }

    @Transactional
    public void deleteItem(Long orderId, Long itemId) {
        OrderEntity order = findOrderWithItems(orderId);
        OrderItem item = orderItemRepository.findByIdAndOrderId(itemId, orderId)
            .orElseThrow(() -> new NotFoundException("Ligne de commande introuvable"));

        order.getItems().removeIf(existing -> existing.getId().equals(itemId));
        orderItemRepository.delete(item);

        syncDerivedInstallationAmount(order);
        syncFinancials(order, computeItemsTotal(order), resolvePaidAmount(order.getId()));
        orderRepository.save(order);
    }

    @Transactional
    public OrderAdminResponseDto updateStatus(Long id, String statusParam) {
        OrderEntity order = findOrder(id);
        order.setStatus(OrderStatus.fromValue(statusParam));
        return toDto(orderRepository.save(order), false);
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
        applyLegacyInstallationAmount(order, request.getInstallationAmount());
        order.setInternalNote(trimToNull(request.getInternalNote()));
        syncDerivedInstallationAmount(order);
        syncFinancials(order, resolvePersistedTotal(order), resolvePaidAmount(order.getId()));

        return toDto(orderRepository.save(order), true);
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
        applyLegacyInstallationAmount(order, request.getInstallationAmount());
        order.setInternalNote(trimToNull(request.getInternalNote()));
        syncDerivedInstallationAmount(order);
        syncFinancials(order, resolvePersistedTotal(order), resolvePaidAmount(order.getId()));

        return toDto(orderRepository.save(order), true);
    }

    @Transactional
    public OrderAdminResponseDto markDelivered(Long id, MarkOrderDeliveredRequest request) {
        OrderEntity order = findOrder(id);
        LocalDateTime deliveredAt = request != null && request.getDeliveredAt() != null ? request.getDeliveredAt() : LocalDateTime.now();
        order.setDeliveredAt(deliveredAt);
        if (request != null && trimToNull(request.getNote()) != null) {
            order.setDeliveryNote(trimToNull(request.getNote()));
        }
        return toDto(orderRepository.save(order), true);
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
        return toDto(orderRepository.save(order), true);
    }

    @Transactional
    public OrderAdminResponseDto recordPayment(Long id, BigDecimal amountPaid) {
        OrderEntity order = orderRepository.findDetailedWithLockById(id)
            .orElseThrow(() -> new NotFoundException("Commande introuvable"));

        if (amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du paiement doit etre strictement positif.");
        }

        ensureLegacyPaymentIfNeeded(order);

        BigDecimal total = resolvePersistedTotal(order);
        BigDecimal currentPaid = resolvePaidAmount(order.getId());
        BigDecimal newPaid = currentPaid.add(amountPaid);

        if (newPaid.compareTo(total) > 0) {
            throw new IllegalArgumentException("Le montant saisi depasse le restant du de la commande.");
        }

        OrderPaymentEntity payment = new OrderPaymentEntity();
        payment.setOrder(order);
        payment.setReceiptNumber(documentNumberService.nextReceiptNumber(Year.now().getValue()));
        payment.setAmount(amountPaid);
        payment.setMethod(normalizeMethod(order.getPaymentMethodSelected()));
        payment.setCreatedBy(resolveActor());
        payment.setCreatedByAdminUserId(resolveActorAdminUserId());
        orderPaymentRepository.save(payment);

        syncFinancials(order, total, resolvePaidAmount(order.getId()));
        orderRepository.save(order);

        return toDto(order, true);
    }

    @Transactional(readOnly = true)
    public List<OrderPaymentAdminResponseDto> getPayments(Long orderId) {
        findOrder(orderId);
        return mapPayments(orderPaymentRepository.findByOrderIdOrderByCreatedAtDescIdDesc(orderId), resolvePersistedTotal(findOrder(orderId)));
    }

    @Transactional
    public String issueInvoice(Long orderId) {
        OrderEntity order = orderRepository.findDetailedWithLockById(orderId)
            .orElseThrow(() -> new NotFoundException("Commande introuvable"));
        ensureInvoiceIssued(order);
        orderRepository.save(order);
        return order.getInvoiceNumber();
    }

    @Transactional
    public InvoiceDocumentData getInvoiceDocument(Long orderId) {
        OrderEntity order = orderRepository.findDetailedWithLockById(orderId)
            .orElseThrow(() -> new NotFoundException("Commande introuvable"));
        ensureInvoiceIssued(order);
        syncDerivedInstallationAmount(order);
        syncFinancials(order, resolvePersistedTotal(order), resolvePaidAmount(order.getId()));
        orderRepository.save(order);
        List<OrderPaymentAdminResponseDto> payments = mapPayments(
            orderPaymentRepository.findByOrderIdOrderByCreatedAtDescIdDesc(orderId),
            resolvePersistedTotal(order)
        );
        return new InvoiceDocumentData(order, payments, order.getAmountPaid(), order.getAmountDue());
    }

    @Transactional(readOnly = true)
    public ReceiptDocumentData getReceiptDocument(Long paymentId) {
        OrderPaymentEntity payment = orderPaymentRepository.findById(paymentId)
            .orElseThrow(() -> new NotFoundException("Paiement introuvable"));

        if (payment.getReceiptNumber() == null || payment.getReceiptNumber().isBlank()) {
            throw new IllegalArgumentException("Aucun recu PDF n'est disponible pour ce paiement.");
        }

        OrderEntity order = orderRepository.findDetailedById(payment.getOrder().getId())
            .orElseThrow(() -> new NotFoundException("Commande introuvable"));

        List<OrderPaymentAdminResponseDto> payments = mapPayments(
            orderPaymentRepository.findByOrderIdOrderByCreatedAtDescIdDesc(order.getId()),
            resolvePersistedTotal(order)
        );

        OrderPaymentAdminResponseDto paymentDto = payments.stream()
            .filter(item -> item.id().equals(paymentId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Paiement introuvable"));

        return new ReceiptDocumentData(order, paymentDto);
    }

    private OrderEntity findOrder(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Commande introuvable"));
    }

    private OrderEntity findOrderWithItems(Long id) {
        return orderRepository.findDetailedById(id)
            .orElseThrow(() -> new NotFoundException("Commande introuvable"));
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }
        return authentication.getName();
    }

    private Long resolveActorAdminUserId() {
        String actor = resolveActor();
        if (actor == null) {
            return null;
        }
        return adminUserRepository.findByEmail(actor)
            .map(AdminUserEntity::getId)
            .orElse(null);
    }

    private OrderAdminResponseDto toDto(OrderEntity order, boolean includePayments) {
        DeliveryDetails deliveryDetails = parseDeliveryDetails(order.getDeliveryJson());
        BigDecimal apiTotalAmount = resolvePersistedTotal(order);
        boolean installationRequested = resolveInstallationRequested(order);
        BigDecimal paidAmount = includePayments || order.getAmountPaid() != null ? resolvePaidAmount(order.getId()) : order.getAmountPaid();
        BigDecimal dueAmount = apiTotalAmount.subtract(paidAmount != null ? paidAmount : BigDecimal.ZERO).max(BigDecimal.ZERO);
        BigDecimal installationAmount = resolveDerivedInstallationAmount(order);

        List<OrderAdminResponseDto.ItemDto> items = order.getItems().stream()
            .filter(i -> i.getLineType() == null || i.getLineType() == OrderLineType.PRODUCT)
            .map(i -> OrderAdminResponseDto.ItemDto.builder()
                .productId(i.getProduct() != null ? i.getProduct().getId() : null)
                .productName(resolveLineDisplayName(i))
                .sku(i.getSkuSnapshot())
                .unit(i.getUnit())
                .quantity(i.getQty())
                .unitPrice(i.getUnitPriceSnapshot())
                .lineTotal(i.getLineTotalSnapshot())
                .build())
            .toList();
        List<OrderAdminResponseDto.OrderLineDto> orderLines = order.getItems().stream()
            .map(this::toOrderLineDto)
            .toList();

        List<OrderPaymentAdminResponseDto> payments = includePayments
            ? mapPayments(orderPaymentRepository.findByOrderIdOrderByCreatedAtDescIdDesc(order.getId()), apiTotalAmount)
            : List.of();

        String reference = order.getOrderNumber() != null ? order.getOrderNumber() : order.getPublicId();

        return OrderAdminResponseDto.builder()
            .id(order.getId())
            .publicId(order.getPublicId())
            .orderNumber(order.getOrderNumber())
            .invoiceNumber(order.getInvoiceNumber())
            .invoiceIssuedAt(order.getInvoiceIssuedAt())
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
                .installationAmount(installationAmount)
                .deliveredAt(order.getDeliveredAt())
                .installedAt(order.getInstalledAt())
                .internalNote(order.getInternalNote())
                .deliveryJson(order.getDeliveryJson())
                .build())
            .items(items)
            .orderLines(orderLines)
            .amounts(OrderAdminResponseDto.AmountsDto.builder()
                .totalAmount(apiTotalAmount)
                .amountPaid(paidAmount)
                .amountDue(dueAmount)
                .depositAmount(order.getDepositAmount())
                .installationAmount(installationAmount)
                .build())
            .totalAmount(apiTotalAmount)
            .amountPaid(paidAmount)
            .amountDue(dueAmount)
            .depositAmount(order.getDepositAmount())
            .installationAmount(installationAmount)
            .paymentStatus(resolvePaymentStatus(order, paidAmount, apiTotalAmount))
            .paymentPlan(order.getPaymentPlan() != null ? order.getPaymentPlan().name() : null)
            .paymentMethodSelected(order.getPaymentMethodSelected())
            .timestamps(OrderAdminResponseDto.TimestampsDto.builder()
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build())
            .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null)
            .trackingUrl(buildTrackingUrl(order.getPublicId()))
            .payments(payments)
            .build();
    }

    private List<OrderPaymentAdminResponseDto> mapPayments(List<OrderPaymentEntity> entities, BigDecimal totalAmount) {
        List<OrderPaymentEntity> ordered = new ArrayList<>(entities);
        ordered.sort(Comparator.comparing(OrderPaymentEntity::getCreatedAt).thenComparing(OrderPaymentEntity::getId));

        BigDecimal runningPaid = BigDecimal.ZERO;
        List<OrderPaymentAdminResponseDto> mapped = new ArrayList<>();
        for (OrderPaymentEntity payment : ordered) {
            runningPaid = runningPaid.add(payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO);
            mapped.add(OrderPaymentAdminResponseDto.builder()
                .id(payment.getId())
                .receiptNumber(payment.getReceiptNumber())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .note(payment.getNote())
                .paidTotal(runningPaid)
                .dueTotal(totalAmount.subtract(runningPaid).max(BigDecimal.ZERO))
                .createdAt(payment.getCreatedAt())
                .build());
        }

        mapped.sort(Comparator.comparing(OrderPaymentAdminResponseDto::createdAt)
            .thenComparing(OrderPaymentAdminResponseDto::id)
            .reversed());
        return mapped;
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

    private void applyLegacyInstallationAmount(OrderEntity order, BigDecimal amount) {
        if (amount == null) {
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le montant de pose ne peut pas etre negatif.");
        }
        order.setInstallationAmount(amount);
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

    private BigDecimal computeItemsTotal(OrderEntity order) {
        return order.getItems().stream()
            .map(OrderItem::getLineTotalSnapshot)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal resolvePersistedTotal(OrderEntity order) {
        if (order.getAmountTotal() != null) {
            return order.getAmountTotal();
        }
        return computeItemsTotal(order).add(resolveLegacyInstallationFallback(order));
    }

    private BigDecimal resolvePaidAmount(Long orderId) {
        return orderPaymentRepository.sumAmountsByOrderId(orderId);
    }

    private void syncFinancials(OrderEntity order, BigDecimal total, BigDecimal paid) {
        BigDecimal safePaid = paid != null ? paid : BigDecimal.ZERO;
        order.setAmountTotal(total);
        order.setAmountPaid(safePaid);
        order.setAmountDue(total.subtract(safePaid).max(BigDecimal.ZERO));
        order.setPaymentStatus(resolvePaymentStatusEnum(safePaid, total));
    }

    private BigDecimal resolveLegacyInstallationFallback(OrderEntity order) {
        if (!resolveInstallationRequested(order) || hasInstallationServiceLine(order)) {
            return BigDecimal.ZERO;
        }
        return order.getInstallationAmount() != null ? order.getInstallationAmount() : BigDecimal.ZERO;
    }

    private BigDecimal resolveDerivedInstallationAmount(OrderEntity order) {
        if (!resolveInstallationRequested(order)) {
            return BigDecimal.ZERO;
        }
        BigDecimal serviceAmount = order.getItems().stream()
            .filter(this::isInstallationServiceLine)
            .map(OrderItem::getLineTotalSnapshot)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (serviceAmount.compareTo(BigDecimal.ZERO) > 0) {
            return serviceAmount;
        }
        return order.getInstallationAmount() != null ? order.getInstallationAmount() : BigDecimal.ZERO;
    }

    private void syncDerivedInstallationAmount(OrderEntity order) {
        if (!resolveInstallationRequested(order)) {
            order.setInstallationAmount(null);
            return;
        }
        BigDecimal derivedInstallationAmount = resolveDerivedInstallationAmount(order);
        order.setInstallationAmount(derivedInstallationAmount.compareTo(BigDecimal.ZERO) > 0 ? derivedInstallationAmount : null);
    }

    private boolean hasInstallationServiceLine(OrderEntity order) {
        return order.getItems().stream().anyMatch(this::isInstallationServiceLine);
    }

    private boolean isInstallationServiceLine(OrderItem item) {
        return item.getLineType() == OrderLineType.SERVICE
            && isInstallationService(item.getProduct(), item.getServiceType());
    }

    private boolean isInstallationService(Product product, ServiceTypeEntity serviceType) {
        String productSku = product != null ? trimToNull(product.getSku()) : null;
        if (productSku != null && "SRV-POSE".equalsIgnoreCase(productSku)) {
            return true;
        }
        return serviceType != null
            && trimToNull(serviceType.getCode()) != null
            && "INSTALLATION".equalsIgnoreCase(serviceType.getCode().trim());
    }

    private OrderAdminResponseDto.OrderLineDto toOrderLineDto(OrderItem item) {
        String serviceName = item.getServiceType() != null
            ? item.getServiceType().getName()
            : ((item.getLineType() == OrderLineType.SERVICE && item.getProduct() != null) ? item.getProduct().getName() : null);
        String serviceCode = item.getServiceType() != null
            ? item.getServiceType().getCode()
            : ((item.getLineType() == OrderLineType.SERVICE && item.getProduct() != null) ? item.getProduct().getSku() : null);
        return OrderAdminResponseDto.OrderLineDto.builder()
            .id(item.getId())
            .lineType((item.getLineType() != null ? item.getLineType() : OrderLineType.PRODUCT).name())
            .productId(item.getProduct() != null ? item.getProduct().getId() : null)
            .serviceTypeId(item.getServiceType() != null ? item.getServiceType().getId() : null)
            .code(item.getSkuSnapshot())
            .displayName(resolveLineDisplayName(item))
            .productName(item.getProduct() != null ? item.getProduct().getName() : null)
            .serviceName(serviceName)
            .serviceCode(serviceCode)
            .unit(item.getUnit())
            .quantity(item.getQty())
            .unitPrice(item.getUnitPriceSnapshot())
            .lineTotal(item.getLineTotalSnapshot())
            .note(item.getLineNote())
            .build();
    }

    private String resolveLineDisplayName(OrderItem item) {
        String displayName = trimToNull(item.getDisplayName());
        if (displayName != null) {
            return displayName;
        }
        if (item.getProduct() != null && trimToNull(item.getProduct().getName()) != null) {
            return item.getProduct().getName();
        }
        if (item.getServiceType() != null && trimToNull(item.getServiceType().getName()) != null) {
            return item.getServiceType().getName();
        }
        return item.getSkuSnapshot();
    }

    private Product resolveServiceProduct(AddOrderServiceRequest request) {
        if (request.getProductId() == null) {
            if (request.getServiceTypeId() == null) {
                throw new IllegalArgumentException("Aucun service selectionne.");
            }
            ServiceTypeEntity legacyServiceType = serviceTypeRepository.findById(request.getServiceTypeId())
                .orElseThrow(() -> new NotFoundException("Type de service introuvable"));
            String sku = mapLegacyServiceSku(legacyServiceType);
            return productRepository.findBySku(sku)
                .filter(product -> product.getType() == ProductType.SERVICE)
                .orElseThrow(() -> new NotFoundException("Produit service introuvable pour " + sku));
        }

        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new NotFoundException("Produit service introuvable"));
        if (product.getType() != ProductType.SERVICE) {
            throw new IllegalArgumentException("Le produit selectionne n'est pas un service.");
        }
        return product;
    }

    private ServiceTypeEntity resolveLegacyServiceType(AddOrderServiceRequest request) {
        if (request.getServiceTypeId() == null) {
            return null;
        }
        return serviceTypeRepository.findById(request.getServiceTypeId()).orElse(null);
    }

    private String mapLegacyServiceSku(ServiceTypeEntity serviceType) {
        String normalizedCode = trimToNull(serviceType.getCode());
        if (normalizedCode == null) {
            throw new IllegalArgumentException("Type de service introuvable.");
        }
        if ("INSTALLATION".equalsIgnoreCase(normalizedCode)) {
            return "SRV-POSE";
        }
        if ("DELIVERY".equalsIgnoreCase(normalizedCode) || "LIVRAISON".equalsIgnoreCase(normalizedCode)) {
            return "SRV-LIVRAISON";
        }
        throw new NotFoundException("Aucun produit service ne correspond au type " + normalizedCode);
    }

    private String blankFallback(String value, String fallback) {
        String trimmedValue = trimToNull(value);
        return trimmedValue != null ? trimmedValue : fallback;
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

    private void ensureLegacyPaymentIfNeeded(OrderEntity order) {
        if (orderPaymentRepository.countByOrderId(order.getId()) > 0) {
            return;
        }
        BigDecimal legacyPaid = order.getAmountPaid();
        if (legacyPaid == null || legacyPaid.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        OrderPaymentEntity legacyPayment = new OrderPaymentEntity();
        legacyPayment.setOrder(order);
        legacyPayment.setAmount(legacyPaid);
        legacyPayment.setMethod(normalizeMethod(order.getPaymentMethodSelected()));
        legacyPayment.setNote("Legacy migrated paid amount");
        legacyPayment.setCreatedBy("migration");
        orderPaymentRepository.save(legacyPayment);
    }

    private void ensureInvoiceIssued(OrderEntity order) {
        if (order.getInvoiceNumber() == null || order.getInvoiceNumber().isBlank()) {
            order.setInvoiceNumber(documentNumberService.nextInvoiceNumber(Year.now().getValue()));
        }
        if (order.getInvoiceIssuedAt() == null) {
            order.setInvoiceIssuedAt(LocalDateTime.now());
        }
    }

    private String resolvePaymentStatus(OrderEntity order, BigDecimal paidAmount, BigDecimal totalAmount) {
        PaymentStatus status = order.getPaymentStatus();
        if (status == null) {
            status = resolvePaymentStatusEnum(paidAmount != null ? paidAmount : BigDecimal.ZERO, totalAmount);
        }
        return status != null ? status.name() : null;
    }

    private PaymentStatus resolvePaymentStatusEnum(BigDecimal paid, BigDecimal total) {
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentStatus.UNPAID;
        }
        if (paid.compareTo(total) >= 0) {
            return PaymentStatus.PAID;
        }
        return PaymentStatus.PARTIALLY_PAID;
    }

    private String normalizeMethod(String method) {
        return trimToNull(method);
    }

    private record DeliveryPayload(String city, String area, String address) {
    }

    private record DeliveryDetails(String city, String zone, String address) {
    }

    public record InvoiceDocumentData(
        OrderEntity order,
        List<OrderPaymentAdminResponseDto> payments,
        BigDecimal paidTotal,
        BigDecimal dueTotal
    ) {
    }

    public record ReceiptDocumentData(
        OrderEntity order,
        OrderPaymentAdminResponseDto payment
    ) {
    }
}
