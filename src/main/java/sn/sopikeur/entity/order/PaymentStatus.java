package sn.sopikeur.entity.order;

public enum PaymentStatus {
    UNPAID,
    PARTIALLY_PAID,
    PAID,
    REFUNDED,
    PAYMENT_FAILED;

    public static PaymentStatus fromValue(String value) {
        if (value == null || value.isBlank()) return UNPAID;
        return PaymentStatus.valueOf(value.trim().toUpperCase());
    }
}
