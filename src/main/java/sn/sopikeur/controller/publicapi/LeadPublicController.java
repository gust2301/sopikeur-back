package sn.sopikeur.controller.publicapi;

import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping(ApiConstants.V1)
@RequiredArgsConstructor
public class LeadPublicController {

    private final LeadService leadService;

    @PostMapping("/quotes")
    public ResponseEntity<Void> createQuote(
        HttpServletRequest httpRequest,
        @Valid @RequestBody QuoteRequestCreate request
    ) {
        leadService.createQuote(request, resolveClientKey(httpRequest));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/contact")
    public ResponseEntity<Void> createContact(
        HttpServletRequest httpRequest,
        @Valid @RequestBody ContactMessageCreate request
    ) {
        leadService.createContact(request, resolveClientKey(httpRequest));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/preorders")
    public ResponseEntity<Void> createPreorder(
        HttpServletRequest httpRequest,
        @Valid @RequestBody PreorderRequestCreate request
    ) {
        leadService.createPreorder(request, resolveClientKey(httpRequest));
        return ResponseEntity.ok().build();
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
