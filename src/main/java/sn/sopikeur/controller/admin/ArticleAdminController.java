package sn.sopikeur.controller.admin;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.dto.request.admin.ArticleRequest;
import sn.sopikeur.dto.response.admin.ArticleAdminResponse;
import sn.sopikeur.service.ArticleService;

@RestController
@RequestMapping(ApiConstants.V1_ADMIN + "/articles")
@RequiredArgsConstructor
public class ArticleAdminController {

    private final ArticleService articleService;

    @GetMapping
    public List<ArticleAdminResponse> listAll() {
        return articleService.listAll();
    }

    @GetMapping("/{id}")
    public ArticleAdminResponse getById(@PathVariable Long id) {
        return articleService.getById(id);
    }

    @PostMapping
    public ResponseEntity<ArticleAdminResponse> create(@Valid @RequestBody ArticleRequest request) {
        return ResponseEntity.ok(articleService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArticleAdminResponse> update(@PathVariable Long id, @Valid @RequestBody ArticleRequest request) {
        return ResponseEntity.ok(articleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
