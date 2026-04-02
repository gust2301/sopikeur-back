package sn.sopikeur.controller.admin;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.sopikeur.dto.request.admin.AddOrderItemRequest;
import sn.sopikeur.dto.request.admin.UpdateOrderDetailsRequest;
import sn.sopikeur.common.pagination.PageResponse;
import sn.sopikeur.dto.response.admin.OrderAdminResponseDto;
import sn.sopikeur.service.OrderAdminService;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderAdminService orderAdminService;

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

    @PostMapping("/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public OrderAdminResponseDto addItem(
        @PathVariable Long id,
        @Valid @RequestBody AddOrderItemRequest body
    ) {
        return orderAdminService.addItem(id, body);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public OrderAdminResponseDto updateStatus(
        @PathVariable Long id,
        @RequestBody Map<String, String> body
    ) {
        return orderAdminService.updateStatus(id, body.get("status"));
    }

    @PatchMapping("/{id}/details")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public OrderAdminResponseDto updateDetails(
        @PathVariable Long id,
        @RequestBody UpdateOrderDetailsRequest body
    ) {
        return orderAdminService.updateDetails(id, body);
    }

    @PatchMapping("/{id}/payment")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public OrderAdminResponseDto recordPayment(
        @PathVariable Long id,
        @RequestBody Map<String, Object> body
    ) {
        java.math.BigDecimal amountPaid = new java.math.BigDecimal(body.get("amountPaid").toString());
        return orderAdminService.recordPayment(id, amountPaid);
    }
}
