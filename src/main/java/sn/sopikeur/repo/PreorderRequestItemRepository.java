package sn.sopikeur.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.leads.item.PreorderRequestItem;

public interface PreorderRequestItemRepository extends JpaRepository<PreorderRequestItem, Long> {
}
