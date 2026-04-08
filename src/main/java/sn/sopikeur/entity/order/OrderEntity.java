package sn.sopikeur.entity.order;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.sopikeur.common.audit.Auditable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class OrderEntity extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id")
    private String publicId;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "invoice_issued_at")
    private LocalDateTime invoiceIssuedAt;

    @Convert(converter = OrderStatusConverter.class)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "city_zone")
    private String cityZone;

    @Column(name = "needs_installation", nullable = false)
    private boolean needsInstallation;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "delivery_json", columnDefinition = "json")
    private String deliveryJson;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "delivery_eta_date")
    private LocalDate deliveryEtaDate;

    @Column(name = "delivery_note")
    private String deliveryNote;

    @Column(name = "installation_requested")
    private Boolean installationRequested;

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Column(name = "installation_eta_date")
    private LocalDate installationEtaDate;

    @Column(name = "installation_note")
    private String installationNote;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "installed_at")
    private LocalDateTime installedAt;

    @Column(name = "internal_note", columnDefinition = "TEXT")
    private String internalNote;

    @Convert(converter = PaymentStatusConverter.class)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @Convert(converter = PaymentPlanConverter.class)
    @Column(name = "payment_plan")
    private PaymentPlan paymentPlan;

    @Column(name = "payment_method_sel")
    private String paymentMethodSelected;

    @Column(name = "amount_total")
    private java.math.BigDecimal amountTotal;

    @Column(name = "amount_paid")
    private java.math.BigDecimal amountPaid;

    @Column(name = "amount_due")
    private java.math.BigDecimal amountDue;

    @Column(name = "deposit_amount")
    private java.math.BigDecimal depositAmount;

    @Column(name = "installation_amount")
    private java.math.BigDecimal installationAmount;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();
}
