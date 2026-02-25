package sn.sopikeur.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.sopikeur.entity.leads.ContactMessage;
import sn.sopikeur.entity.leads.ContactStatus;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    long countByStatus(ContactStatus status);
}
