package sn.sopikeur.dto.request.publicapi;

import lombok.Builder;
import lombok.Data;
import sn.sopikeur.entity.catalog.ProductType;

@Data
@Builder
public class ProductSearchRequest {
    private ProductType type;
    private String q;
    @Builder.Default
    private StockFilter stock = StockFilter.ALL;
    @Builder.Default
    private int page = 1;
    @Builder.Default
    private int size = 12;
    private Boolean featured;
}
