package sn.sopikeur.controller.publicapi;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.dto.request.publicapi.ContactMessageCreate;
import sn.sopikeur.dto.response.publicapi.ContactApiResponse;
import sn.sopikeur.entity.leads.ContactMessage;
import sn.sopikeur.security.ClientIpResolver;
import sn.sopikeur.security.TurnstileVerifier;
import sn.sopikeur.service.LeadService;

@RestController
@RequestMapping(ApiConstants.V1)
@RequiredArgsConstructor
public class LeadPublicController {

    private final LeadService leadService;
    private final ClientIpResolver clientIpResolver;
    private final TurnstileVerifier turnstileVerifier;

    @PostMapping("/contact")
    public ResponseEntity<ContactApiResponse> createContact(
        HttpServletRequest httpRequest,
        @Valid @RequestBody ContactMessageCreate request
    ) {
        assertContactPayloadAllowed(request);
        String clientIp = clientIpResolver.resolve(httpRequest);

        if (turnstileVerifier.isEnabled() && !turnstileVerifier.verify(request.getTurnstileToken(), clientIp)) {
            throw new IllegalArgumentException("Requête invalide");
        }

        ContactMessage saved = leadService.createContact(request, clientIp);
        ContactApiResponse response = new ContactApiResponse(
            "Contact request received",
            "cnt_" + saved.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private void assertContactPayloadAllowed(ContactMessageCreate request) {
        if (request.getWebsite() != null && !request.getWebsite().isBlank()) {
            throw new IllegalArgumentException("Requête invalide");
        }

        String message = request.getMessage();
        if (message != null) {
            int count = message.toLowerCase().split("http", -1).length - 1;
            if (count > 2) {
                throw new IllegalArgumentException("Requête invalide");
            }
        }
    }
}
