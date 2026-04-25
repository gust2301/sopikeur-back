package sn.sopikeur.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import sn.sopikeur.entity.blog.ArticleStatus;

@Data
public class ArticleRequest {
    @NotBlank(message = "Slug obligatoire")
    private String slug;

    @NotBlank(message = "Titre obligatoire")
    private String title;

    @NotBlank(message = "Extrait obligatoire")
    @Size(max = 500, message = "L'extrait ne peut pas depasser 500 caracteres")
    private String excerpt;

    @NotBlank(message = "Contenu obligatoire")
    private String content;

    private String coverUrl;

    private ArticleStatus status;

    private Integer readingTimeMinutes;

    private String metaTitle;

    private String metaDescription;
}
