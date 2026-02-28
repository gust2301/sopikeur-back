package sn.sopikeur.entity.order;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.sopikeur.common.audit.Auditable;
import sn.sopikeur.entity.payment.PaymentStatus;

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

    /**
     * Legacy status column — kept as plain String to avoid enum mismatch
     * with old values (PENDING_CONFIRMATION, FULFILLED, etc.) that may still
     * exist in the database. New code uses orderStatus instead.
     */
    @Column(name = "status", nullable = false)
    private String legacyStatus;

    /** New lifecycle status replacing the old status enum. */
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_plan", nullable = false)
    private OrderPaymentPlan paymentPlan;

    @Column(name = "payment_method_sel")
    private String paymentMethodSelected;

    @Column(name = "amount_total", precision = 12, scale = 2)
    private BigDecimal amountTotal;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "amount_due", precision = 12, scale = 2)
    private BigDecimal amountDue;

    @Column(name = "deposit_amount", precision = 12, scale = 2)
    private BigDecimal depositAmount;

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

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();
}
