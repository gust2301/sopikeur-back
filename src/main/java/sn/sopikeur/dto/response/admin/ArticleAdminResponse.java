package sn.sopikeur.dto.response.admin;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;
import sn.sopikeur.entity.blog.ArticleStatus;

@Data
@Builder
public class ArticleAdminResponse {
    private Long id;
    private String slug;
    private String title;
    private String excerpt;
    private String content;
    private String coverUrl;
    private ArticleStatus status;
    private OffsetDateTime publishedAt;
    private int readingTimeMinutes;
    private String metaTitle;
    private String metaDescription;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
