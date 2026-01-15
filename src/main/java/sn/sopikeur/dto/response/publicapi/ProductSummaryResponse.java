package sn.sopikeur.dto.response.publicapi;

import java.math.BigDecimal;
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
    private String unit;
    private String dimensions;
    private ProductStatus status;
    private ProductType type;
    private boolean featured;
    private String coverUrl;
    private StockStatus stockStatus;
}
