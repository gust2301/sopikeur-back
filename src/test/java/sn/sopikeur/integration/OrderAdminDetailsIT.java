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
    void updateDetails_shouldPersistCustomerDeliveryAndInstallationFields() throws Exception {
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
        Long orderId = jdbcTemplate.queryForObject("SELECT id FROM orders WHERE public_id = ?", Long.class, publicId);
        String token = login();

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/details", orderId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName":"Client Modifie",
                      "phone":"+221780000000",
                      "email":"updated@sopikeur.sn",
                      "deliveryCity":"Saly",
                      "deliveryZone":"Niakhniakhal",
                      "deliveryAddress":"Residence 4",
                      "expectedDeliveryDate":"2026-04-12",
                      "deliveryNote":"Appeler 30 min avant",
                      "installationRequested":true,
                      "installationDate":"2026-04-15",
                      "installationNote":"Equipe a confirmer",
                      "note":"Commande VIP"
                    }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customer.fullName").value("Client Modifie"))
            .andExpect(jsonPath("$.customer.phone").value("+221780000000"))
            .andExpect(jsonPath("$.customer.email").value("updated@sopikeur.sn"))
            .andExpect(jsonPath("$.delivery.city").value("Saly"))
            .andExpect(jsonPath("$.delivery.zone").value("Niakhniakhal"))
            .andExpect(jsonPath("$.delivery.address").value("Residence 4"))
            .andExpect(jsonPath("$.delivery.cityZone").value("Saly - Niakhniakhal"))
            .andExpect(jsonPath("$.delivery.expectedDeliveryDate").value("2026-04-12"))
            .andExpect(jsonPath("$.delivery.deliveryNote").value("Appeler 30 min avant"))
            .andExpect(jsonPath("$.delivery.installationRequested").value(true))
            .andExpect(jsonPath("$.delivery.installationDate").value("2026-04-15"))
            .andExpect(jsonPath("$.delivery.installationNote").value("Equipe a confirmer"))
            .andExpect(jsonPath("$.delivery.note").value("Commande VIP"));
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
