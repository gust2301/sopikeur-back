package sn.sopikeur.entity.leads;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.sopikeur.common.audit.Auditable;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "preorder_requests")
public class PreorderRequest extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id")
    private String publicId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "product_slug")
    private String productSlug;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "city_zone")
    private String cityZone;

    @Column(name = "needs_installation", nullable = false)
    private boolean needsInstallation;

    @Column(name = "delivery_json", columnDefinition = "json")
    private String deliveryJson;

    @Column(name = "accepts_delay")
    private boolean acceptsDelay;

    @Column(name = "unit")
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PreorderStatus status;
}
