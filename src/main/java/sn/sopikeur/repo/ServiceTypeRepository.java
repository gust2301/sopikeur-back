package sn.sopikeur.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.catalog.ServiceTypeEntity;

public interface ServiceTypeRepository extends JpaRepository<ServiceTypeEntity, Long> {
    List<ServiceTypeEntity> findAllByOrderByNameAsc();
    List<ServiceTypeEntity> findByActiveTrueOrderByNameAsc();
    Optional<ServiceTypeEntity> findByCodeIgnoreCase(String code);
}
