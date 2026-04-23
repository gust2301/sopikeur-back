package sn.sopikeur.dto.response.publicapi.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderTrackingResponseDto {
    private String orderRef;
    private String publicId;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String customerName;
    private String phone;
    private DeliveryDto delivery;
    private InstallationDto installation;
    private boolean installationRequested;
    private LocalDate installationDate;
    private String installationDateText;
    private List<ItemDto> items;
    private TotalsDto totals;
    private PaymentDto payment;
    private List<TimelineStepDto> timeline;

    @Data
    @Builder
    public static class DeliveryDto {
        private String city;
        private String zone;
        private String cityZone;
        private LocalDate expectedDate;
        private LocalDate expectedDeliveryDate;
        private String note;
    }

    @Data
    @Builder
    public static class InstallationDto {
        private boolean requested;
        private LocalDate date;
        private String note;
    }

    @Data
    @Builder
    public static class ItemDto {
        private String sku;
        private String name;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }

    @Data
    @Builder
    public static class TotalsDto {
        private BigDecimal total;
        private BigDecimal paid;
        private BigDecimal due;
    }

    @Data
    @Builder
    public static class PaymentDto {
        private String paymentPlan;
        private String paymentMethod;
    }

    @Data
    @Builder
    public static class TimelineStepDto {
        private String label;
        private String date;
        private boolean done;
    }
}
