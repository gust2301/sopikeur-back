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
import sn.sopikeur.service.ProductPricingService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

    @Mock
    private ProductPricingService productPricingService;

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
        when(productPricingService.isPromotionActive(any(Product.class))).thenReturn(false);
        when(productPricingService.resolveEffectivePrice(any(Product.class))).thenReturn(BigDecimal.TEN);
        when(productPricingService.resolveDiscountPercent(any(Product.class))).thenReturn(null);
        when(adminProductMapper.toDto(
            argThat(product -> product != null && Long.valueOf(10L).equals(product.getId())),
            eq(false),
            eq(BigDecimal.TEN),
            isNull()
        ))
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

    @Test
    void create_shouldRejectPromotionPriceGreaterThanBasePrice() {
        ProductUpsertRequestDto dto = new ProductUpsertRequestDto();
        dto.setSku("SKU11");
        dto.setSlug("sku11");
        dto.setName("Name11");
        dto.setType(ProductType.SPC);
        dto.setStatus(ProductStatus.ACTIVE);
        dto.setFeatured(false);
        dto.setPrice(new BigDecimal("10000"));
        dto.setPromoActive(true);
        dto.setPromoPrice(new BigDecimal("12000"));

        assertThatThrownBy(() -> adminProductService.create(dto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("prix promotionnel");
    }
}
