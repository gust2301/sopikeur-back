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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderAdminDetailsIT extends BaseMySqlIT {

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
    void deliveryWorkflowEndpoints_shouldPersistEtaAndCompletionFields() throws Exception {
        Long orderId = createOrderAndReturnId();
        String token = login();

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/delivery", orderId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "deliveryCity":"Saly",
                      "deliveryZone":"Niakhniakhal",
                      "deliveryAddress":"Residence 4",
                      "deliveryEtaDate":"2026-04-12",
                      "deliveryNote":"Appeler 30 min avant",
                      "installationRequested":true,
                      "installationEtaDate":"2026-04-15",
                      "installationNote":"Equipe a confirmer",
                      "installationAmount":50000,
                      "internalNote":"Commande VIP"
                    }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.delivery.city").value("Saly"))
            .andExpect(jsonPath("$.delivery.zone").value("Niakhniakhal"))
            .andExpect(jsonPath("$.delivery.address").value("Residence 4"))
            .andExpect(jsonPath("$.delivery.deliveryEtaDate").value("2026-04-12"))
            .andExpect(jsonPath("$.delivery.installationEtaDate").value("2026-04-15"))
            .andExpect(jsonPath("$.delivery.installationAmount").value(50000.00))
            .andExpect(jsonPath("$.totalAmount").value(70000.00))
            .andExpect(jsonPath("$.amountDue").value(70000.00))
            .andExpect(jsonPath("$.delivery.internalNote").value("Commande VIP"))
            .andExpect(jsonPath("$.trackingUrl").value("http://localhost:4200/suivi/" + jdbcTemplate.queryForObject("SELECT public_id FROM orders WHERE id = ?", String.class, orderId)));

        mockMvc.perform(post("/api/v1/admin/orders/{id}/mark-delivered", orderId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "deliveredAt":"2026-04-11T10:15:00",
                      "note":"Livre plus tot"
                    }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.delivery.deliveredAt").value("2026-04-11T10:15:00"))
            .andExpect(jsonPath("$.delivery.deliveryNote").value("Livre plus tot"));

        mockMvc.perform(post("/api/v1/admin/orders/{id}/mark-installed", orderId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "installedAt":"2026-04-13T15:45:00",
                      "note":"Pose terminee"
                    }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.delivery.installedAt").value("2026-04-13T15:45:00"))
            .andExpect(jsonPath("$.delivery.installationNote").value("Pose terminee"));
    }

    private Long createOrderAndReturnId() throws Exception {
        String createPayload = """
            {
              "customer": {
                "fullName": "Client Initial",
                "phone": "+221771234567",
                "email": "initial@sopikeur.sn"
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
            """;

        String created = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
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
