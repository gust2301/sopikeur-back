package sn.sopikeur.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import sn.sopikeur.dto.response.admin.OrderPaymentAdminResponseDto;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.order.OrderItem;

@Service
@RequiredArgsConstructor
public class OrderDocumentPdfService {

    private static final String LOGO_URL = "https://assets.sopikeur.sn/logos/sopiker-logo-dark-h32.png";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] buildInvoicePdf(OrderEntity order, List<OrderPaymentAdminResponseDto> payments, BigDecimal paidTotal, BigDecimal dueTotal) {
        String template = loadTemplate("templates/invoice.html");
        String html = template
            .replace("{{logoUrl}}", escape(LOGO_URL))
            .replace("{{invoiceNumber}}", escape(order.getInvoiceNumber()))
            .replace("{{invoiceIssuedAt}}", escape(formatDateTime(order.getInvoiceIssuedAt() != null ? order.getInvoiceIssuedAt().atOffset(OffsetDateTime.now().getOffset()) : null)))
            .replace("{{orderReference}}", escape(resolveReference(order)))
            .replace("{{customerName}}", escape(order.getFullName()))
            .replace("{{customerPhone}}", escape(order.getPhone()))
            .replace("{{customerEmail}}", escape(order.getEmail()))
            .replace("{{cityZone}}", escape(order.getCityZone()))
            .replace("{{deliveryDate}}", escape(order.getDeliveryEtaDate() != null ? order.getDeliveryEtaDate().format(DATE_FORMAT) : "A confirmer"))
            .replace("{{installation}}", escape(Boolean.TRUE.equals(order.getInstallationRequested()) || order.isNeedsInstallation() ? "Oui" : "Non"))
            .replace("{{itemsRows}}", buildInvoiceRows(order.getItems()))
            .replace("{{paymentsRows}}", buildPaymentsRows(payments))
            .replace("{{totalAmount}}", money(order.getAmountTotal()))
            .replace("{{paidAmount}}", money(paidTotal))
            .replace("{{dueAmount}}", money(dueTotal))
            .replace("{{installationAmount}}", money(order.getInstallationAmount()))
            .replace("{{generatedAt}}", escape(formatDateTime(OffsetDateTime.now())));

        return render(html);
    }

    public byte[] buildReceiptPdf(OrderEntity order, OrderPaymentAdminResponseDto payment) {
        String template = loadTemplate("templates/receipt.html");
        String html = template
            .replace("{{receiptNumber}}", escape(payment.receiptNumber()))
            .replace("{{paymentDate}}", escape(formatDateTime(payment.createdAt())))
            .replace("{{orderReference}}", escape(resolveReference(order)))
            .replace("{{customerName}}", escape(order.getFullName()))
            .replace("{{paymentMethod}}", escape(displayMethod(payment.method())))
            .replace("{{paymentAmount}}", money(payment.amount()))
            .replace("{{paidTotal}}", money(payment.paidTotal()))
            .replace("{{dueTotal}}", money(payment.dueTotal()))
            .replace("{{paymentNote}}", escape(blankFallback(payment.note(), "Aucune")))
            .replace("{{generatedAt}}", escape(formatDateTime(OffsetDateTime.now())));

        return render(html);
    }

    private byte[] render(String html) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de generer le PDF", e);
        }
    }

    private String loadTemplate(String path) {
        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de charger le template PDF " + path, e);
        }
    }

    private String buildInvoiceRows(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "<tr><td colspan=\"5\">Aucune ligne de commande</td></tr>";
        }

        StringBuilder rows = new StringBuilder();
        for (OrderItem item : items) {
            rows.append("<tr>")
                .append("<td>").append(escape(resolveItemName(item))).append("</td>")
                .append("<td>").append(escape(item.getSkuSnapshot())).append("</td>")
                .append("<td>").append(escape(item.getUnit())).append("</td>")
                .append("<td class=\"text-right\">").append(item.getQty()).append("</td>")
                .append("<td class=\"text-right\">").append(money(item.getLineTotalSnapshot())).append("</td>")
                .append("</tr>");
        }
        return rows.toString();
    }

    private String buildPaymentsRows(List<OrderPaymentAdminResponseDto> payments) {
        if (payments == null || payments.isEmpty()) {
            return "<tr><td colspan=\"4\">Aucun paiement enregistre</td></tr>";
        }

        StringBuilder rows = new StringBuilder();
        for (OrderPaymentAdminResponseDto payment : payments) {
            rows.append("<tr>")
                .append("<td>").append(escape(formatDateTime(payment.createdAt()))).append("</td>")
                .append("<td>").append(escape(blankFallback(payment.receiptNumber(), "N/A"))).append("</td>")
                .append("<td>").append(escape(displayMethod(payment.method()))).append("</td>")
                .append("<td class=\"text-right\">").append(money(payment.amount())).append("</td>")
                .append("</tr>");
        }
        return rows.toString();
    }

    private String displayMethod(String method) {
        if (method == null || method.isBlank() || "NONE".equalsIgnoreCase(method)) {
            return "N/A";
        }
        return method;
    }

    private String resolveReference(OrderEntity order) {
        return blankFallback(order.getOrderNumber(), order.getPublicId());
    }

    private String resolveItemName(OrderItem item) {
        if (item.getDisplayName() != null && !item.getDisplayName().isBlank()) {
            return item.getDisplayName();
        }
        if (item.getProduct() != null && item.getProduct().getName() != null && !item.getProduct().getName().isBlank()) {
            return item.getProduct().getName();
        }
        if (item.getServiceType() != null && item.getServiceType().getName() != null && !item.getServiceType().getName().isBlank()) {
            return item.getServiceType().getName();
        }
        return item.getSkuSnapshot();
    }

    private String blankFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String formatDateTime(OffsetDateTime value) {
        return value == null ? "N/A" : DATE_TIME_FORMAT.format(value);
    }

    private String money(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).stripTrailingZeros().toPlainString() + " FCFA";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
