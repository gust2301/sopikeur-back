package sn.sopikeur.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.service.OrderAdminService;
import sn.sopikeur.service.OrderDocumentPdfService;

@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final OrderAdminService orderAdminService;
    private final OrderDocumentPdfService orderDocumentPdfService;

    @GetMapping("/{paymentId}/receipt.pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long paymentId) {
        OrderAdminService.ReceiptDocumentData document = orderAdminService.getReceiptDocument(paymentId);
        byte[] pdf = orderDocumentPdfService.buildReceiptPdf(document.order(), document.payment());
        String filename = document.payment().receiptNumber() + ".pdf";
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
            .body(pdf);
    }
}
