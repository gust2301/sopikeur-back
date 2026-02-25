package sn.sopikeur.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.leads.PreorderRequest;
import sn.sopikeur.entity.leads.PreorderStatus;

public interface PreorderRequestRepository extends JpaRepository<PreorderRequest, Long> {
    long countByStatus(PreorderStatus status);
}
