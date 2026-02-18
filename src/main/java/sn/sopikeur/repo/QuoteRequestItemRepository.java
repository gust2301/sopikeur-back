package sn.sopikeur.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.leads.item.QuoteRequestItem;

public interface QuoteRequestItemRepository extends JpaRepository<QuoteRequestItem, Long> {
}
