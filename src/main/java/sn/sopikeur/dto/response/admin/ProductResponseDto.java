package sn.sopikeur.dto.response.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import sn.sopikeur.entity.catalog.ProductStatus;
import sn.sopikeur.entity.catalog.ProductType;

@Data
@Builder
public class ProductResponseDto {
    private Long id;
    private String sku;
    private String slug;
    private String name;
    private ProductType type;
    private ProductStatus status;
    private boolean featured;
    private boolean discountService;
    private BigDecimal price;
    private boolean promoActive;
    private BigDecimal promoPrice;
    private LocalDate promoStartDate;
    private LocalDate promoEndDate;
    private String promoLabel;
    private boolean promotionActive;
    private BigDecimal effectivePrice;
    private Integer discountPercent;
    private String unit;
    private String dimensions;
    private String descriptionShort;
    private String descriptionLong;
    private String coverUrl;
    private List<String> images;
    private List<MediaAssetResponseDto> assets;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
