package sn.sopikeur.mapper;

import java.util.Comparator;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sn.sopikeur.dto.response.admin.MediaAssetResponseDto;
import sn.sopikeur.dto.response.admin.ProductResponseDto;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.media.MediaAsset;

@Mapper(componentModel = "spring")
public interface AdminProductMapper {

    @Mapping(target = "coverUrl", expression = "java(resolveCoverUrl(product))")
    @Mapping(target = "images",   expression = "java(resolveImageUrls(product))")
    @Mapping(target = "assets",   expression = "java(resolveAssets(product))")
    @Mapping(target = "promotionActive", source = "promotionActive")
    @Mapping(target = "effectivePrice", source = "effectivePrice")
    @Mapping(target = "discountPercent", source = "discountPercent")
    ProductResponseDto toDto(
        Product product,
        boolean promotionActive,
        java.math.BigDecimal effectivePrice,
        Integer discountPercent
    );

    default String resolveCoverUrl(Product product) {
        if (product.getMediaAssets() == null) return null;
        return product.getMediaAssets().stream()
            .filter(MediaAsset::isCover)
            .findFirst()
            .or(() -> product.getMediaAssets().stream().findFirst())
            .map(MediaAsset::getUrl)
            .orElse(null);
    }

    default List<String> resolveImageUrls(Product product) {
        if (product.getMediaAssets() == null) return List.of();
        return product.getMediaAssets().stream()
            .sorted(Comparator.comparingInt(
                a -> (a.getSortOrder() != null ? a.getSortOrder() : Integer.MAX_VALUE)))
            .map(MediaAsset::getUrl)
            .toList();
    }

    default List<MediaAssetResponseDto> resolveAssets(Product product) {
        if (product.getMediaAssets() == null) return List.of();
        return product.getMediaAssets().stream()
            .sorted(Comparator.comparingInt(
                a -> (a.getSortOrder() != null ? a.getSortOrder() : Integer.MAX_VALUE)))
            .map(a -> MediaAssetResponseDto.builder()
                .id(a.getId())
                .url(a.getUrl())
                .path(a.getPath() != null && !a.getPath().isBlank() ? a.getPath() : a.getUrl())
                .alt(a.getAlt())
                .cover(a.isCover())
                .sortOrder(a.getSortOrder())
                .build())
            .toList();
    }
}
