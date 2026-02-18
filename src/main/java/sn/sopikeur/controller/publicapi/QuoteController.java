package sn.sopikeur.controller.publicapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.dto.request.publicapi.commerce.QuoteCreateRequest;
import sn.sopikeur.dto.response.publicapi.commerce.CommerceCreateResponse;
import sn.sopikeur.service.commerce.CommerceService;

@RestController
@RequestMapping(ApiConstants.V1)
@RequiredArgsConstructor
public class QuoteController {
    private final CommerceService commerceService;

    @PostMapping("/quotes")
    public ResponseEntity<CommerceCreateResponse> createQuote(@Valid @RequestBody QuoteCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commerceService.createQuote(request));
    }
}
