package sn.sopikeur.dto.response.publicapi;

import java.math.BigDecimal;
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
    private String unit;
    private String dimensions;
    private ProductStatus status;
    private ProductType type;
    private boolean featured;
    private List<MediaAssetResponse> images;
    private StockStatus stockStatus;
}
