package sn.sopikeur.controller.admin;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.common.pagination.PageResponse;
import sn.sopikeur.dto.request.admin.AddOrderItemRequest;
import sn.sopikeur.dto.request.admin.AddOrderServiceRequest;
import sn.sopikeur.dto.request.admin.MarkOrderDeliveredRequest;
import sn.sopikeur.dto.request.admin.MarkOrderInstalledRequest;
import sn.sopikeur.dto.request.admin.OrderExpenseUpsertRequest;
import sn.sopikeur.dto.request.admin.UpdateOrderItemRequest;
import sn.sopikeur.dto.request.admin.UpdateOrderDeliveryRequest;
import sn.sopikeur.dto.request.admin.UpdateOrderDetailsRequest;
import sn.sopikeur.dto.response.admin.OrderAdminResponseDto;
import sn.sopikeur.dto.response.admin.OrderExpenseAdminResponseDto;
import sn.sopikeur.dto.response.admin.OrderExpenseListAdminResponseDto;
import sn.sopikeur.dto.response.admin.OrderPaymentAdminResponseDto;
import sn.sopikeur.service.OrderAdminService;
import sn.sopikeur.service.OrderDocumentPdfService;
import sn.sopikeur.service.OrderExpenseAdminService;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderAdminService orderAdminService;
    private final OrderDocumentPdfService orderDocumentPdfService;
    private final OrderExpenseAdminService orderExpenseAdminService;

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

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public List<OrderPaymentAdminResponseDto> listPayments(@PathVariable Long id) {
        return orderAdminService.getPayments(id);
    }

    @GetMapping("/{id}/expenses")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public OrderExpenseListAdminResponseDto listExpenses(@PathVariable Long id) {
        return orderExpenseAdminService.list(id);
    }

    @PostMapping("/{id}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public OrderExpenseAdminResponseDto createExpense(
        @PathVariable Long id,
        @Valid @RequestBody OrderExpenseUpsertRequest body
    ) {
        return orderExpenseAdminService.create(id, body);
    }

    @PutMapping("/{id}/expenses/{expenseId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public OrderExpenseAdminResponseDto updateExpense(
        @PathVariable Long id,
        @PathVariable Long expenseId,
        @Valid @RequestBody OrderExpenseUpsertRequest body
    ) {
        return orderExpenseAdminService.update(id, expenseId, body);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}/expenses/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void deleteExpense(
        @PathVariable Long id,
        @PathVariable Long expenseId
    ) {
        orderExpenseAdminService.delete(id, expenseId);
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

    @PostMapping("/{id}/services")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES','EDITOR')")
    public OrderAdminResponseDto addService(
        @PathVariable Long id,
        @Valid @RequestBody AddOrderServiceRequest body
    ) {
        return orderAdminService.addService(id, body);
    }

    @PatchMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES','EDITOR')")
    public OrderAdminResponseDto updateItem(
        @PathVariable Long id,
        @PathVariable Long itemId,
        @Valid @RequestBody UpdateOrderItemRequest body
    ) {
        return orderAdminService.updateItem(id, itemId, body);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES','EDITOR')")
    public void deleteItem(
        @PathVariable Long id,
        @PathVariable Long itemId
    ) {
        orderAdminService.deleteItem(id, itemId);
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES','EDITOR')")
    public OrderAdminResponseDto updateDetails(
        @PathVariable Long id,
        @RequestBody UpdateOrderDetailsRequest body
    ) {
        return orderAdminService.updateDetails(id, body);
    }

    @PatchMapping("/{id}/delivery")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES','EDITOR')")
    public OrderAdminResponseDto updateDelivery(
        @PathVariable Long id,
        @RequestBody UpdateOrderDeliveryRequest body
    ) {
        return orderAdminService.updateDelivery(id, body);
    }

    @PostMapping("/{id}/mark-delivered")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES','EDITOR')")
    public OrderAdminResponseDto markDelivered(
        @PathVariable Long id,
        @RequestBody(required = false) MarkOrderDeliveredRequest body
    ) {
        return orderAdminService.markDelivered(id, body);
    }

    @PostMapping("/{id}/mark-installed")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES','EDITOR')")
    public OrderAdminResponseDto markInstalled(
        @PathVariable Long id,
        @RequestBody(required = false) MarkOrderInstalledRequest body
    ) {
        return orderAdminService.markInstalled(id, body);
    }

    @PatchMapping("/{id}/payment")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public OrderAdminResponseDto recordPayment(
        @PathVariable Long id,
        @RequestBody Map<String, Object> body
    ) {
        BigDecimal amountPaid = new BigDecimal(body.get("amountPaid").toString());
        return orderAdminService.recordPayment(id, amountPaid);
    }

    @PostMapping("/{id}/issue-invoice")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public Map<String, String> issueInvoice(@PathVariable Long id) {
        return Map.of("invoiceNumber", orderAdminService.issueInvoice(id));
    }

    @GetMapping("/{id}/invoice.pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {
        OrderAdminService.InvoiceDocumentData document = orderAdminService.getInvoiceDocument(id);
        byte[] pdf = orderDocumentPdfService.buildInvoicePdf(
            document.order(),
            document.payments(),
            document.paidTotal(),
            document.dueTotal()
        );
        String filename = document.order().getInvoiceNumber() + ".pdf";
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
            .body(pdf);
    }
}
