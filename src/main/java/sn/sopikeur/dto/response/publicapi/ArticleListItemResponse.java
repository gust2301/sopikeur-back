package sn.sopikeur.dto.response.publicapi;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArticleListItemResponse {
    private Long id;
    private String slug;
    private String title;
    private String excerpt;
    private String coverUrl;
    private OffsetDateTime publishedAt;
    private int readingTimeMinutes;
}
