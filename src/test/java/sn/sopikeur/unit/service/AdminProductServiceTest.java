package sn.sopikeur.unit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.sopikeur.dto.request.admin.ProductUpsertRequestDto;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.catalog.ProductStatus;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.mapper.AdminProductMapper;
import sn.sopikeur.repo.MediaAssetRepository;
import sn.sopikeur.repo.ProductRepository;
import sn.sopikeur.repo.StockItemRepository;
import sn.sopikeur.service.AdminProductService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MediaAssetRepository mediaAssetRepository;

    @Mock
    private StockItemRepository stockItemRepository;

    @Mock
    private AdminProductMapper adminProductMapper;

    @InjectMocks
    private AdminProductService adminProductService;

    @Test
    void create_shouldMapFields() {
        Product saved = new Product();
        saved.setId(10L);
        saved.setSku("SKU10");
        saved.setSlug("sku10");
        saved.setName("Name10");
        saved.setType(ProductType.SPC);
        saved.setStatus(ProductStatus.ACTIVE);
        saved.setPrice(BigDecimal.TEN);

        when(productRepository.save(any(Product.class))).thenReturn(saved);
        when(adminProductMapper.toDto(argThat(product -> product != null && Long.valueOf(10L).equals(product.getId()))))
            .thenReturn(sn.sopikeur.dto.response.admin.ProductResponseDto.builder()
                .id(10L)
                .sku("SKU10")
                .build());

        ProductUpsertRequestDto dto = new ProductUpsertRequestDto();
        dto.setSku("SKU10"); dto.setSlug("sku10"); dto.setName("Name10");
        dto.setType(ProductType.SPC); dto.setStatus(ProductStatus.ACTIVE);
        dto.setFeatured(true); dto.setPrice(BigDecimal.TEN);

        var response = adminProductService.create(dto);
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getSku()).isEqualTo("SKU10");
    }
}
