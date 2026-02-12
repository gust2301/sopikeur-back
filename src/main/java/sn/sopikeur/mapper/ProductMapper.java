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
    ProductSummaryResponse toSummary(Product product, StockStatus stockStatus);

    @Mapping(target = "images", expression = "java(sortMedia(product.getMediaAssets()))")
    @Mapping(target = "mainImage", expression = "java(resolveCoverUrl(product))")
    @Mapping(target = "galleryImages", expression = "java(resolveGalleryUrls(product))")
    @Mapping(target = "stockStatus", source = "stockStatus")
    ProductDetailResponse toDetail(Product product, StockStatus stockStatus);

    default String resolveCoverUrl(Product product) {
        return product.getMediaAssets().stream()
            .filter(MediaAsset::isCover)
            .sorted(Comparator.comparing(MediaAsset::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
            .map(MediaAsset::getUrl)
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
            .map(MediaAsset::getUrl)
            .collect(Collectors.toList());
    }

    MediaAssetResponse toMediaAsset(MediaAsset mediaAsset);
}
