package sn.sopikeur.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sn.sopikeur.dto.response.admin.StockItemResponseDto;
import sn.sopikeur.entity.stock.StockItem;

@Mapper(componentModel = "spring")
public interface StockMapper {

    @Mapping(target = "productId",   source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "inStock",     expression = "java((item.getQuantity() - item.getReserved()) > 0)")
    StockItemResponseDto toDto(StockItem item);
}
