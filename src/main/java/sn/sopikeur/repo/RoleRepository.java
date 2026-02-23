package sn.sopikeur.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.auth.RoleEntity;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByCode(String code);
}
