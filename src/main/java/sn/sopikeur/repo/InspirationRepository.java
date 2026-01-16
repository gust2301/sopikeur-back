package sn.sopikeur.repo;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.entity.inspirations.Inspiration;

public interface InspirationRepository extends JpaRepository<Inspiration, Long> {
    Optional<Inspiration> findBySlug(String slug);

    Page<Inspiration> findByTagsContainingIgnoreCase(String tag, Pageable pageable);

    Page<Inspiration> findDistinctByProducts_Type(ProductType type, Pageable pageable);

    Page<Inspiration> findDistinctByTagsContainingIgnoreCaseAndProducts_Type(
        String tag,
        ProductType type,
        Pageable pageable
    );
}
