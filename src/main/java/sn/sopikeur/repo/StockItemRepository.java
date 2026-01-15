package sn.sopikeur.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.stock.StockItem;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {
    Optional<StockItem> findByProductId(Long productId);
}
