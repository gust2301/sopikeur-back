package sn.sopikeur.common.error;

import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage).toList();
        return ResponseEntity.badRequest().body(ApiError.builder().code("VALIDATION_ERROR").message("Validation failed").details(details).build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiError.builder().code("BAD_REQUEST").message(ex.getMessage()).build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = extractConstraintMessage(ex);
        return ResponseEntity.badRequest().body(ApiError.builder().code("BAD_REQUEST").message(message).build());
    }

    @ExceptionHandler(StockConflictException.class)
    public ResponseEntity<ApiError> handleStockConflict(StockConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiError.builder().code("CONFLICT").message(ex.getMessage()).build());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiError> handleTooManyRequests(TooManyRequestsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ApiError.builder().code("TOO_MANY_REQUESTS").message(ex.getMessage()).build());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.builder().code("NOT_FOUND").message(ex.getMessage()).build());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiError.builder().code("UNAUTHORIZED").message("Unauthorized").build());
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleForbidden(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.builder().code("FORBIDDEN").message("Forbidden").build());
    }

    private String extractConstraintMessage(DataIntegrityViolationException ex) {
        String raw = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        if (raw == null) {
            return "Impossible d'enregistrer votre demande pour le moment. Merci de reessayer.";
        }
        if (raw.contains("products.sku")) {
            return "Un produit avec ce SKU existe deja.";
        }
        if (raw.contains("products.slug")) {
            return "Un produit avec ce slug existe deja.";
        }
        if (raw.contains("articles.slug")) {
            return "Un article avec ce slug existe deja.";
        }
        return "Impossible d'enregistrer votre demande pour le moment. Merci de reessayer.";
    }
}
