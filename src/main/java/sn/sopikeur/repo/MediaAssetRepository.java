package sn.sopikeur.repo;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.sopikeur.entity.media.MediaAsset;

import java.util.List;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    List<MediaAsset> findAll(Sort sort);

    List<MediaAsset> findByProductId(Long productId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MediaAsset a SET a.product = null, a.cover = false, a.sortOrder = null WHERE a.product.id = :productId")
    void detachAllFromProduct(@Param("productId") Long productId);
}
