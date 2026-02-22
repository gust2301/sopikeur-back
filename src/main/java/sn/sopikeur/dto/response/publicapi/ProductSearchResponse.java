package sn.sopikeur.dto.response.publicapi;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductSearchResponse {
    private List<ProductSummaryResponse> products;
    private long total;
    private int page;
    private int size;
}
