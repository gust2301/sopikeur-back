package sn.sopikeur.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.blog.Article;
import sn.sopikeur.entity.blog.ArticleStatus;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findBySlug(String slug);

    Page<Article> findByStatusOrderByPublishedAtDesc(ArticleStatus status, Pageable pageable);

    List<Article> findByStatusOrderByPublishedAtDesc(ArticleStatus status);
}
