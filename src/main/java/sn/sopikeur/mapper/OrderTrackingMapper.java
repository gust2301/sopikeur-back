package sn.sopikeur.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sn.sopikeur.dto.response.publicapi.order.OrderTrackingResponseDto;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.order.OrderItem;
import sn.sopikeur.entity.order.OrderStatus;

@Component
@RequiredArgsConstructor
public class OrderTrackingMapper {

    private final ObjectMapper objectMapper;

    public OrderTrackingResponseDto toDto(OrderEntity order) {
        DeliveryInfo deliveryInfo = parseDelivery(order);
        BigDecimal total = computeTotal(order);
        BigDecimal paid = order.getAmountPaid() != null ? order.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal due = order.getAmountDue() != null ? order.getAmountDue() : total.subtract(paid).max(BigDecimal.ZERO);

        return OrderTrackingResponseDto.builder()
            .orderRef(order.getOrderNumber() != null ? order.getOrderNumber() : order.getPublicId())
            .publicId(order.getPublicId())
            .status(order.getStatus())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .customerName(order.getFullName())
            .phone(maskPhone(order.getPhone()))
            .delivery(OrderTrackingResponseDto.DeliveryDto.builder()
                .city(deliveryInfo.city())
                .zone(deliveryInfo.zone())
                .cityZone(order.getCityZone())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .build())
            .installationRequested(order.isNeedsInstallation())
            .installationDate(order.getInstallationDate())
            .installationDateText(resolveInstallationDateText(order))
            .items(order.getItems().stream().map(this::toItemDto).toList())
            .totals(OrderTrackingResponseDto.TotalsDto.builder()
                .total(total)
                .paid(paid)
                .due(due)
                .build())
            .payment(OrderTrackingResponseDto.PaymentDto.builder()
                .paymentPlan(mapPaymentPlan(order))
                .paymentMethod(mapPaymentMethod(order.getPaymentMethodSelected()))
                .build())
            .timeline(buildTimeline(order))
            .build();
    }

    private OrderTrackingResponseDto.ItemDto toItemDto(OrderItem item) {
        return OrderTrackingResponseDto.ItemDto.builder()
            .sku(item.getSkuSnapshot())
            .name(item.getProduct() != null ? item.getProduct().getName() : item.getSkuSnapshot())
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
        return order.getItems().stream()
            .map(OrderItem::getLineTotalSnapshot)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
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
            case "NONE" -> "Espèces à la livraison";
            case "WAVE" -> "Wave";
            case "ORANGE_MONEY" -> "Orange Money";
            case "STRIPE" -> "Stripe";
            default -> paymentMethodSelected;
        };
    }

    private String resolveInstallationDateText(OrderEntity order) {
        if (!order.isNeedsInstallation()) {
            return null;
        }
        if (order.getInstallationDate() != null) {
            return null;
        }
        if (order.getInstallationNote() != null && !order.getInstallationNote().isBlank()) {
            return order.getInstallationNote();
        }
        return "À confirmer";
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

    private List<OrderTrackingResponseDto.TimelineStepDto> buildTimeline(OrderEntity order) {
        List<OrderTrackingResponseDto.TimelineStepDto> timeline = new ArrayList<>();
        OrderStatus status = order.getStatus() != null ? order.getStatus() : OrderStatus.PENDING_CONFIRMATION;
        boolean confirmed = status == OrderStatus.CONFIRMED || status == OrderStatus.FULFILLED;
        boolean fulfilled = status == OrderStatus.FULFILLED;

        timeline.add(step("Commande reçue", order.getCreatedAt(), true));
        timeline.add(step("Commande confirmée", confirmed ? order.getUpdatedAt() : null, confirmed));
        timeline.add(step("Préparation en cours", confirmed ? order.getUpdatedAt() : null, confirmed));
        timeline.add(step("Livraison en cours", fulfilled ? resolveDeliveryStepDate(order) : order.getExpectedDeliveryDate(), fulfilled));

        String finalLabel = order.isNeedsInstallation() ? "Installation terminée" : "Commande finalisée";
        String finalDate = fulfilled ? resolveFinalStepDate(order) : null;
        timeline.add(OrderTrackingResponseDto.TimelineStepDto.builder()
            .label(finalLabel)
            .date(finalDate)
            .done(fulfilled)
            .build());

        return timeline;
    }

    private OrderTrackingResponseDto.TimelineStepDto step(String label, Object date, boolean done) {
        return OrderTrackingResponseDto.TimelineStepDto.builder()
            .label(label)
            .date(formatDate(date))
            .done(done)
            .build();
    }

    private String resolveDeliveryStepDate(OrderEntity order) {
        if (order.getExpectedDeliveryDate() != null) {
            return order.getExpectedDeliveryDate().toString();
        }
        return formatDate(order.getUpdatedAt());
    }

    private String resolveFinalStepDate(OrderEntity order) {
        if (order.isNeedsInstallation() && order.getInstallationDate() != null) {
            return order.getInstallationDate().toString();
        }
        return formatDate(order.getUpdatedAt());
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
        return String.valueOf(value);
    }

    private record DeliveryInfo(String city, String zone) {
    }
}
