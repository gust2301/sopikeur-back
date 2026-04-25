package sn.sopikeur.controller.publicapi;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.dto.response.publicapi.ArticleListItemResponse;
import sn.sopikeur.dto.response.publicapi.ArticlePublicResponse;
import sn.sopikeur.service.ArticleService;

@RestController
@RequestMapping(ApiConstants.V1 + "/articles")
@RequiredArgsConstructor
public class ArticlePublicController {

    private final ArticleService articleService;

    @GetMapping
    public List<ArticleListItemResponse> listPublished() {
        return articleService.listPublished();
    }

    @GetMapping("/{slug}")
    public ArticlePublicResponse getBySlug(@PathVariable String slug) {
        return articleService.getPublishedBySlug(slug);
    }
}
