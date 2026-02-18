package sn.sopikeur.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.leads.item.QuoteRequestPack;

public interface QuoteRequestPackRepository extends JpaRepository<QuoteRequestPack, Long> {
}
