package sn.sopikeur.service.commerce;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sn.sopikeur.common.error.NotFoundException;
import sn.sopikeur.common.error.StockConflictException;
import sn.sopikeur.dto.request.publicapi.commerce.*;
import sn.sopikeur.dto.response.publicapi.commerce.CommerceCreateResponse;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.entity.leads.PreorderRequest;
import sn.sopikeur.entity.leads.PreorderStatus;
import sn.sopikeur.entity.leads.QuoteRequest;
import sn.sopikeur.entity.leads.QuoteStatus;
import sn.sopikeur.entity.leads.item.PreorderRequestItem;
import sn.sopikeur.entity.leads.item.QuoteRequestItem;
import sn.sopikeur.entity.leads.item.QuoteRequestPack;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.order.OrderItem;
import sn.sopikeur.entity.order.OrderPaymentPlan;
import sn.sopikeur.entity.order.OrderStatus;
import sn.sopikeur.entity.payment.PaymentStatus;
import sn.sopikeur.entity.stock.StockItem;
import sn.sopikeur.repo.*;
import sn.sopikeur.repo.order.OrderItemRepository;
import sn.sopikeur.repo.order.OrderRepository;
import sn.sopikeur.service.NotificationService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommerceService {
    private final QuoteRequestRepository quoteRequestRepository;
    private final PreorderRequestRepository preorderRequestRepository;
    private final QuoteRequestItemRepository quoteRequestItemRepository;
    private final QuoteRequestPackRepository quoteRequestPackRepository;
    private final PreorderRequestItemRepository preorderRequestItemRepository;
    private final ProductRepository productRepository;
    private final StockItemRepository stockItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @Transactional
    public CommerceCreateResponse createQuote(QuoteCreateRequest request) {
        if (!"quote".equalsIgnoreCase(request.getIntent()) && !"preorder".equalsIgnoreCase(request.getIntent())) {
            throw new IllegalArgumentException("intent must be quote or preorder");
        }
        DeliveryDto delivery = resolveDelivery(request.getDelivery(), request.getCityZone());
        QuoteRequest quote = new QuoteRequest();
        quote.setPublicId(UUID.randomUUID().toString());
        quote.setFullName(request.getCustomer().getFullName());
        quote.setPhone(request.getCustomer().getPhone());
        quote.setEmail(request.getCustomer().getEmail() == null ? "unknown@sopikeur.sn" : request.getCustomer().getEmail());
        quote.setProjectType(request.getProjectType());
        quote.setCityZone(toLegacyCityZone(delivery, request.getCityZone()));
        quote.setDeliveryJson(toDeliveryJson(delivery));
        quote.setNeedsInstallation(Boolean.TRUE.equals(request.getInstallRequested()));
        quote.setMessage(request.getMessage());
        quote.setIntent(request.getIntent().toLowerCase(Locale.ROOT));
        quote.setStatus(QuoteStatus.NEW);
        QuoteRequest saved = quoteRequestRepository.save(quote);

        for (CommerceItemCreateRequest item : request.getItems()) {
            Product product = resolveProduct(item);
            validateUnit(item.getUnit(), product);
            QuoteRequestItem quoteItem = new QuoteRequestItem();
            quoteItem.setQuoteRequest(saved);
            quoteItem.setProduct(product);
            quoteItem.setProductSlugSnapshot(product.getSlug());
            quoteItem.setSkuSnapshot(product.getSku());
            quoteItem.setQty(BigDecimal.valueOf(item.getQty() == null ? 1D : item.getQty()));
            quoteItem.setUnit(item.getUnit().name());
            quoteRequestItemRepository.save(quoteItem);
        }
        if (request.getPacks() != null) {
            for (String pack : request.getPacks()) {
                QuoteRequestPack p = new QuoteRequestPack();
                p.setQuoteRequest(saved);
                p.setPackCode(pack);
                p.setPackLabelSnapshot(pack);
                quoteRequestPackRepository.save(p);
            }
        }
        return response(saved.getPublicId(), null, saved.getStatus().name(), saved.getCreatedAt());
    }

    @Transactional
    public CommerceCreateResponse createPreorder(PreorderCreateRequest request) {
        if (!Boolean.TRUE.equals(request.getAcceptsDelay())) {
            throw new IllegalArgumentException("acceptsDelay must be true for preorder");
        }
        DeliveryDto delivery = resolveDelivery(request.getDelivery(), request.getCityZone());
        PreorderRequest preorder = new PreorderRequest();
        preorder.setPublicId(UUID.randomUUID().toString());
        preorder.setFullName(request.getContact().getFullName());
        preorder.setPhone(request.getContact().getPhone());
        preorder.setEmail(request.getContact().getEmail() == null ? "unknown@sopikeur.sn" : request.getContact().getEmail());
        preorder.setCityZone(toLegacyCityZone(delivery, request.getCityZone()));
        preorder.setDeliveryJson(toDeliveryJson(delivery));
        preorder.setNeedsInstallation(Boolean.TRUE.equals(request.getInstallRequested()));
        preorder.setAcceptsDelay(true);
        preorder.setMessage(request.getMessage());
        preorder.setStatus(PreorderStatus.NEW);
        PreorderRequest saved = preorderRequestRepository.save(preorder);

        for (CommerceItemCreateRequest item : request.getItems()) {
            Product product = resolveProduct(item);
            validateUnit(item.getUnit(), product);
            PreorderRequestItem preorderItem = new PreorderRequestItem();
            preorderItem.setPreorderRequest(saved);
            preorderItem.setProduct(product);
            preorderItem.setSkuSnapshot(product.getSku());
            preorderItem.setQty(BigDecimal.valueOf(item.getQty() == null ? 1D : item.getQty()));
            preorderItem.setUnit(item.getUnit().name());
            preorderRequestItemRepository.save(preorderItem);
        }

        return response(saved.getPublicId(), null, saved.getStatus().name(), saved.getCreatedAt());
    }

    @Transactional
    public CommerceCreateResponse createOrder(OrderCreateRequest request) {
        DeliveryDto delivery = resolveDelivery(request.getDelivery(), request.getCityZone());
        for (CommerceItemCreateRequest item : request.getItems()) {
            if (item.getQty() == null || item.getQty() <= 0) {
                throw new IllegalArgumentException("qty must be > 0 for order items");
            }
            Product product = resolveProduct(item);
            validateUnit(item.getUnit(), product);
            StockItem stockItem = stockItemRepository.findByProductId(product.getId())
                .orElseThrow(() -> new StockConflictException("Stock unavailable for sku=" + product.getSku()));
            int requested = (int) Math.floor(item.getQty());
            int available = stockItem.getQuantity() - stockItem.getReserved();
            if (requested > available) {
                throw new StockConflictException("Insufficient stock for sku=" + product.getSku() + ", available=" + available);
            }
        }

        OrderPaymentPlan plan = request.getPaymentPlan() != null
            ? request.getPaymentPlan() : OrderPaymentPlan.CASH_ON_DELIVERY;
        String methodSelected = request.getPaymentMethodSelected() != null
            ? request.getPaymentMethodSelected() : "STRIPE";

        // Plans Stripe : commande en attente de paiement, invisible en backoffice.
        // Le webhook checkout.session.completed la passera en SUBMITTED.
        boolean isDraft = (plan == OrderPaymentPlan.DEPOSIT_50 || plan == OrderPaymentPlan.FULL_ONLINE);
        OrderStatus initialStatus = isDraft ? OrderStatus.DRAFT_PENDING_PAYMENT : OrderStatus.SUBMITTED;

        OrderEntity order = new OrderEntity();
        order.setPublicId(UUID.randomUUID().toString());
        order.setOrderNumber(generateOrderNumber());
        order.setLegacyStatus(initialStatus.name());
        order.setOrderStatus(initialStatus);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setPaymentPlan(plan);
        order.setPaymentMethodSelected(methodSelected);
        order.setAmountPaid(BigDecimal.ZERO);
        order.setFullName(request.getCustomer().getFullName());
        order.setPhone(request.getCustomer().getPhone());
        order.setEmail(request.getCustomer().getEmail());
        order.setCityZone(toLegacyCityZone(delivery, request.getCityZone()));
        order.setNeedsInstallation(Boolean.TRUE.equals(request.getInstallRequested()));
        order.setNote(delivery.getNotes());
        order.setDeliveryJson(toDeliveryJson(delivery));
        OrderEntity saved = orderRepository.save(order);

        BigDecimal amountTotal = BigDecimal.ZERO;
        for (CommerceItemCreateRequest item : request.getItems()) {
            Product product = resolveProduct(item);
            StockItem stockItem = stockItemRepository.findByProductId(product.getId())
                .orElseThrow(() -> new StockConflictException("Stock unavailable for sku=" + product.getSku()));
            int qty = (int) Math.floor(item.getQty());
            stockItem.setQuantity(stockItem.getQuantity() - qty);
            stockItemRepository.save(stockItem);

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(qty));
            amountTotal = amountTotal.add(lineTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(saved);
            orderItem.setProduct(product);
            orderItem.setSkuSnapshot(product.getSku());
            orderItem.setUnit(item.getUnit().name());
            orderItem.setQty(qty);
            orderItem.setUnitPriceSnapshot(product.getPrice());
            orderItem.setLineTotalSnapshot(lineTotal);
            orderItemRepository.save(orderItem);
        }

        // Compute deposit amount for DEPOSIT_50 plan
        BigDecimal depositAmount = null;
        if (plan == OrderPaymentPlan.DEPOSIT_50) {
            depositAmount = amountTotal.multiply(BigDecimal.valueOf(0.5))
                .setScale(0, java.math.RoundingMode.HALF_UP);
        }

        saved.setAmountTotal(amountTotal);
        saved.setAmountDue(amountTotal);
        saved.setDepositAmount(depositAmount);
        orderRepository.save(saved);

        // Notifier uniquement pour les commandes directes (CASH_ON_DELIVERY).
        // Pour les drafts Stripe, la notification est envoyée par le webhook au succès du paiement.
        if (!isDraft) {
            notificationService.notifyOrderCreated(saved);
        }
        return CommerceCreateResponse.builder()
            .id(saved.getPublicId())
            .orderNumber(saved.getOrderNumber())
            .status(saved.getOrderStatus().name())
            .paymentPlan(saved.getPaymentPlan().name())
            .paymentStatus(saved.getPaymentStatus().name())
            .createdAt(saved.getCreatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            .build();
    }

    private String generateOrderNumber() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return "SK-" + System.currentTimeMillis() + "-" + suffix;
    }

    private String toDeliveryJson(DeliveryDto delivery) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("city", delivery.getCity().trim());
        if (hasText(delivery.getArea())) {
            payload.put("area", delivery.getArea().trim());
        }
        if (hasText(delivery.getAddress())) {
            payload.put("address", delivery.getAddress().trim());
        }
        if (hasText(delivery.getNotes())) {
            payload.put("notes", delivery.getNotes().trim());
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid delivery payload");
        }
    }

    private DeliveryDto resolveDelivery(DeliveryDto delivery, String legacyCityZone) {
        if (delivery != null && hasText(delivery.getCity())) {
            return delivery;
        }
        if (hasText(legacyCityZone)) {
            DeliveryDto fallback = new DeliveryDto();
            fallback.setCity(legacyCityZone.trim());
            return fallback;
        }
        throw new IllegalArgumentException("delivery.city is required");
    }

    private String toLegacyCityZone(DeliveryDto delivery, String legacyCityZone) {
        if (delivery != null && hasText(delivery.getCity())) {
            if (hasText(delivery.getArea())) {
                return delivery.getCity().trim() + " - " + delivery.getArea().trim();
            }
            return delivery.getCity().trim();
        }
        return hasText(legacyCityZone) ? legacyCityZone.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Product resolveProduct(CommerceItemCreateRequest item) {
        if (item.getSku() != null && !item.getSku().isBlank()) {
            return productRepository.findBySku(item.getSku())
                .orElseThrow(() -> new NotFoundException("Produit introuvable pour sku=" + item.getSku()));
        }
        try {
            Long id = Long.parseLong(item.getProductId());
            return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Produit introuvable pour id=" + item.getProductId()));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("productId must be a numeric id when sku is missing");
        }
    }

    private void validateUnit(ProductUnit unit, Product product) {
        ProductUnit expected = product.getType() == ProductType.SPC ? ProductUnit.M2 : ProductUnit.PIECE;
        if (unit != expected) {
            throw new IllegalArgumentException("unit mismatch for sku=" + product.getSku() + ", expected=" + expected);
        }
    }

    private CommerceCreateResponse response(String id, String orderNumber, String status, OffsetDateTime createdAt) {
        return CommerceCreateResponse.builder()
            .id(id)
            .orderNumber(orderNumber)
            .status(status)
            .createdAt(createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            .build();
    }
}
