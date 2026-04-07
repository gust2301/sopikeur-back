package sn.sopikeur.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sn.sopikeur.dto.response.publicapi.order.OrderTrackingResponseDto;
import sn.sopikeur.dto.response.publicapi.order.PublicOrderTrackingResponseDto;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.order.OrderItem;
import sn.sopikeur.entity.order.OrderLineType;
import sn.sopikeur.entity.order.OrderStatus;

@Component
@RequiredArgsConstructor
public class OrderTrackingMapper {

    private static final DateTimeFormatter DATE_TIME_DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ObjectMapper objectMapper;

    public OrderTrackingResponseDto toDto(OrderEntity order) {
        DeliveryInfo deliveryInfo = parseDelivery(order);
        boolean installationRequested = resolveInstallationRequested(order);
        BigDecimal total = computeTotal(order);
        BigDecimal paid = order.getAmountPaid() != null ? order.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal due = order.getAmountDue() != null ? order.getAmountDue() : total.subtract(paid).max(BigDecimal.ZERO);
        LocalDate deliveryEta = resolveDeliveryEta(order);
        LocalDate installationEta = resolveInstallationEta(order);

        return OrderTrackingResponseDto.builder()
            .orderRef(resolveReference(order))
            .publicId(order.getPublicId())
            .status(resolveDisplayStatus(order))
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .customerName(order.getFullName())
            .phone(maskPhone(order.getPhone()))
            .delivery(OrderTrackingResponseDto.DeliveryDto.builder()
                .city(deliveryInfo.city())
                .zone(deliveryInfo.zone())
                .cityZone(order.getCityZone())
                .expectedDate(deliveryEta)
                .expectedDeliveryDate(deliveryEta)
                .note(order.getDeliveryNote())
                .build())
            .installation(OrderTrackingResponseDto.InstallationDto.builder()
                .requested(installationRequested)
                .date(installationEta)
                .note(resolveInstallationNote(order))
                .build())
            .installationRequested(installationRequested)
            .installationDate(installationEta)
            .installationDateText(resolveInstallationDateText(order))
            .items(order.getItems().stream().map(this::toLegacyItemDto).toList())
            .totals(OrderTrackingResponseDto.TotalsDto.builder()
                .total(total)
                .paid(paid)
                .due(due)
                .build())
            .payment(OrderTrackingResponseDto.PaymentDto.builder()
                .paymentPlan(mapPaymentPlan(order))
                .paymentMethod(mapPaymentMethod(order.getPaymentMethodSelected()))
                .build())
            .timeline(buildLegacyTimeline(order))
            .build();
    }

    public PublicOrderTrackingResponseDto toTrackingDto(OrderEntity order) {
        DeliveryInfo deliveryInfo = parseDelivery(order);
        BigDecimal total = computeTotal(order);
        BigDecimal paid = order.getAmountPaid() != null ? order.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal due = order.getAmountDue() != null ? order.getAmountDue() : total.subtract(paid).max(BigDecimal.ZERO);

        return PublicOrderTrackingResponseDto.builder()
            .reference(resolveReference(order))
            .publicId(order.getPublicId())
            .status(resolveDisplayStatus(order))
            .installationRequested(resolveInstallationRequested(order))
            .deliveryEtaDate(resolveDeliveryEta(order))
            .deliveredAt(order.getDeliveredAt())
            .installationEtaDate(resolveInstallationEta(order))
            .installedAt(order.getInstalledAt())
            .timelineSteps(buildTrackingTimeline(order))
            .summary(PublicOrderTrackingResponseDto.SummaryDto.builder()
                .customerName(order.getFullName())
                .phone(maskPhone(order.getPhone()))
                .city(deliveryInfo.city())
                .zone(deliveryInfo.zone())
                .cityZone(order.getCityZone())
                .build())
            .payment(PublicOrderTrackingResponseDto.TotalsDto.builder()
                .total(total)
                .paid(paid)
                .due(due)
                .installationAmount(resolveInstallationAmount(order))
                .paymentPlan(mapPaymentPlan(order))
                .paymentMethod(mapPaymentMethod(order.getPaymentMethodSelected()))
                .build())
            .items(order.getItems().stream().map(this::toTrackingItemDto).toList())
            .build();
    }

    private OrderTrackingResponseDto.ItemDto toLegacyItemDto(OrderItem item) {
        return OrderTrackingResponseDto.ItemDto.builder()
            .sku(item.getSkuSnapshot())
            .name(resolveItemName(item))
            .unit(item.getUnit())
            .quantity(item.getQty())
            .unitPrice(item.getUnitPriceSnapshot())
            .lineTotal(item.getLineTotalSnapshot())
            .build();
    }

    private PublicOrderTrackingResponseDto.ItemDto toTrackingItemDto(OrderItem item) {
        return PublicOrderTrackingResponseDto.ItemDto.builder()
            .sku(item.getSkuSnapshot())
            .name(resolveItemName(item))
            .unit(item.getUnit())
            .quantity(item.getQty())
            .unitPrice(item.getUnitPriceSnapshot())
            .lineTotal(item.getLineTotalSnapshot())
            .build();
    }

    private DeliveryInfo parseDelivery(OrderEntity order) {
        if (order.getDeliveryJson() == null || order.getDeliveryJson().isBlank()) {
            return fallbackDelivery(order.getCityZone());
        }

        try {
            JsonNode root = objectMapper.readTree(order.getDeliveryJson());
            String city = textOrNull(root.get("city"));
            String area = textOrNull(root.get("area"));
            if (city == null && area == null) {
                return fallbackDelivery(order.getCityZone());
            }
            return new DeliveryInfo(city, area);
        } catch (Exception ignored) {
            return fallbackDelivery(order.getCityZone());
        }
    }

    private DeliveryInfo fallbackDelivery(String cityZone) {
        if (cityZone == null || cityZone.isBlank()) {
            return new DeliveryInfo(null, null);
        }

        String[] parts = cityZone.split("\\s+-\\s+", 2);
        if (parts.length == 2) {
            return new DeliveryInfo(parts[0].trim(), parts[1].trim());
        }
        return new DeliveryInfo(cityZone.trim(), null);
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal computeTotal(OrderEntity order) {
        if (order.getAmountTotal() != null) {
            return order.getAmountTotal();
        }
        BigDecimal itemsTotal = order.getItems().stream()
            .map(OrderItem::getLineTotalSnapshot)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return itemsTotal.add(resolveLegacyInstallationFallback(order));
    }

    private BigDecimal resolveInstallationAmount(OrderEntity order) {
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

    private BigDecimal resolveLegacyInstallationFallback(OrderEntity order) {
        if (order.getItems().stream().anyMatch(this::isInstallationServiceLine) || !resolveInstallationRequested(order)) {
            return BigDecimal.ZERO;
        }
        return order.getInstallationAmount() != null ? order.getInstallationAmount() : BigDecimal.ZERO;
    }

    private boolean isInstallationServiceLine(OrderItem item) {
        return item.getLineType() == OrderLineType.SERVICE
            && item.getServiceType() != null
            && item.getServiceType().getCode() != null
            && "INSTALLATION".equalsIgnoreCase(item.getServiceType().getCode().trim());
    }

    private String resolveItemName(OrderItem item) {
        if (item.getDisplayName() != null && !item.getDisplayName().isBlank()) {
            return item.getDisplayName();
        }
        if (item.getProduct() != null && item.getProduct().getName() != null && !item.getProduct().getName().isBlank()) {
            return item.getProduct().getName();
        }
        if (item.getServiceType() != null && item.getServiceType().getName() != null && !item.getServiceType().getName().isBlank()) {
            return item.getServiceType().getName();
        }
        return item.getSkuSnapshot();
    }

    private String mapPaymentPlan(OrderEntity order) {
        if (order.getPaymentPlan() == null) {
            return null;
        }
        return switch (order.getPaymentPlan()) {
            case CASH_ON_DELIVERY -> "CASH_ON_DELIVERY";
            case DEPOSIT_50 -> "DEPOSIT";
            case FULL_ONLINE -> "FULL";
        };
    }

    private String mapPaymentMethod(String paymentMethodSelected) {
        if (paymentMethodSelected == null || paymentMethodSelected.isBlank()) {
            return null;
        }
        return switch (paymentMethodSelected.trim().toUpperCase()) {
            case "NONE" -> "Especes a la livraison";
            case "WAVE" -> "Wave";
            case "ORANGE_MONEY" -> "Orange Money";
            case "STRIPE" -> "Stripe";
            default -> paymentMethodSelected;
        };
    }

    private boolean resolveInstallationRequested(OrderEntity order) {
        if (order.getInstallationRequested() != null) {
            return order.getInstallationRequested();
        }
        return order.isNeedsInstallation();
    }

    private LocalDate resolveDeliveryEta(OrderEntity order) {
        return order.getDeliveryEtaDate() != null ? order.getDeliveryEtaDate() : order.getExpectedDeliveryDate();
    }

    private LocalDate resolveInstallationEta(OrderEntity order) {
        return order.getInstallationEtaDate() != null ? order.getInstallationEtaDate() : order.getInstallationDate();
    }

    private String resolveInstallationNote(OrderEntity order) {
        if (order.getInstallationNote() == null || order.getInstallationNote().isBlank()) {
            return null;
        }
        return order.getInstallationNote();
    }

    private String resolveInstallationDateText(OrderEntity order) {
        if (!resolveInstallationRequested(order)) {
            return null;
        }
        if (resolveInstallationEta(order) != null) {
            return null;
        }
        String installationNote = resolveInstallationNote(order);
        if (installationNote != null) {
            return installationNote;
        }
        return "A confirmer";
    }

    private String resolveReference(OrderEntity order) {
        return order.getOrderNumber() != null ? order.getOrderNumber() : order.getPublicId();
    }

    private String resolveDisplayStatus(OrderEntity order) {
        if (order.getInstalledAt() != null) {
            return "INSTALLED";
        }
        if (order.getDeliveredAt() != null) {
            return "DELIVERED";
        }
        return order.getStatus() != null ? order.getStatus().name() : OrderStatus.PENDING_CONFIRMATION.name();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String normalized = phone.trim();
        if (normalized.length() <= 4) {
            return normalized;
        }
        return normalized.substring(0, 2) + "*".repeat(Math.max(0, normalized.length() - 4)) + normalized.substring(normalized.length() - 2);
    }

    private List<OrderTrackingResponseDto.TimelineStepDto> buildLegacyTimeline(OrderEntity order) {
        return buildTrackingTimeline(order).stream()
            .map(step -> OrderTrackingResponseDto.TimelineStepDto.builder()
                .label(step.getLabel())
                .date(step.getDateDisplay())
                .done("DONE".equals(step.getState()))
                .build())
            .toList();
    }

    private List<PublicOrderTrackingResponseDto.TimelineStepDto> buildTrackingTimeline(OrderEntity order) {
        List<PublicOrderTrackingResponseDto.TimelineStepDto> timeline = new ArrayList<>();
        boolean installationRequested = resolveInstallationRequested(order);
        boolean confirmed = isConfirmed(order);
        boolean delivered = order.getDeliveredAt() != null;
        boolean installed = order.getInstalledAt() != null;

        timeline.add(step("Commande recue", "DONE", formatDate(order.getCreatedAt())));
        timeline.add(step("Confirmee", confirmed ? "DONE" : "CURRENT", confirmed ? formatDate(order.getUpdatedAt()) : null));
        timeline.add(step("Preparation", confirmed ? "DONE" : "TODO", confirmed ? formatDate(order.getUpdatedAt()) : null));

        String deliveryState = delivered ? "DONE" : (confirmed ? "CURRENT" : "TODO");
        timeline.add(step(
            delivered ? "Livree" : "Livraison planifiee",
            deliveryState,
            delivered ? formatDate(order.getDeliveredAt()) : formatDate(resolveDeliveryEta(order))
        ));

        if (installationRequested) {
            String installationPlanningState = delivered ? "DONE" : (confirmed ? "CURRENT" : "TODO");
            timeline.add(step(
                installed ? "Installation en cours" : "Installation planifiee",
                installationPlanningState,
                installed ? formatDate(order.getInstalledAt()) : formatDate(resolveInstallationEta(order))
            ));
            timeline.add(step(
                "Installation terminee",
                installed ? "DONE" : (delivered ? "CURRENT" : "TODO"),
                installed ? formatDate(order.getInstalledAt()) : formatDate(resolveInstallationEta(order))
            ));
        }

        return timeline;
    }

    private boolean isConfirmed(OrderEntity order) {
        OrderStatus status = order.getStatus() != null ? order.getStatus() : OrderStatus.PENDING_CONFIRMATION;
        return status == OrderStatus.CONFIRMED || status == OrderStatus.FULFILLED || order.getDeliveredAt() != null || order.getInstalledAt() != null;
    }

    private PublicOrderTrackingResponseDto.TimelineStepDto step(String label, String state, String dateDisplay) {
        return PublicOrderTrackingResponseDto.TimelineStepDto.builder()
            .label(label)
            .state(state)
            .dateDisplay(dateDisplay)
            .build();
    }

    private String formatDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime dateTime) {
            return dateTime.toString();
        }
        if (value instanceof LocalDate localDate) {
            return localDate.toString();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.format(DATE_TIME_DISPLAY);
        }
        return String.valueOf(value);
    }

    private record DeliveryInfo(String city, String zone) {
    }
}
