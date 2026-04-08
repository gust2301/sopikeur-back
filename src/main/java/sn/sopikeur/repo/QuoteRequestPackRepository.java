package sn.sopikeur.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.leads.item.QuoteRequestPack;

public interface QuoteRequestPackRepository extends JpaRepository<QuoteRequestPack, Long> {
    List<QuoteRequestPack> findByQuoteRequestIdOrderByIdAsc(Long quoteRequestId);
}
