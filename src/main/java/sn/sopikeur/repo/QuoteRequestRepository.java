package sn.sopikeur.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.leads.QuoteRequest;
import sn.sopikeur.entity.leads.QuoteStatus;

public interface QuoteRequestRepository extends JpaRepository<QuoteRequest, Long> {
    long countByStatus(QuoteStatus status);
}
