package sn.sopikeur.mapper;

import org.mapstruct.Mapper;
import sn.sopikeur.dto.response.publicapi.MediaAssetResponse;
import sn.sopikeur.entity.media.MediaAsset;

@Mapper(componentModel = "spring")
public interface MediaAssetMapper {
    MediaAssetResponse toResponse(MediaAsset mediaAsset);
}
