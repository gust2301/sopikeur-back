package sn.sopikeur.repo.push;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.push.PushSubscription;

import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    Optional<PushSubscription> findByEndpoint(String endpoint);
    void deleteByEndpoint(String endpoint);
}
