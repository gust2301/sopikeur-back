package sn.sopikeur.dto.response.admin;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StockItemResponseDto {
    private Long productId;
    private String productName;
    private Integer quantity;
    private Integer reserved;
    private Boolean preorderAllowed;
    private Boolean inStock;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
