package sn.sopikeur.dto.response.admin;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderAdminResponseDto {

    private Long   id;
    private String publicId;
    private String orderNumber;
    /** Alias = orderNumber sinon publicId — conservé pour la vue liste */
    private String reference;
    private String status;

    // ── Statuts séparés ──────────────────────────────────────────────────────
    private String orderStatus;
    private String paymentStatus;
    private String paymentPlan;
    private String paymentMethodSelected;

    private CustomerDto          customer;
    private DeliveryDto          delivery;
    private List<ItemDto>        items;
    private AmountsDto           amounts;
    private TimestampsDto        timestamps;
    private List<PaymentIntentDto> payments;

    // ── Champs plats conservés pour rétrocompat liste ────────────────────────
    private String     customerName;
    private String     customerEmail;
    private String     customerPhone;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal amountDue;
    private BigDecimal depositAmount;
    private String     createdAt;

    // ── Nested DTOs ──────────────────────────────────────────────────────────

    @Data @Builder
    public static class CustomerDto {
        private String fullName;
        private String phone;
        private String email;
    }

    @Data @Builder
    public static class DeliveryDto {
        private String  cityZone;
        private boolean needsInstallation;
        private String  note;
        /** Contenu brut du champ JSON delivery_json */
        private String  deliveryJson;
    }

    @Data @Builder
    public static class ItemDto {
        private Long       productId;
        private String     productName;
        private String     sku;
        private String     unit;
        private int        quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }

    @Data @Builder
    public static class AmountsDto {
        private BigDecimal totalAmount;
        private BigDecimal amountPaid;
        private BigDecimal amountDue;
        private BigDecimal depositAmount;
    }

    @Data @Builder
    public static class TimestampsDto {
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }

    @Data @Builder
    public static class PaymentIntentDto {
        private String        publicId;
        private Long          amount;
        private String        currency;
        private String        purpose;
        private String        status;
        private String        checkoutUrl;
        private OffsetDateTime createdAt;
    }
}
