package sn.sopikeur.controller.publicapi;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.dto.response.publicapi.ProductDetailResponse;
import sn.sopikeur.dto.response.publicapi.ProductSummaryResponse;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.service.ProductService;

@RestController
@RequestMapping(ApiConstants.PUBLIC_API_BASE + "/products")
@RequiredArgsConstructor
public class ProductPublicController {

    private final ProductService productService;

    @GetMapping
    public List<ProductSummaryResponse> listProducts(
        @RequestParam(required = false) ProductType type,
        @RequestParam(required = false) Boolean featured
    ) {
        return productService.listProducts(type, featured);
    }

    @GetMapping("/{slug}")
    public ProductDetailResponse getProduct(@PathVariable String slug) {
        return productService.getProduct(slug);
    }
}
