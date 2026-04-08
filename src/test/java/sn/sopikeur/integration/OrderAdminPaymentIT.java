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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderAdminPaymentIT extends BaseMySqlIT {

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
    void recordPayment_shouldAddDeltaToExistingPaidAmount() throws Exception {
        Long orderId = createOrderAndReturnId();
        jdbcTemplate.update(
            "UPDATE orders SET amount_total = 603500.00, amount_paid = 200000.00, amount_due = 403500.00, payment_status = 'PARTIALLY_PAID' WHERE id = ?",
            orderId
        );

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/payment", orderId)
                .header("Authorization", "Bearer " + login())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"amountPaid":50000}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amountPaid").value(250000.00))
            .andExpect(jsonPath("$.amountDue").value(353500.00))
            .andExpect(jsonPath("$.payments[0].receiptNumber").value(org.hamcrest.Matchers.startsWith("RCPT-")))
            .andExpect(jsonPath("$.payments[0].amount").value(50000.00));
    }

    @Test
    void recordPayment_shouldRejectZeroAmount() throws Exception {
        Long orderId = createOrderAndReturnId();
        jdbcTemplate.update(
            "UPDATE orders SET amount_total = 603500.00, amount_paid = 200000.00, amount_due = 403500.00 WHERE id = ?",
            orderId
        );

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/payment", orderId)
                .header("Authorization", "Bearer " + login())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"amountPaid":0}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Le montant du paiement doit etre strictement positif."));
    }

    @Test
    void recordPayment_shouldRejectAmountAboveDue() throws Exception {
        Long orderId = createOrderAndReturnId();
        jdbcTemplate.update(
            "UPDATE orders SET amount_total = 603500.00, amount_paid = 200000.00, amount_due = 403500.00 WHERE id = ?",
            orderId
        );

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/payment", orderId)
                .header("Authorization", "Bearer " + login())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"amountPaid":500000}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Le montant saisi depasse le restant du de la commande."));
    }

    @Test
    void recordPayment_shouldAccumulateAcrossMultipleCalls() throws Exception {
        Long orderId = createOrderAndReturnId();
        jdbcTemplate.update(
            "UPDATE orders SET amount_total = 603500.00, amount_paid = 200000.00, amount_due = 403500.00, payment_status = 'PARTIALLY_PAID' WHERE id = ?",
            orderId
        );
        String token = login();

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/payment", orderId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"amountPaid":50000}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amountPaid").value(250000.00))
            .andExpect(jsonPath("$.amountDue").value(353500.00));

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/payment", orderId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"amountPaid":100000}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amountPaid").value(350000.00))
            .andExpect(jsonPath("$.amountDue").value(253500.00));

        mockMvc.perform(get("/api/v1/admin/orders/{id}/payments", orderId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].receiptNumber").value(org.hamcrest.Matchers.startsWith("RCPT-")))
            .andExpect(jsonPath("$[1].receiptNumber").value(org.hamcrest.Matchers.startsWith("RCPT-")))
            .andExpect(jsonPath("$[0].paidTotal").value(350000.00))
            .andExpect(jsonPath("$[0].dueTotal").value(253500.00));
    }

    @Test
    void recordPayment_shouldUseTotalIncludingInstallationAmount() throws Exception {
        Long orderId = createOrderAndReturnId();
        jdbcTemplate.update(
            "UPDATE orders SET installation_requested = TRUE, needs_installation = TRUE, installation_amount = 50000.00, amount_total = 1050000.00, amount_paid = 1000000.00, amount_due = 50000.00, payment_status = 'PARTIALLY_PAID' WHERE id = ?",
            orderId
        );

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/payment", orderId)
                .header("Authorization", "Bearer " + login())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"amountPaid":50000}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalAmount").value(1050000.00))
            .andExpect(jsonPath("$.amountPaid").value(1050000.00))
            .andExpect(jsonPath("$.amountDue").value(0.00))
            .andExpect(jsonPath("$.installationAmount").value(50000.00));
    }

    private Long createOrderAndReturnId() throws Exception {
        String created = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customer": {
                        "fullName": "Client Paiement",
                        "phone": "+221771234567",
                        "email": "payment@sopikeur.sn"
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
                """))
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
