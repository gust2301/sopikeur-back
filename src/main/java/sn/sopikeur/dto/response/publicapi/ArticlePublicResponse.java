package sn.sopikeur.dto.response.publicapi;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArticlePublicResponse {
    private Long id;
    private String slug;
    private String title;
    private String excerpt;
    private String content;
    private String coverUrl;
    private OffsetDateTime publishedAt;
    private int readingTimeMinutes;
    private String metaTitle;
    private String metaDescription;
}
