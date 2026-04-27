package sn.sopikeur.unit.service;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import sn.sopikeur.config.TimeConfig;
import sn.sopikeur.dto.request.publicapi.ProductSearchRequest;
import sn.sopikeur.dto.request.publicapi.StockFilter;
import sn.sopikeur.dto.response.publicapi.ProductSearchResponse;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.catalog.ProductStatus;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.mapper.ProductMapper;
import sn.sopikeur.repo.ProductRepository;
import sn.sopikeur.repo.StockItemRepository;
import sn.sopikeur.service.ProductPricingService;
import sn.sopikeur.service.ProductService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockItemRepository stockItemRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        ProductMapper mapper = Mappers.getMapper(ProductMapper.class);
        productService = new ProductService(
            productRepository,
            stockItemRepository,
            mapper,
            new ProductPricingService(new TimeConfig().clock())
        );
    }

    @Test
    void listProducts_convertsFrontPaginationAndUsesFilters() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Hex Panel");
        product.setSku("PANEL-HEX");
        product.setType(ProductType.PANEL);
        product.setStatus(ProductStatus.ACTIVE);

        when(productRepository.search(eq(ProductType.PANEL), eq("hex"), eq("IN_STOCK"), eq(null), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(product), Pageable.ofSize(12), 1));
        when(stockItemRepository.findByProductId(1L)).thenReturn(java.util.Optional.empty());

        ProductSearchResponse response = productService.listProducts(ProductSearchRequest.builder()
            .type(ProductType.PANEL)
            .q(" hex ")
            .stock(StockFilter.IN_STOCK)
            .page(1)
            .size(12)
            .build());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).search(eq(ProductType.PANEL), eq("hex"), eq("IN_STOCK"), eq(null), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(12);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(12);
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getProducts()).hasSize(1);
    }
}
