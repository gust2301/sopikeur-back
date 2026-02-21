package sn.sopikeur.entity.leads.item;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.sopikeur.common.audit.Auditable;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.leads.QuoteRequest;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "quote_request_items")
public class QuoteRequestItem extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_request_id", nullable = false)
    private QuoteRequest quoteRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_slug_snapshot")
    private String productSlugSnapshot;

    @Column(name = "sku_snapshot")
    private String skuSnapshot;

    @Column(name = "qty", nullable = false)
    private BigDecimal qty;

    @Column(name = "unit", nullable = false)
    private String unit;
}
