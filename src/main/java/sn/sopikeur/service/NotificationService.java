package sn.sopikeur.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sn.sopikeur.entity.leads.ContactMessage;
import sn.sopikeur.entity.leads.PreorderRequest;
import sn.sopikeur.entity.leads.QuoteRequest;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifyPreorderCreated(PreorderRequest preorder) {
        log.info("TODO: envoyer notification précommande id={}", preorder.getId());
    }

    public void notifyQuoteCreated(QuoteRequest quote) {
        log.info("TODO: envoyer notification devis id={}", quote.getId());
    }

    public void notifyContactCreated(ContactMessage contact) {
        log.info("TODO: envoyer notification contact id={}", contact.getId());
    }
}
