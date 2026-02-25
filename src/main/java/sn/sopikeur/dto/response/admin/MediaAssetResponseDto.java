package sn.sopikeur.dto.response.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MediaAssetResponseDto {
    private Long id;
    private String url;
    private String path;
    private String alt;
    private boolean cover;
    private Integer sortOrder;
}
