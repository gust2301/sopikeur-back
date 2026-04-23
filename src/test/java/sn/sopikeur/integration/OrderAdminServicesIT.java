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

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderAdminServicesIT extends BaseMySqlIT {

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
    void serviceTypeCrud_shouldCreateUpdateListAndSoftDelete() throws Exception {
        String token = login();

        String created = mockMvc.perform(post("/api/v1/admin/service-types")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code":"INSTALLATION",
                      "name":"Pose standard",
                      "unit":"forfait",
                      "defaultPrice":50000,
                      "active":true
                    }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("INSTALLATION"))
            .andExpect(jsonPath("$.name").value("Pose standard"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long serviceTypeId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(put("/api/v1/admin/service-types/{id}", serviceTypeId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code":"INSTALLATION",
                      "name":"Pose premium",
                      "unit":"forfait",
                      "defaultPrice":65000,
                      "active":true
                    }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Pose premium"))
            .andExpect(jsonPath("$.defaultPrice").value(65000.00));

        mockMvc.perform(get("/api/v1/admin/service-types")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].code").exists());

        mockMvc.perform(delete("/api/v1/admin/service-types/{id}", serviceTypeId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/service-types")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == %s)]".formatted(serviceTypeId)).doesNotExist());

        mockMvc.perform(get("/api/v1/admin/service-types?includeInactive=true")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == %s)].active".formatted(serviceTypeId)).value(false));
    }

    @Test
    void addService_shouldAppendServiceLineAndIncreaseTotals() throws Exception {
        String token = login();
        Long orderId = createOrderAndReturnId("service-order@sopikeur.sn");
        Long serviceTypeId = createServiceType(token, "INSTALLATION", "Pose standard", 50000);

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/delivery", orderId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "installationRequested":true,
                      "installationEtaDate":"2026-04-15"
                    }
                """))
            .andExpect(status().isOk());

        BigDecimal itemsTotal = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(line_total_snapshot), 0) FROM order_items WHERE order_id = ?",
            BigDecimal.class,
            orderId
        );

        mockMvc.perform(post("/api/v1/admin/orders/{id}/services", orderId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "serviceTypeId": %s,
                      "quantity": 1,
                      "unitPrice": 50000,
                      "note": "Equipe matin"
                    }
                """.formatted(serviceTypeId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderLines[?(@.lineType == 'SERVICE')].serviceCode").value("INSTALLATION"))
            .andExpect(jsonPath("$.orderLines[?(@.lineType == 'SERVICE')].note").value("Equipe matin"))
            .andExpect(jsonPath("$.installationAmount").value(50000.00))
            .andExpect(jsonPath("$.totalAmount").value(itemsTotal.add(BigDecimal.valueOf(50000)).doubleValue()));
    }

    @Test
    void addService_shouldRejectInstallationWhenNotRequested() throws Exception {
        String token = login();
        Long orderId = createOrderAndReturnId("service-blocked@sopikeur.sn");
        Long serviceTypeId = createServiceType(token, "INSTALLATION", "Pose standard", 50000);

        mockMvc.perform(post("/api/v1/admin/orders/{id}/services", orderId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "serviceTypeId": %s,
                      "quantity": 1,
                      "unitPrice": 50000
                    }
                """.formatted(serviceTypeId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Impossible d'ajouter un service d'installation si la pose n'est pas demandee."));
    }

    @Test
    void updateItem_shouldRecalculateOrderTotals() throws Exception {
        String token = login();
        Long orderId = createOrderAndReturnId("line-update@sopikeur.sn");
        Long itemId = jdbcTemplate.queryForObject(
            "SELECT id FROM order_items WHERE order_id = ? ORDER BY id ASC LIMIT 1",
            Long.class,
            orderId
        );

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/items/{itemId}", orderId, itemId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "quantity": 17.92,
                      "unitPrice": 19900,
                      "note": "Ajuste depuis backoffice"
                    }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderLines[0].quantity").value(17.92))
            .andExpect(jsonPath("$.orderLines[0].lineTotal").value(356608.00))
            .andExpect(jsonPath("$.orderLines[0].note").value("Ajuste depuis backoffice"))
            .andExpect(jsonPath("$.totalAmount").value(356608.00));
    }

    @Test
    void deleteItem_shouldRemoveLineAndRecalculateOrderTotals() throws Exception {
        String token = login();
        Long orderId = createOrderAndReturnId("line-delete@sopikeur.sn");
        Long itemId = jdbcTemplate.queryForObject(
            "SELECT id FROM order_items WHERE order_id = ? ORDER BY id ASC LIMIT 1",
            Long.class,
            orderId
        );

        mockMvc.perform(delete("/api/v1/admin/orders/{id}/items/{itemId}", orderId, itemId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/orders/{id}", orderId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderLines").isEmpty())
            .andExpect(jsonPath("$.totalAmount").value(0.0));
    }

    private Long createServiceType(String token, String code, String name, int defaultPrice) throws Exception {
        Long existingId = jdbcTemplate.query(
            "SELECT id FROM service_types WHERE code = ? LIMIT 1",
            rs -> rs.next() ? rs.getLong(1) : null,
            code
        );

        String payload = """
            {
              "code":"%s",
              "name":"%s",
              "unit":"forfait",
              "defaultPrice":%s,
              "active":true
            }
        """.formatted(code, name, defaultPrice);

        if (existingId != null) {
            mockMvc.perform(put("/api/v1/admin/service-types/{id}", existingId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                .andExpect(status().isOk());
            return existingId;
        }

        String created = mockMvc.perform(post("/api/v1/admin/service-types")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(created).get("id").asLong();
    }

    private Long createOrderAndReturnId(String email) throws Exception {
        String created = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customer": {
                        "fullName": "Client Services",
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
