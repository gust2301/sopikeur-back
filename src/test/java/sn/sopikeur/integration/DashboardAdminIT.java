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
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardAdminIT extends BaseMySqlIT {

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
    void dashboardRevenue_shouldOnlyCountPaidAmounts() throws Exception {
        Long firstOrderId = createOrderAndReturnId("Client Revenue 1", "revenue1@sopikeur.sn");
        Long secondOrderId = createOrderAndReturnId("Client Revenue 2", "revenue2@sopikeur.sn");

        jdbcTemplate.update(
            "UPDATE orders SET amount_total = 603500.00, amount_paid = 200000.00, amount_due = 403500.00, payment_status = 'PARTIALLY_PAID' WHERE id = ?",
            firstOrderId
        );
        jdbcTemplate.update(
            "UPDATE orders SET amount_total = 1000000.00, amount_paid = 0.00, amount_due = 1000000.00, payment_status = 'UNPAID' WHERE id = ?",
            secondOrderId
        );

        mockMvc.perform(get("/api/v1/admin/dashboard/stats")
                .header("Authorization", "Bearer " + login()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRevenue").value(200000.00));
    }

    @Test
    void recentOrders_shouldOnlyIncludeActiveOrders() throws Exception {
        Long pendingOrderId = createOrderAndReturnId("Client Pending", "pending@sopikeur.sn");
        Long confirmedOrderId = createOrderAndReturnId("Client Confirmed", "confirmed@sopikeur.sn");
        Long cancelledOrderId = createOrderAndReturnId("Client Cancelled", "cancelled@sopikeur.sn");

        jdbcTemplate.update("UPDATE orders SET status = 'PENDING_CONFIRMATION' WHERE id = ?", pendingOrderId);
        jdbcTemplate.update("UPDATE orders SET status = 'CONFIRMED' WHERE id = ?", confirmedOrderId);
        jdbcTemplate.update("UPDATE orders SET status = 'CANCELLED' WHERE id = ?", cancelledOrderId);

        mockMvc.perform(get("/api/v1/admin/dashboard/stats")
                .header("Authorization", "Bearer " + login()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recentOrders[*].status").value(everyItem(anyOf(
                is("PENDING_CONFIRMATION"),
                is("CONFIRMED")
            ))))
            .andExpect(jsonPath("$.recentOrders[*].customerEmail").value(not(hasItem("cancelled@sopikeur.sn"))));
    }

    private Long createOrderAndReturnId(String fullName, String email) throws Exception {
        String created = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customer": {
                        "fullName": "%s",
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
                """.formatted(fullName, email)))
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
