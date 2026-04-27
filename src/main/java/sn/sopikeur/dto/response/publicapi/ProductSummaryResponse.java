package sn.sopikeur.dto.response.publicapi;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;
import sn.sopikeur.entity.catalog.ProductStatus;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.entity.stock.StockStatus;

@Data
@Builder
public class ProductSummaryResponse {
    private Long id;
    private String sku;
    private String slug;
    private String name;
    private String descriptionShort;
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
    private ProductStatus status;
    private ProductType type;
    private boolean featured;
    private String coverUrl;
    private String mainImage;
    private StockStatus stockStatus;
}
