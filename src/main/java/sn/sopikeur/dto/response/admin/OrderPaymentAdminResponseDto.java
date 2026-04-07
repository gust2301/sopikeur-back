package sn.sopikeur.dto.response.admin;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record OrderPaymentAdminResponseDto(
    Long id,
    String receiptNumber,
    BigDecimal amount,
    String method,
    String note,
    BigDecimal paidTotal,
    BigDecimal dueTotal,
    OffsetDateTime createdAt
) {
}
