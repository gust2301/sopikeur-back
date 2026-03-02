package sn.sopikeur.controller.admin;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.sopikeur.common.pagination.PageResponse;
import sn.sopikeur.dto.request.admin.payment.CreateBalanceLinkRequest;
import sn.sopikeur.dto.response.admin.OrderAdminResponseDto;
import sn.sopikeur.dto.response.publicapi.stripe.StripeCheckoutResponse;
import sn.sopikeur.service.OrderAdminService;
import sn.sopikeur.service.payment.PaymentOrchestratorService;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderAdminService orderAdminService;
    private final PaymentOrchestratorService paymentOrchestratorService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public OrderAdminResponseDto getById(@PathVariable Long id) {
        return orderAdminService.getById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public PageResponse<OrderAdminResponseDto> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String status
    ) {
        return orderAdminService.list(page, size, status);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public OrderAdminResponseDto updateStatus(
        @PathVariable Long id,
        @RequestBody Map<String, String> body
    ) {
        return orderAdminService.updateStatus(id, body.get("status"));
    }

    /**
     * Génère un lien de paiement (solde / acompte) pour une commande via le provider choisi.
     * Utilisé par le backoffice pour envoyer un lien au client.
     * POST /api/v1/admin/orders/{publicId}/payments/balance-link
     * Body: { "provider": "WAVE", "purpose": "BALANCE" }
     */
    @PostMapping("/{publicId}/payments/balance-link")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public ResponseEntity<StripeCheckoutResponse> createBalanceLink(
        @PathVariable String publicId,
        @Valid @RequestBody CreateBalanceLinkRequest request
    ) {
        StripeCheckoutResponse response = paymentOrchestratorService.createOrderPayment(
            publicId,
            request.getProvider(),
            request.getPurpose()
        );
        return ResponseEntity.ok(response);
    }
}
