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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminAuthAndProductIT extends BaseMySqlIT {

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
    void login_shouldReturn401WithInvalidPassword() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"superadmin@sopikeur.sn","password":"bad-password"}
                """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void login_preflight_shouldReturnCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/admin/auth/login")
                .header("Origin", "https://develop.sopikeur-backoffice.pages.dev")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "https://develop.sopikeur-backoffice.pages.dev"));
    }

    @Test
    void productsCrud_andPagination_andStockValidation() throws Exception {
        String token = login();

        String created = mockMvc.perform(post("/api/v1/admin/products")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sku":"SPC-ADMIN-001",
                      "slug":"spc-admin-001",
                      "name":"SPC ADMIN 001",
                      "type":"SPC",
                      "status":"ACTIVE",
                      "featured":true,
                      "price":20000.00,
                      "unit":"FCFA / m²",
                      "dimensions":"1220x180x5",
                      "descriptionShort":"short",
                      "descriptionLong":"long"
                    }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNumber())
            .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/api/v1/admin/products/{id}", id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").value("SPC-ADMIN-001"));

        mockMvc.perform(put("/api/v1/admin/products/{id}", id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sku":"SPC-ADMIN-001",
                      "slug":"spc-admin-001",
                      "name":"SPC ADMIN 001 UPDATED",
                      "type":"SPC",
                      "status":"ACTIVE",
                      "featured":false,
                      "price":19000.00,
                      "unit":"FCFA / m²"
                    }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("SPC ADMIN 001 UPDATED"));

        mockMvc.perform(get("/api/v1/admin/products?page=1&size=1&sort=createdAt,desc&q=SPC-ADMIN-001")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.total").isNumber())
            .andExpect(jsonPath("$.items[0].sku").value("SPC-ADMIN-001"));

        mockMvc.perform(put("/api/v1/admin/stocks/{productId}", id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"quantity":5,"reserved":6,"preorderAllowed":false}
                """))
            .andExpect(status().isConflict());
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
