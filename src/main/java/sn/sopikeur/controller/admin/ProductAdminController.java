package sn.sopikeur.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.sopikeur.common.pagination.PageResponse;
import sn.sopikeur.dto.request.admin.ProductUpsertRequestDto;
import sn.sopikeur.dto.response.admin.ProductResponseDto;
import sn.sopikeur.entity.catalog.ProductStatus;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.service.AdminProductService;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {

    private final AdminProductService service;

    @GetMapping
    @Operation(summary = "Liste paginée des produits admin")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR','SALES')")
    public PageResponse<ProductResponseDto> list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort,
        @RequestParam(required = false) ProductType type,
        @RequestParam(required = false) ProductStatus status,
        @RequestParam(required = false) Boolean featured,
        @RequestParam(required = false) String q
    ) {
        return service.list(page, size, sort, type, status, featured, q);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR','SALES')")
    public ProductResponseDto get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR')")
    public ProductResponseDto create(@Valid @RequestBody ProductUpsertRequestDto dto) { return service.create(dto); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR')")
    public ProductResponseDto update(@PathVariable Long id, @Valid @RequestBody ProductUpsertRequestDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public void delete(@PathVariable Long id) { service.delete(id); }
}
