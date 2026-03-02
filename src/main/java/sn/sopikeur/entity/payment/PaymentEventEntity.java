package sn.sopikeur.entity.payment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_events")
public class PaymentEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Provider à l'origine de cet événement : STRIPE | WAVE | ORANGE_MONEY */
    @Column(name = "provider", nullable = false, length = 32)
    private String provider = "STRIPE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_intent_id", nullable = false)
    private PaymentIntentEntity paymentIntent;

    /** Nullable : uniquement renseigné pour Stripe */
    @Column(name = "stripe_event_id", unique = true)
    private String stripeEventId;

    /**
     * Identifiant unique de l'événement côté provider (pour idempotency).
     * Stripe: event.getId() — Wave: webhook event id — OM: txnid
     */
    @Column(name = "provider_event_id", length = 500)
    private String providerEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", columnDefinition = "MEDIUMTEXT")
    private String payload;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;
}
