package sn.sopikeur.controller.publicapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.dto.request.publicapi.ProductSearchRequest;
import sn.sopikeur.dto.request.publicapi.StockFilter;
import sn.sopikeur.dto.response.publicapi.ProductDetailResponse;
import sn.sopikeur.dto.response.publicapi.ProductSearchResponse;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.service.ProductService;

@RestController
@RequestMapping(ApiConstants.V1 + "/products")
@RequiredArgsConstructor
public class ProductPublicController {

    private final ProductService productService;

    @Operation(summary = "Lister les produits avec filtres catalogue")
    @GetMapping
    public ProductSearchResponse listProducts(
        @Parameter(description = "Type produit (ex: PANEL, SPC)")
        @RequestParam(required = false) ProductType type,
        @Parameter(description = "Filtre texte simple (name, sku, description courte)")
        @RequestParam(required = false) String q,
        @Parameter(description = "Filtre stock: ALL | IN_STOCK | PREORDER")
        @RequestParam(defaultValue = "ALL") StockFilter stock,
        @Parameter(description = "Page 1-based côté front")
        @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "Nombre d'éléments par page")
        @RequestParam(defaultValue = "12") int size,
        @Parameter(description = "Filtre legacy featured")
        @RequestParam(required = false) Boolean featured
    ) {
        return productService.listProducts(ProductSearchRequest.builder()
            .type(type)
            .q(q)
            .stock(stock)
            .page(page)
            .size(size)
            .featured(featured)
            .build());
    }

    @GetMapping("/{slug}")
    public ProductDetailResponse getProduct(@PathVariable String slug) {
        return productService.getProduct(slug);
    }
}
