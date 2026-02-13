package sn.sopikeur.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.sopikeur.common.error.NotFoundException;
import sn.sopikeur.dto.request.admin.UpdateContactStatusRequest;
import sn.sopikeur.dto.request.admin.UpdatePreorderStatusRequest;
import sn.sopikeur.dto.request.admin.UpdateQuoteStatusRequest;
import sn.sopikeur.dto.request.publicapi.ContactMessageCreate;
import sn.sopikeur.dto.request.publicapi.PreorderRequestCreate;
import sn.sopikeur.dto.request.publicapi.QuoteRequestCreate;
import sn.sopikeur.dto.response.admin.ContactMessageResponse;
import sn.sopikeur.dto.response.admin.PreorderRequestResponse;
import sn.sopikeur.dto.response.admin.QuoteRequestResponse;
import sn.sopikeur.entity.leads.ContactMessage;
import sn.sopikeur.entity.leads.ContactStatus;
import sn.sopikeur.entity.leads.PreorderRequest;
import sn.sopikeur.entity.leads.PreorderStatus;
import sn.sopikeur.entity.leads.QuoteRequest;
import sn.sopikeur.entity.leads.QuoteStatus;
import sn.sopikeur.mapper.LeadMapper;
import sn.sopikeur.repo.ContactMessageRepository;
import sn.sopikeur.repo.PreorderRequestRepository;
import sn.sopikeur.repo.QuoteRequestRepository;

@Service
@RequiredArgsConstructor
public class LeadService {

    private final QuoteRequestRepository quoteRequestRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final PreorderRequestRepository preorderRequestRepository;
    private final LeadMapper leadMapper;
    private final NotificationService notificationService;

    @Transactional
    public void createQuote(QuoteRequestCreate request, String clientKey) {
        assertLeadAllowed(request.getWebsite());
        QuoteRequest quote = new QuoteRequest();
        quote.setFullName(request.getFullName());
        quote.setEmail(request.getEmail());
        quote.setPhone(request.getPhone());
        quote.setMessage(request.getMessage());
        quote.setStatus(QuoteStatus.NEW);
        QuoteRequest saved = quoteRequestRepository.save(quote);
        notificationService.notifyQuoteCreated(saved);
    }

    @Transactional
    public ContactMessage createContact(ContactMessageCreate request, String clientKey) {
        ContactMessage contact = new ContactMessage();
        contact.setFullName(request.getName());
        contact.setCustomerType(request.getCustomerType());
        contact.setEmail(request.getEmail());
        contact.setPhone(request.getPhone());
        contact.setMessage(request.getMessage());
        contact.setStatus(ContactStatus.NEW);
        ContactMessage saved = contactMessageRepository.save(contact);
        notificationService.notifyContactCreated(saved);
        return saved;
    }

    @Transactional
    public void createPreorder(PreorderRequestCreate request, String clientKey) {
        assertLeadAllowed(request.getWebsite());
        PreorderRequest preorder = new PreorderRequest();
        preorder.setFullName(request.getFullName());
        preorder.setEmail(request.getEmail());
        preorder.setPhone(request.getPhone());
        preorder.setProductSlug(request.getProductSlug());
        preorder.setQuantity(request.getQuantity());
        preorder.setMessage(request.getMessage());
        preorder.setStatus(PreorderStatus.NEW);
        PreorderRequest saved = preorderRequestRepository.save(preorder);
        notificationService.notifyPreorderCreated(saved);
    }

    @Transactional(readOnly = true)
    public List<QuoteRequestResponse> listQuotes() {
        return quoteRequestRepository.findAll().stream()
            .map(leadMapper::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContactMessageResponse> listContacts() {
        return contactMessageRepository.findAll().stream()
            .map(leadMapper::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PreorderRequestResponse> listPreorders() {
        return preorderRequestRepository.findAll().stream()
            .map(leadMapper::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public QuoteRequestResponse updateQuoteStatus(Long id, UpdateQuoteStatusRequest request) {
        QuoteRequest quote = quoteRequestRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Demande de devis introuvable"));
        quote.setStatus(request.getStatus());
        return leadMapper.toResponse(quoteRequestRepository.save(quote));
    }

    @Transactional
    public ContactMessageResponse updateContactStatus(Long id, UpdateContactStatusRequest request) {
        ContactMessage contact = contactMessageRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Message introuvable"));
        contact.setStatus(request.getStatus());
        return leadMapper.toResponse(contactMessageRepository.save(contact));
    }

    @Transactional
    public PreorderRequestResponse updatePreorderStatus(Long id, UpdatePreorderStatusRequest request) {
        PreorderRequest preorder = preorderRequestRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Précommande introuvable"));
        preorder.setStatus(request.getStatus());
        return leadMapper.toResponse(preorderRequestRepository.save(preorder));
    }

    private void assertLeadAllowed(String honeypot) {
        if (honeypot != null && !honeypot.isBlank()) {
            throw new IllegalArgumentException("Requête rejetée.");
        }
    }
}
