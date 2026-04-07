package sn.sopikeur.dto.response.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record OrderExpenseAdminResponseDto(
    Long id,
    BigDecimal amount,
    String category,
    String kind,
    LocalDate date,
    String note,
    String createdBy,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
