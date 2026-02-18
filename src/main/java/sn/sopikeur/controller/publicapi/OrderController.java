package sn.sopikeur.controller.publicapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.dto.request.publicapi.commerce.OrderCreateRequest;
import sn.sopikeur.dto.response.publicapi.commerce.CommerceCreateResponse;
import sn.sopikeur.service.commerce.CommerceService;

@RestController
@RequestMapping(ApiConstants.V1)
@RequiredArgsConstructor
public class OrderController {
    private final CommerceService commerceService;

    @PostMapping("/orders")
    public ResponseEntity<CommerceCreateResponse> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commerceService.createOrder(request));
    }
}
