package sn.sopikeur.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.auth.AdminUserEntity;

public interface AdminUserRepository extends JpaRepository<AdminUserEntity, Long> {
    Optional<AdminUserEntity> findByEmail(String email);
}
