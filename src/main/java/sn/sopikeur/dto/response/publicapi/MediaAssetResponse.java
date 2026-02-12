package sn.sopikeur.dto.response.publicapi;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MediaAssetResponse {
    private String url;
    private String alt;
    private Long size;
    private Integer sortOrder;
    private boolean cover;
}
