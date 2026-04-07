package sn.sopikeur.dto.response.admin;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record ServiceTypeResponseDto(
    Long id,
    String code,
    String name,
    String unit,
    BigDecimal defaultPrice,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
