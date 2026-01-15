package sn.sopikeur.controller.publicapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.dto.request.publicapi.ContactMessageCreate;
import sn.sopikeur.dto.request.publicapi.PreorderRequestCreate;
import sn.sopikeur.dto.request.publicapi.QuoteRequestCreate;
import sn.sopikeur.service.LeadService;

@RestController
@RequestMapping(ApiConstants.PUBLIC_API_BASE)
@RequiredArgsConstructor
public class LeadPublicController {

    private final LeadService leadService;

    @PostMapping("/quotes")
    public ResponseEntity<Void> createQuote(@Valid @RequestBody QuoteRequestCreate request) {
        leadService.createQuote(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/contact")
    public ResponseEntity<Void> createContact(@Valid @RequestBody ContactMessageCreate request) {
        leadService.createContact(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/preorders")
    public ResponseEntity<Void> createPreorder(@Valid @RequestBody PreorderRequestCreate request) {
        leadService.createPreorder(request);
        return ResponseEntity.ok().build();
    }
}
