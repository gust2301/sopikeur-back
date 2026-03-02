package sn.sopikeur.entity.order;

public enum OrderStatus {
    /** Commande en attente de paiement Stripe — invisible en backoffice jusqu'au succès. */
    DRAFT_PENDING_PAYMENT,
    SUBMITTED,
    CONFIRMED,
    IN_PROGRESS,
    DELIVERED,
    CANCELED
}
