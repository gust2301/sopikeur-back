package sn.sopikeur.entity.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.sopikeur.common.audit.Auditable;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_payments")
public class OrderPaymentEntity extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "receipt_number")
    private String receiptNumber;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "method")
    private String method;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_by_admin_user_id")
    private Long createdByAdminUserId;
}
