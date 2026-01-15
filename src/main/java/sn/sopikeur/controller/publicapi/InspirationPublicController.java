package sn.sopikeur.controller.publicapi;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.dto.response.publicapi.InspirationResponse;
import sn.sopikeur.service.InspirationService;

@RestController
@RequestMapping(ApiConstants.PUBLIC_API_BASE + "/inspirations")
@RequiredArgsConstructor
public class InspirationPublicController {

    private final InspirationService inspirationService;

    @GetMapping
    public List<InspirationResponse> list(@RequestParam(required = false) String tag) {
        return inspirationService.list(tag);
    }

    @GetMapping("/{slug}")
    public InspirationResponse get(@PathVariable String slug) {
        return inspirationService.getBySlug(slug);
    }
}
