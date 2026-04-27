package sn.sopikeur.dto.response.publicapi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import sn.sopikeur.entity.catalog.ProductStatus;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.entity.stock.StockStatus;

@Data
@Builder
public class ProductDetailResponse {
    private Long id;
    private String sku;
    private String slug;
    private String name;
    private String descriptionShort;
    private String descriptionLong;
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
    private String mainImage;
    private List<String> galleryImages;
    private List<MediaAssetResponse> images;
    private StockStatus stockStatus;
}
