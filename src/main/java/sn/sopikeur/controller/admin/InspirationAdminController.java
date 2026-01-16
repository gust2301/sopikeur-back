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
import sn.sopikeur.dto.request.admin.InspirationRequest;
import sn.sopikeur.dto.response.publicapi.InspirationResponse;
import sn.sopikeur.service.InspirationService;

@RestController
@RequestMapping(ApiConstants.V1_ADMIN + "/inspirations")
@RequiredArgsConstructor
public class InspirationAdminController {

    private final InspirationService inspirationService;

    @GetMapping
    public List<InspirationResponse> list() {
        return inspirationService.list(null, null);
    }

    @PostMapping
    public ResponseEntity<InspirationResponse> create(@Valid @RequestBody InspirationRequest request) {
        return ResponseEntity.ok(inspirationService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InspirationResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody InspirationRequest request
    ) {
        return ResponseEntity.ok(inspirationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        inspirationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/products/{productId}")
    public ResponseEntity<InspirationResponse> linkProduct(
        @PathVariable Long id,
        @PathVariable Long productId
    ) {
        return ResponseEntity.ok(inspirationService.linkProduct(id, productId));
    }

    @DeleteMapping("/{id}/products/{productId}")
    public ResponseEntity<InspirationResponse> unlinkProduct(
        @PathVariable Long id,
        @PathVariable Long productId
    ) {
        return ResponseEntity.ok(inspirationService.unlinkProduct(id, productId));
    }
}
