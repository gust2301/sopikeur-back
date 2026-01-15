package sn.sopikeur.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.stock.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
}
