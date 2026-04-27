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
import sn.sopikeur.entity.stock.StockItem;
import sn.sopikeur.mapper.AdminProductMapper;
import sn.sopikeur.repo.MediaAssetRepository;
import sn.sopikeur.repo.ProductRepository;
import sn.sopikeur.repo.StockItemRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final StockItemRepository stockItemRepository;
    private final AdminProductMapper adminProductMapper;
    private final ProductPricingService productPricingService;

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
        if (status != null) {
            spec = spec.and((r, qy, cb) -> cb.equal(r.get("status"), status));
        } else {
            spec = spec.and((r, qy, cb) -> cb.notEqual(r.get("status"), ProductStatus.ARCHIVED));
        }
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
            .items(result.getContent().stream().map(this::toDto).toList())
            .page(safePage)
            .size(safeSize)
            .total(result.getTotalElements())
            .totalPages(result.getTotalPages())
            .build();
    }

    @Transactional(readOnly = true)
    public ProductResponseDto get(Long id) {
        return toDto(productRepository.findById(id).orElseThrow(() -> new NotFoundException("Produit introuvable")));
    }

    @Transactional
    public ProductResponseDto create(ProductUpsertRequestDto dto) {
        Product p = new Product();
        apply(p, dto);
        Product saved = productRepository.save(p);
        if (saved.getType() != ProductType.SERVICE) {
            StockItem stock = new StockItem();
            stock.setProduct(saved);
            stock.setQuantity(0);
            stock.setReserved(0);
            stock.setPreorderAllowed(false);
            stockItemRepository.save(stock);
        }
        return toDto(saved);
    }

    @Transactional
    public ProductResponseDto update(Long id, ProductUpsertRequestDto dto) {
        Product p = productRepository.findById(id).orElseThrow(() -> new NotFoundException("Produit introuvable"));
        apply(p, dto);
        return toDto(productRepository.save(p));
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
        validatePromotion(dto);
        p.setSku(dto.getSku());
        p.setSlug(dto.getSlug());
        p.setName(dto.getName());
        p.setType(dto.getType());
        p.setStatus(dto.getStatus());
        p.setFeatured(Boolean.TRUE.equals(dto.getFeatured()));
        p.setPrice(dto.getPrice());
        p.setPromoActive(Boolean.TRUE.equals(dto.getPromoActive()));
        p.setPromoPrice(dto.getPromoPrice());
        p.setPromoStartDate(dto.getPromoStartDate());
        p.setPromoEndDate(dto.getPromoEndDate());
        p.setPromoLabel(trimToNull(dto.getPromoLabel()));
        p.setUnit(dto.getUnit());
        p.setDimensions(dto.getDimensions());
        p.setDescriptionShort(dto.getDescriptionShort());
        p.setDescriptionLong(dto.getDescriptionLong());
    }

    private ProductResponseDto toDto(Product product) {
        return adminProductMapper.toDto(
            product,
            productPricingService.isPromotionActive(product),
            productPricingService.resolveEffectivePrice(product),
            productPricingService.resolveDiscountPercent(product)
        );
    }

    private void validatePromotion(ProductUpsertRequestDto dto) {
        if (!Boolean.TRUE.equals(dto.getPromoActive())) {
            dto.setPromoPrice(null);
            dto.setPromoStartDate(null);
            dto.setPromoEndDate(null);
            dto.setPromoLabel(null);
            return;
        }
        if (dto.getPromoPrice() == null) {
            throw new IllegalArgumentException("Le prix promotionnel est obligatoire si la promotion est active.");
        }
        if (dto.getPromoPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le prix promotionnel ne peut pas etre negatif.");
        }
        if (dto.getPrice() == null || dto.getPromoPrice().compareTo(dto.getPrice()) >= 0) {
            throw new IllegalArgumentException("Le prix promotionnel doit etre strictement inferieur au prix normal.");
        }
        if (dto.getPromoStartDate() != null && dto.getPromoEndDate() != null
            && dto.getPromoEndDate().isBefore(dto.getPromoStartDate())) {
            throw new IllegalArgumentException("La date de fin de promotion doit etre posterieure ou egale a la date de debut.");
        }
        dto.setPromoLabel(trimToNull(dto.getPromoLabel()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
