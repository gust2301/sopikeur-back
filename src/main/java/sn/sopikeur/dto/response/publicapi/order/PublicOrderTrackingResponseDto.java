package sn.sopikeur.dto.response.publicapi.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicOrderTrackingResponseDto {
    private String reference;
    private String publicId;
    private String status;
    private boolean installationRequested;
    private LocalDate deliveryEtaDate;
    private LocalDateTime deliveredAt;
    private LocalDate installationEtaDate;
    private LocalDateTime installedAt;
    private List<TimelineStepDto> timelineSteps;
    private SummaryDto summary;
    private TotalsDto payment;
    private List<ItemDto> items;

    @Data
    @Builder
    public static class TimelineStepDto {
        private String label;
        private String state;
        private String dateDisplay;
    }

    @Data
    @Builder
    public static class SummaryDto {
        private String customerName;
        private String phone;
        private String city;
        private String zone;
        private String cityZone;
    }

    @Data
    @Builder
    public static class TotalsDto {
        private BigDecimal total;
        private BigDecimal paid;
        private BigDecimal due;
        private BigDecimal installationAmount;
        private String paymentPlan;
        private String paymentMethod;
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
}
