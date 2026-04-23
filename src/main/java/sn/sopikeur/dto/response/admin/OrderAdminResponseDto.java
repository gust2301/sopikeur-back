package sn.sopikeur.dto.response.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderAdminResponseDto {

    private Long id;
    private String publicId;
    private String orderNumber;
    private String invoiceNumber;
    private LocalDateTime invoiceIssuedAt;
    private String reference;
    private String status;

    private CustomerDto customer;
    private DeliveryDto delivery;
    private List<ItemDto> items;
    private List<OrderLineDto> orderLines;
    private AmountsDto amounts;
    private TimestampsDto timestamps;

    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal amountDue;
    private BigDecimal depositAmount;
    private BigDecimal installationAmount;
    private String paymentStatus;
    private String paymentPlan;
    private String paymentMethodSelected;
    private String createdAt;
    private String trackingUrl;
    private List<OrderPaymentAdminResponseDto> payments;

    @Data
    @Builder
    public static class CustomerDto {
        private String fullName;
        private String phone;
        private String email;
    }

    @Data
    @Builder
    public static class DeliveryDto {
        private String cityZone;
        private String city;
        private String zone;
        private String address;
        private boolean needsInstallation;
        private String note;
        private LocalDate expectedDeliveryDate;
        private LocalDate deliveryEtaDate;
        private String deliveryNote;
        private Boolean installationRequested;
        private LocalDate installationDate;
        private LocalDate installationEtaDate;
        private String installationNote;
        private BigDecimal installationAmount;
        private LocalDateTime deliveredAt;
        private LocalDateTime installedAt;
        private String internalNote;
        private String deliveryJson;
    }

    @Data
    @Builder
    public static class ItemDto {
        private Long productId;
        private String productName;
        private String sku;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }

    @Data
    @Builder
    public static class OrderLineDto {
        private Long id;
        private String lineType;
        private Long productId;
        private Long serviceTypeId;
        private String code;
        private String displayName;
        private String productName;
        private String serviceName;
        private String serviceCode;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private String note;
    }

    @Data
    @Builder
    public static class AmountsDto {
        private BigDecimal totalAmount;
        private BigDecimal amountPaid;
        private BigDecimal amountDue;
        private BigDecimal depositAmount;
        private BigDecimal installationAmount;
    }

    @Data
    @Builder
    public static class TimestampsDto {
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }
}
