package sn.sopikeur.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.inspirations.Inspiration;

public interface InspirationRepository extends JpaRepository<Inspiration, Long> {
    Optional<Inspiration> findBySlug(String slug);

    List<Inspiration> findByTagsContainingIgnoreCase(String tag);
}
