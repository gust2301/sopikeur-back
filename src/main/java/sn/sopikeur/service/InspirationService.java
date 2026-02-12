package sn.sopikeur.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.sopikeur.common.error.NotFoundException;
import sn.sopikeur.common.pagination.PageResponse;
import sn.sopikeur.common.utils.StringUtils;
import sn.sopikeur.dto.request.admin.InspirationRequest;
import sn.sopikeur.dto.response.publicapi.InspirationResponse;
import sn.sopikeur.dto.response.publicapi.ProductSummaryResponse;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.entity.inspirations.Inspiration;
import sn.sopikeur.mapper.InspirationMapper;
import sn.sopikeur.repo.InspirationRepository;
import sn.sopikeur.repo.ProductRepository;

@Service
@RequiredArgsConstructor
public class InspirationService {

    private final InspirationRepository inspirationRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final InspirationMapper inspirationMapper;

    @Transactional(readOnly = true)
    public List<InspirationResponse> list(String tag, ProductType type) {
        Page<Inspiration> inspirations = listInternal(tag, type, Pageable.unpaged());
        return inspirations.getContent().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<InspirationResponse> listPaged(String tag, ProductType type, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());
        Page<Inspiration> inspirations = listInternal(tag, type, pageable);
        return PageResponse.<InspirationResponse>builder()
            .items(inspirations.getContent().stream().map(this::toResponse).collect(Collectors.toList()))
            .total(inspirations.getTotalElements())
            .build();
    }

    private Page<Inspiration> listInternal(String tag, ProductType type, Pageable pageable) {
        if (tag == null && type == null) {
            return inspirationRepository.findAll(pageable);
        }
        if (tag != null && type == null) {
            return inspirationRepository.findByTagsContainingIgnoreCase(tag, pageable);
        }
        if (tag == null) {
            return inspirationRepository.findDistinctByProducts_Type(type, pageable);
        }
        return inspirationRepository.findDistinctByTagsContainingIgnoreCaseAndProducts_Type(tag, type, pageable);
    }

    @Transactional(readOnly = true)
    public List<InspirationResponse> list(String tag) {
        return list(tag, null);
    }

    @Transactional(readOnly = true)
    public InspirationResponse getBySlug(String slug) {
        Inspiration inspiration = inspirationRepository.findBySlug(slug)
            .orElseThrow(() -> new NotFoundException("Inspiration introuvable"));
        return toResponse(inspiration);
    }

    @Transactional
    public InspirationResponse create(InspirationRequest request) {
        Inspiration inspiration = new Inspiration();
        applyRequest(inspiration, request);
        return toResponse(inspirationRepository.save(inspiration));
    }

    @Transactional
    public InspirationResponse update(Long id, InspirationRequest request) {
        Inspiration inspiration = inspirationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Inspiration introuvable"));
        applyRequest(inspiration, request);
        return toResponse(inspirationRepository.save(inspiration));
    }

    @Transactional
    public void delete(Long id) {
        Inspiration inspiration = inspirationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Inspiration introuvable"));
        inspirationRepository.delete(inspiration);
    }

    @Transactional
    public InspirationResponse linkProduct(Long inspirationId, Long productId) {
        Inspiration inspiration = inspirationRepository.findById(inspirationId)
            .orElseThrow(() -> new NotFoundException("Inspiration introuvable"));
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NotFoundException("Produit introuvable"));
        inspiration.getProducts().add(product);
        return toResponse(inspirationRepository.save(inspiration));
    }

    @Transactional
    public InspirationResponse unlinkProduct(Long inspirationId, Long productId) {
        Inspiration inspiration = inspirationRepository.findById(inspirationId)
            .orElseThrow(() -> new NotFoundException("Inspiration introuvable"));
        inspiration.getProducts().removeIf(product -> product.getId().equals(productId));
        return toResponse(inspirationRepository.save(inspiration));
    }

    private void applyRequest(Inspiration inspiration, InspirationRequest request) {
        inspiration.setSlug(request.getSlug());
        inspiration.setTitle(request.getTitle());
        inspiration.setTags(StringUtils.joinCsv(request.getTags()));
        inspiration.setCoverUrl(request.getCoverUrl());
        inspiration.setGalleryUrls(StringUtils.joinCsv(request.getGalleryUrls()));
    }

    private InspirationResponse toResponse(Inspiration inspiration) {
        List<ProductSummaryResponse> products = inspiration.getProducts().stream()
            .map(productService::toSummary)
            .collect(Collectors.toList());
        return inspirationMapper.toResponse(inspiration, products);
    }
}
