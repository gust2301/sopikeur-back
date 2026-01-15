package sn.sopikeur.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.leads.ContactMessage;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
}
