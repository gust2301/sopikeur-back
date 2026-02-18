package sn.sopikeur.entity.leads.item;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.sopikeur.common.audit.Auditable;
import sn.sopikeur.entity.leads.QuoteRequest;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "quote_request_packs")
public class QuoteRequestPack extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_request_id", nullable = false)
    private QuoteRequest quoteRequest;

    @Column(name = "pack_code", nullable = false)
    private String packCode;

    @Column(name = "pack_label_snapshot")
    private String packLabelSnapshot;
}
