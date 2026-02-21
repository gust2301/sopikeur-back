package sn.sopikeur.controller.publicapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.dto.request.publicapi.commerce.PreorderCreateRequest;
import sn.sopikeur.dto.response.publicapi.commerce.CommerceCreateResponse;
import sn.sopikeur.service.commerce.CommerceService;

@RestController
@RequestMapping(ApiConstants.V1)
@RequiredArgsConstructor
public class PreorderController {
    private final CommerceService commerceService;

    @PostMapping("/preorders")
    public ResponseEntity<CommerceCreateResponse> createPreorder(@Valid @RequestBody PreorderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commerceService.createPreorder(request));
    }
}
