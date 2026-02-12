package sn.sopikeur.controller.publicapi;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.common.pagination.PageResponse;
import sn.sopikeur.dto.response.publicapi.InspirationResponse;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.service.InspirationService;

@RestController
@RequestMapping(ApiConstants.V1 + "/inspirations")
@RequiredArgsConstructor
public class InspirationPublicV1Controller {

    private final InspirationService inspirationService;

    @GetMapping
    public PageResponse<InspirationResponse> list(
        @RequestParam(required = false) String tag,
        @RequestParam(required = false) ProductType type,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size
    ) {
        return inspirationService.listPaged(tag, type, page, size);
    }

    @GetMapping("/{slug}")
    public InspirationResponse get(@PathVariable String slug) {
        return inspirationService.getBySlug(slug);
    }
}
