package sn.sopikeur.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderAdminDocumentsIT extends BaseMySqlIT {

    private static final String ADMIN_EMAIL = "superadmin@sopikeur.sn";
    private static final String ADMIN_PASSWORD = "password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void ensureAdminCredentials() {
        String encodedPassword = passwordEncoder.encode(ADMIN_PASSWORD);

        int updated = jdbcTemplate.update(
            "UPDATE admin_users SET password_hash = ?, enabled = TRUE, updated_at = NOW() WHERE email = ?",
            encodedPassword,
            ADMIN_EMAIL
        );

        if (updated == 0) {
            jdbcTemplate.update(
                """
                INSERT INTO admin_users(username, email, password_hash, role, enabled, created_at, updated_at)
                VALUES (?, ?, ?, ?, TRUE, NOW(), NOW())
                """,
                "superadmin",
                ADMIN_EMAIL,
                encodedPassword,
                "SUPER_ADMIN"
            );
        }

        jdbcTemplate.update("INSERT IGNORE INTO roles(code) VALUES ('SUPER_ADMIN')");
        jdbcTemplate.update(
            """
            INSERT INTO admin_user_roles(admin_user_id, role_id)
            SELECT au.id, r.id
            FROM admin_users au
            JOIN roles r ON r.code = 'SUPER_ADMIN'
            WHERE au.email = ?
              AND NOT EXISTS (
                  SELECT 1 FROM admin_user_roles aur WHERE aur.admin_user_id = au.id AND aur.role_id = r.id
              )
            """,
            ADMIN_EMAIL
        );
    }

    @Test
    void issueInvoice_shouldGenerateUniqueInvoiceNumber_andInvoicePdfShouldReturnPdf() throws Exception {
        Long orderId = createOrderAndReturnId("invoice1@sopikeur.sn");
        String token = login();

        mockMvc.perform(post("/api/v1/admin/orders/{id}/issue-invoice", orderId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.invoiceNumber").value(org.hamcrest.Matchers.startsWith("INV-")));

        mockMvc.perform(get("/api/v1/admin/orders/{id}/invoice.pdf", orderId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void recordPayment_shouldGenerateUniqueReceiptNumber_andReceiptPdfShouldReturnPdf() throws Exception {
        Long orderId = createOrderAndReturnId("receipt1@sopikeur.sn");
        jdbcTemplate.update(
            "UPDATE orders SET amount_total = 603500.00, amount_paid = 0.00, amount_due = 603500.00, payment_status = 'UNPAID' WHERE id = ?",
            orderId
        );

        String token = login();
        String response = mockMvc.perform(patch("/api/v1/admin/orders/{id}/payment", orderId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"amountPaid":50000}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.payments[0].receiptNumber").value(org.hamcrest.Matchers.startsWith("RCPT-")))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long paymentId = objectMapper.readTree(response).get("payments").get(0).get("id").asLong();

        mockMvc.perform(get("/api/v1/admin/payments/{paymentId}/receipt.pdf", paymentId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void documentNumbers_shouldBeUniqueAcrossOrdersAndPayments() throws Exception {
        String token = login();
        Long firstOrderId = createOrderAndReturnId("unique1@sopikeur.sn");
        Long secondOrderId = createOrderAndReturnId("unique2@sopikeur.sn");

        mockMvc.perform(post("/api/v1/admin/orders/{id}/issue-invoice", firstOrderId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/orders/{id}/issue-invoice", secondOrderId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        String firstInvoice = jdbcTemplate.queryForObject("SELECT invoice_number FROM orders WHERE id = ?", String.class, firstOrderId);
        String secondInvoice = jdbcTemplate.queryForObject("SELECT invoice_number FROM orders WHERE id = ?", String.class, secondOrderId);
        org.junit.jupiter.api.Assertions.assertNotEquals(firstInvoice, secondInvoice);

        jdbcTemplate.update("UPDATE orders SET amount_total = 603500.00, amount_paid = 0.00, amount_due = 603500.00 WHERE id IN (?, ?)", firstOrderId, secondOrderId);

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/payment", firstOrderId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"amountPaid":10000}
                """))
            .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/admin/orders/{id}/payment", secondOrderId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"amountPaid":20000}
                """))
            .andExpect(status().isOk());

        String firstReceipt = jdbcTemplate.queryForObject("SELECT receipt_number FROM order_payments WHERE order_id = ? ORDER BY id DESC LIMIT 1", String.class, firstOrderId);
        String secondReceipt = jdbcTemplate.queryForObject("SELECT receipt_number FROM order_payments WHERE order_id = ? ORDER BY id DESC LIMIT 1", String.class, secondOrderId);
        org.junit.jupiter.api.Assertions.assertNotEquals(firstReceipt, secondReceipt);
    }

    private Long createOrderAndReturnId(String email) throws Exception {
        String created = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customer": {
                        "fullName": "Client Documents",
                        "phone": "+221771234567",
                        "email": "%s"
                      },
                      "delivery": {
                        "city": "Dakar",
                        "area": "Mermoz",
                        "address": "Rue 1"
                      },
                      "installRequested": false,
                      "items": [
                        {
                          "productId": "1002",
                          "sku": "SPC006",
                          "qty": 1,
                          "unit": "M2"
                        }
                      ]
                    }
                """.formatted(email)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode createdNode = objectMapper.readTree(created);
        String publicId = createdNode.get("id").asText();
        return jdbcTemplate.queryForObject("SELECT id FROM orders WHERE public_id = ?", Long.class, publicId);
    }

    private String login() throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"superadmin@sopikeur.sn","password":"password"}
                """))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("accessToken").asText();
    }
}
