package sn.sopikeur.controller.admin;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.dto.request.admin.UpdateContactStatusRequest;
import sn.sopikeur.dto.request.admin.UpdatePreorderStatusRequest;
import sn.sopikeur.dto.request.admin.UpdateQuoteStatusRequest;
import sn.sopikeur.dto.response.admin.ContactMessageResponse;
import sn.sopikeur.dto.response.admin.PreorderRequestResponse;
import sn.sopikeur.dto.response.admin.QuoteRequestResponse;
import sn.sopikeur.service.LeadService;

@RestController
@RequestMapping(ApiConstants.V1_ADMIN)
@RequiredArgsConstructor
public class LeadAdminController {

    private final LeadService leadService;

    @GetMapping("/quotes")
    public List<QuoteRequestResponse> listQuotes() {
        return leadService.listQuotes();
    }

    @PatchMapping("/quotes/{id}")
    public ResponseEntity<QuoteRequestResponse> updateQuoteStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateQuoteStatusRequest request
    ) {
        return ResponseEntity.ok(leadService.updateQuoteStatus(id, request));
    }

    @GetMapping("/contact")
    public List<ContactMessageResponse> listContacts() {
        return leadService.listContacts();
    }

    @PatchMapping("/contact/{id}")
    public ResponseEntity<ContactMessageResponse> updateContactStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateContactStatusRequest request
    ) {
        return ResponseEntity.ok(leadService.updateContactStatus(id, request));
    }

    @GetMapping("/preorders")
    public List<PreorderRequestResponse> listPreorders() {
        return leadService.listPreorders();
    }

    @PatchMapping("/preorders/{id}")
    public ResponseEntity<PreorderRequestResponse> updatePreorderStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdatePreorderStatusRequest request
    ) {
        return ResponseEntity.ok(leadService.updatePreorderStatus(id, request));
    }
}
