package sn.sopikeur.controller.admin;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
