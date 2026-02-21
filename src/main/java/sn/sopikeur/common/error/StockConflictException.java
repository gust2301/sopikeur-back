package sn.sopikeur.common.error;

public class StockConflictException extends RuntimeException {
    public StockConflictException(String message) {
        super(message);
    }
}
