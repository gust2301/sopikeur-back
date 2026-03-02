package sn.sopikeur.entity.payment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.sopikeur.common.audit.Auditable;
import sn.sopikeur.entity.order.OrderEntity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_intents")
public class PaymentIntentEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Provider de paiement : STRIPE | WAVE | ORANGE_MONEY */
    @Column(name = "provider", nullable = false, length = 32)
    private String provider = "STRIPE";

    @Column(name = "public_id", nullable = false, unique = true, length = 64)
    private String publicId;

    /** Nullable : uniquement renseigné pour Stripe */
    @Column(name = "stripe_session_id", unique = true)
    private String stripeSessionId;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    /**
     * Identifiant de session côté provider (Wave checkout_id, OM payToken, Stripe session.id).
     * Utilisé pour retrouver le PaymentIntent via webhook.
     */
    @Column(name = "provider_checkout_id")
    private String providerCheckoutId;

    /**
     * Référence complémentaire provider (Wave transaction_id, OM mpesa_ref, Stripe paymentIntentId).
     */
    @Column(name = "provider_payment_ref")
    private String providerPaymentRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "currency", nullable = false, length = 8)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 32)
    private PaymentPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PaymentIntentStatus status;

    @Column(name = "checkout_url", columnDefinition = "TEXT")
    private String checkoutUrl;
}
