package sn.sopikeur.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sn.sopikeur.dto.response.publicapi.MediaAssetResponse;
import sn.sopikeur.entity.media.MediaAsset;

@Mapper(componentModel = "spring")
public interface MediaAssetMapper {

    @Mapping(target = "path", expression = "java(resolvePath(mediaAsset))")
    MediaAssetResponse toResponse(MediaAsset mediaAsset);

    default String resolvePath(MediaAsset mediaAsset) {
        if (mediaAsset == null) {
            return null;
        }
        if (mediaAsset.getPath() != null && !mediaAsset.getPath().isBlank()) {
            return mediaAsset.getPath();
        }
        if (mediaAsset.getUrl() == null || mediaAsset.getUrl().isBlank()) {
            return null;
        }
        String normalized = mediaAsset.getUrl()
            .replaceFirst("^https?://assets\\.sopikeur\\.sn/", "")
            .replaceFirst("^/?assets/", "")
            .replaceFirst("^/+", "");
        return normalized;
    }
}
