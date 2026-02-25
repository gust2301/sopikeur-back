package sn.sopikeur.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.sopikeur.common.error.NotFoundException;
import sn.sopikeur.common.pagination.PageResponse;
import sn.sopikeur.dto.request.admin.ProductAssetAssignmentDto;
import sn.sopikeur.dto.request.admin.ProductUpsertRequestDto;
import sn.sopikeur.dto.response.admin.ProductResponseDto;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.catalog.ProductStatus;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.entity.media.MediaAsset;
import sn.sopikeur.mapper.AdminProductMapper;
import sn.sopikeur.repo.MediaAssetRepository;
import sn.sopikeur.repo.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final AdminProductMapper adminProductMapper;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponseDto> list(
        int page,
        int size,
        String sort,
        ProductType type,
        ProductStatus status,
        Boolean featured,
        String q
    ) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String[] sortParts = sort.split(",");
        Sort sorting = Sort.by(Sort.Direction.fromString(sortParts.length > 1 ? sortParts[1] : "desc"), sortParts[0]);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, sorting);

        Specification<Product> spec = Specification.where(null);
        if (type != null) spec = spec.and((r, qy, cb) -> cb.equal(r.get("type"), type));
        if (status != null) spec = spec.and((r, qy, cb) -> cb.equal(r.get("status"), status));
        if (featured != null) spec = spec.and((r, qy, cb) -> cb.equal(r.get("featured"), featured));
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((r, qy, cb) -> cb.or(
                cb.like(cb.lower(r.get("name")), like),
                cb.like(cb.lower(r.get("sku")), like),
                cb.like(cb.lower(r.get("slug")), like)
            ));
        }

        Page<Product> result = productRepository.findAll(spec, pageable);
        return PageResponse.<ProductResponseDto>builder()
            .items(result.getContent().stream().map(adminProductMapper::toDto).toList())
            .page(safePage)
            .size(safeSize)
            .total(result.getTotalElements())
            .totalPages(result.getTotalPages())
            .build();
    }

    @Transactional(readOnly = true)
    public ProductResponseDto get(Long id) {
        return adminProductMapper.toDto(
            productRepository.findById(id).orElseThrow(() -> new NotFoundException("Produit introuvable"))
        );
    }

    @Transactional
    public ProductResponseDto create(ProductUpsertRequestDto dto) {
        Product p = new Product();
        apply(p, dto);
        return adminProductMapper.toDto(productRepository.save(p));
    }

    @Transactional
    public ProductResponseDto update(Long id, ProductUpsertRequestDto dto) {
        Product p = productRepository.findById(id).orElseThrow(() -> new NotFoundException("Produit introuvable"));
        apply(p, dto);
        return adminProductMapper.toDto(productRepository.save(p));
    }

    @Transactional
    public void delete(Long id) {
        Product p = productRepository.findById(id).orElseThrow(() -> new NotFoundException("Produit introuvable"));
        p.setStatus(ProductStatus.ARCHIVED);
        productRepository.save(p);
    }

    @Transactional
    public void setProductAssets(Long productId, List<ProductAssetAssignmentDto> assignments) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NotFoundException("Produit introuvable"));
        mediaAssetRepository.detachAllFromProduct(productId);
        for (var dto : assignments) {
            MediaAsset asset = mediaAssetRepository.findById(dto.getAssetId())
                .orElseThrow(() -> new NotFoundException("Asset introuvable : " + dto.getAssetId()));
            asset.setProduct(product);
            asset.setCover(dto.isCover());
            asset.setSortOrder(dto.getSortOrder());
        }
    }

    private void apply(Product p, ProductUpsertRequestDto dto) {
        p.setSku(dto.getSku());
        p.setSlug(dto.getSlug());
        p.setName(dto.getName());
        p.setType(dto.getType());
        p.setStatus(dto.getStatus());
        p.setFeatured(Boolean.TRUE.equals(dto.getFeatured()));
        p.setPrice(dto.getPrice());
        p.setUnit(dto.getUnit());
        p.setDimensions(dto.getDimensions());
        p.setDescriptionShort(dto.getDescriptionShort());
        p.setDescriptionLong(dto.getDescriptionLong());
    }
}
