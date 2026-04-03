package sn.sopikeur.controller.admin;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.sopikeur.dto.request.admin.RegisterMediaAssetRequest;
import sn.sopikeur.dto.response.admin.MediaAssetResponseDto;
import sn.sopikeur.entity.media.MediaAsset;
import sn.sopikeur.repo.MediaAssetRepository;

@RestController
@RequestMapping("/api/v1/admin/media")
@RequiredArgsConstructor
public class MediaAdminController {

    private final MediaAssetRepository mediaAssetRepository;

    @GetMapping("/assets")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR','SALES')")
    public List<MediaAssetResponseDto> listAssets() {
        return mediaAssetRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder", "id"))
            .stream()
            .map(this::toDto)
            .toList();
    }

    /**
     * Enregistre un asset R2 existant dans la médiathèque en fournissant son chemin relatif.
     * Ex : { "path": "accessories/mon-produit.jpg", "alt": "Mon produit" }
     */
    @PostMapping("/assets")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR')")
    public MediaAssetResponseDto registerAsset(@Valid @RequestBody RegisterMediaAssetRequest request) {
        String cleanPath = request.getPath().replaceFirst("^/?assets/", "").replaceFirst("^/+", "");
        MediaAsset asset = new MediaAsset();
        asset.setPath(cleanPath);
        asset.setUrl("https://assets.sopikeur.sn/" + cleanPath);
        asset.setAlt(request.getAlt());
        asset.setCover(false);
        return toDto(mediaAssetRepository.save(asset));
    }

    private MediaAssetResponseDto toDto(MediaAsset a) {
        return MediaAssetResponseDto.builder()
            .id(a.getId())
            .url(a.getUrl())
            .path(resolvePath(a))
            .alt(a.getAlt())
            .cover(a.isCover())
            .sortOrder(a.getSortOrder())
            .build();
    }

    private String resolvePath(MediaAsset a) {
        if (a.getPath() != null && !a.getPath().isBlank()) {
            return a.getPath();
        }
        if (a.getUrl() == null || a.getUrl().isBlank()) {
            return null;
        }
        return a.getUrl()
            .replaceFirst("^https?://assets\\.sopikeur\\.sn/", "")
            .replaceFirst("^/?assets/", "")
            .replaceFirst("^/+", "");
    }
}
