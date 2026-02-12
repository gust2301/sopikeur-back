package sn.sopikeur.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sn.sopikeur.dto.response.publicapi.InspirationResponse;
import sn.sopikeur.dto.response.publicapi.ProductSummaryResponse;
import sn.sopikeur.entity.inspirations.Inspiration;

@Mapper(componentModel = "spring")
public interface InspirationMapper {

    @Mapping(target = "tags", expression = "java(sn.sopikeur.common.utils.StringUtils.splitCsv(inspiration.getTags()))")
    @Mapping(target = "galleryUrls", expression = "java(sn.sopikeur.common.utils.StringUtils.splitCsv(inspiration.getGalleryUrls()))")
    @Mapping(target = "products", expression = "java(mapProducts(products))")
    InspirationResponse toResponse(Inspiration inspiration, List<ProductSummaryResponse> products);

    default List<ProductSummaryResponse> mapProducts(List<ProductSummaryResponse> products) {
        if (products == null) {
            return List.of();
        }
        return products;
    }
}
