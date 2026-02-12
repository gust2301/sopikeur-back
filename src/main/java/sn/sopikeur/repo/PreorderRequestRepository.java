package sn.sopikeur.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.leads.PreorderRequest;

public interface PreorderRequestRepository extends JpaRepository<PreorderRequest, Long> {
}
