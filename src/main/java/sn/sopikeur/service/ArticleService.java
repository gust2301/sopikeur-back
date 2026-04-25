package sn.sopikeur.service;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.sopikeur.common.error.NotFoundException;
import sn.sopikeur.dto.request.admin.ArticleRequest;
import sn.sopikeur.dto.response.admin.ArticleAdminResponse;
import sn.sopikeur.dto.response.publicapi.ArticleListItemResponse;
import sn.sopikeur.dto.response.publicapi.ArticlePublicResponse;
import sn.sopikeur.entity.blog.Article;
import sn.sopikeur.entity.blog.ArticleStatus;
import sn.sopikeur.repo.ArticleRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class ArticleService {

    private final ArticleRepository repo;

    public ArticleAdminResponse create(ArticleRequest req) {
        ensureSlugAvailable(req.getSlug(), null);
        Article article = new Article();
        applyRequest(article, req);
        return toAdminResponse(repo.save(article));
    }

    public ArticleAdminResponse update(Long id, ArticleRequest req) {
        Article article = repo.findById(id)
            .orElseThrow(() -> new NotFoundException("Article introuvable"));
        ensureSlugAvailable(req.getSlug(), id);
        applyRequest(article, req);
        return toAdminResponse(repo.save(article));
    }

    public void delete(Long id) {
        Article article = repo.findById(id)
            .orElseThrow(() -> new NotFoundException("Article introuvable"));
        repo.delete(article);
    }

    @Transactional(readOnly = true)
    public List<ArticleAdminResponse> listAll() {
        return repo.findAll().stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .map(this::toAdminResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ArticleAdminResponse getById(Long id) {
        return repo.findById(id)
            .map(this::toAdminResponse)
            .orElseThrow(() -> new NotFoundException("Article introuvable"));
    }

    @Transactional(readOnly = true)
    public List<ArticleListItemResponse> listPublished() {
        return repo.findByStatusOrderByPublishedAtDesc(ArticleStatus.PUBLISHED).stream()
            .map(this::toListItemResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ArticlePublicResponse getPublishedBySlug(String slug) {
        Article article = repo.findBySlug(slug)
            .filter(found -> found.getStatus() == ArticleStatus.PUBLISHED)
            .orElseThrow(() -> new NotFoundException("Article introuvable"));
        return toPublicResponse(article);
    }

    private void applyRequest(Article article, ArticleRequest req) {
        article.setSlug(req.getSlug());
        article.setTitle(req.getTitle());
        article.setExcerpt(req.getExcerpt());
        article.setContent(req.getContent());
        article.setCoverUrl(req.getCoverUrl());
        article.setStatus(req.getStatus() == null ? ArticleStatus.DRAFT : req.getStatus());
        article.setReadingTimeMinutes(req.getReadingTimeMinutes() == null ? 3 : Math.max(req.getReadingTimeMinutes(), 1));
        article.setMetaTitle(req.getMetaTitle());
        article.setMetaDescription(req.getMetaDescription());

        if (article.getStatus() == ArticleStatus.PUBLISHED && article.getPublishedAt() == null) {
            article.setPublishedAt(OffsetDateTime.now());
        }
        if (article.getStatus() == ArticleStatus.DRAFT) {
            article.setPublishedAt(null);
        }
    }

    private void ensureSlugAvailable(String slug, Long currentId) {
        repo.findBySlug(slug).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new IllegalArgumentException("Un article avec ce slug existe deja.");
            }
        });
    }

    private ArticleAdminResponse toAdminResponse(Article article) {
        return ArticleAdminResponse.builder()
            .id(article.getId())
            .slug(article.getSlug())
            .title(article.getTitle())
            .excerpt(article.getExcerpt())
            .content(article.getContent())
            .coverUrl(article.getCoverUrl())
            .status(article.getStatus())
            .publishedAt(article.getPublishedAt())
            .readingTimeMinutes(article.getReadingTimeMinutes())
            .metaTitle(article.getMetaTitle())
            .metaDescription(article.getMetaDescription())
            .createdAt(article.getCreatedAt())
            .updatedAt(article.getUpdatedAt())
            .build();
    }

    private ArticlePublicResponse toPublicResponse(Article article) {
        return ArticlePublicResponse.builder()
            .id(article.getId())
            .slug(article.getSlug())
            .title(article.getTitle())
            .excerpt(article.getExcerpt())
            .content(article.getContent())
            .coverUrl(article.getCoverUrl())
            .publishedAt(article.getPublishedAt())
            .readingTimeMinutes(article.getReadingTimeMinutes())
            .metaTitle(article.getMetaTitle())
            .metaDescription(article.getMetaDescription())
            .build();
    }

    private ArticleListItemResponse toListItemResponse(Article article) {
        return ArticleListItemResponse.builder()
            .id(article.getId())
            .slug(article.getSlug())
            .title(article.getTitle())
            .excerpt(article.getExcerpt())
            .coverUrl(article.getCoverUrl())
            .publishedAt(article.getPublishedAt())
            .readingTimeMinutes(article.getReadingTimeMinutes())
            .build();
    }
}
