package sn.sopikeur.entity.order;

public enum OrderStatus {
    PENDING_CONFIRMATION,
    CONFIRMED,
    CANCELLED,
    FULFILLED;

    public static OrderStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return PENDING_CONFIRMATION;
        }

        return switch (value.trim().toUpperCase()) {
            case "SUBMITTED" -> PENDING_CONFIRMATION;
            case "DRAFT_PENDING_PAYMENT" -> PENDING_CONFIRMATION;
            case "CANCELED" -> CANCELLED;
            case "DELIVERED" -> FULFILLED;
            default -> OrderStatus.valueOf(value.trim().toUpperCase());
        };
    }
}
