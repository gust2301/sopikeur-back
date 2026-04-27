package sn.sopikeur.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sn.sopikeur.dto.response.publicapi.MediaAssetResponse;
import sn.sopikeur.dto.response.publicapi.ProductDetailResponse;
import sn.sopikeur.dto.response.publicapi.ProductSummaryResponse;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.media.MediaAsset;
import sn.sopikeur.entity.stock.StockStatus;

@Mapper(componentModel = "spring", uses = MediaAssetMapper.class)
public interface ProductMapper {

    @Mapping(target = "coverUrl", expression = "java(resolveCoverUrl(product))")
    @Mapping(target = "mainImage", expression = "java(resolveCoverUrl(product))")
    @Mapping(target = "stockStatus", source = "stockStatus")
    @Mapping(target = "promotionActive", source = "promotionActive")
    @Mapping(target = "effectivePrice", source = "effectivePrice")
    @Mapping(target = "discountPercent", source = "discountPercent")
    ProductSummaryResponse toSummary(
        Product product,
        StockStatus stockStatus,
        boolean promotionActive,
        java.math.BigDecimal effectivePrice,
        Integer discountPercent
    );

    @Mapping(target = "images", expression = "java(sortMedia(product.getMediaAssets()))")
    @Mapping(target = "mainImage", expression = "java(resolveCoverUrl(product))")
    @Mapping(target = "galleryImages", expression = "java(resolveGalleryUrls(product))")
    @Mapping(target = "stockStatus", source = "stockStatus")
    @Mapping(target = "promotionActive", source = "promotionActive")
    @Mapping(target = "effectivePrice", source = "effectivePrice")
    @Mapping(target = "discountPercent", source = "discountPercent")
    ProductDetailResponse toDetail(
        Product product,
        StockStatus stockStatus,
        boolean promotionActive,
        java.math.BigDecimal effectivePrice,
        Integer discountPercent
    );

    default String resolveCoverUrl(Product product) {
        return product.getMediaAssets().stream()
            .filter(MediaAsset::isCover)
            .sorted(Comparator.comparing(MediaAsset::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
            .map(this::resolveMediaPath)
            .findFirst()
            .orElse(null);
    }

    default List<MediaAssetResponse> sortMedia(List<MediaAsset> mediaAssets) {
        if (mediaAssets == null) {
            return List.of();
        }
        return mediaAssets.stream()
            .sorted(Comparator.comparing(MediaAsset::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
            .map(this::toMediaAsset)
            .collect(Collectors.toList());
    }

    default List<String> resolveGalleryUrls(Product product) {
        if (product.getMediaAssets() == null) {
            return List.of();
        }
        return product.getMediaAssets().stream()
            .filter(mediaAsset -> !mediaAsset.isCover())
            .sorted(Comparator.comparing(MediaAsset::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
            .map(this::resolveMediaPath)
            .collect(Collectors.toList());
    }

    MediaAssetResponse toMediaAsset(MediaAsset mediaAsset);

    default String resolveMediaPath(MediaAsset mediaAsset) {
        if (mediaAsset == null) {
            return null;
        }
        if (mediaAsset.getPath() != null && !mediaAsset.getPath().isBlank()) {
            return mediaAsset.getPath();
        }
        if (mediaAsset.getUrl() == null || mediaAsset.getUrl().isBlank()) {
            return null;
        }
        return mediaAsset.getUrl()
            .replaceFirst("^https?://assets\\.sopikeur\\.sn/", "")
            .replaceFirst("^/?assets/", "")
            .replaceFirst("^/+", "");
    }
}
