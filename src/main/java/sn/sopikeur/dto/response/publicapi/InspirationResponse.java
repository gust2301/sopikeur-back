package sn.sopikeur.dto.response.publicapi;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InspirationResponse {
    private Long id;
    private String slug;
    private String title;
    private List<String> tags;
    private String coverUrl;
    private List<String> galleryUrls;
    private List<ProductSummaryResponse> products;
}
