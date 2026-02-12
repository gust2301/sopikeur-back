package sn.sopikeur.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Data
public class InspirationRequest {
    @NotBlank(message = "Slug obligatoire")
    private String slug;

    @NotBlank(message = "Titre obligatoire")
    private String title;

    private List<String> tags;

    private String coverUrl;

    private List<String> galleryUrls;
}
