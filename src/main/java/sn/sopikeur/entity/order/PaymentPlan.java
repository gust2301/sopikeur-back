package sn.sopikeur.entity.order;

public enum PaymentPlan {
    CASH_ON_DELIVERY,
    DEPOSIT_50,
    FULL_ONLINE;

    public static PaymentPlan fromValue(String value) {
        if (value == null || value.isBlank()) return null;
        return PaymentPlan.valueOf(value.trim().toUpperCase());
    }
}
