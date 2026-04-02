package sn.sopikeur.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.repo.order.OrderRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderTrackingPublicIT extends BaseMySqlIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void getByPublicId_returnsTrackingPayloadWithoutJwt() throws Exception {
        String payload = """
            {
              "customer": {
                "fullName": "Client Tracking",
                "phone": "+221771234567",
                "email": "tracking@sopikeur.sn"
              },
              "delivery": {
                "city": "Dakar",
                "area": "Almadies",
                "address": "Villa 12"
              },
              "installRequested": true,
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

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode body = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String publicId = body.get("id").asText();

        OrderEntity saved = orderRepository.findByPublicId(publicId).orElseThrow();
        saved.setExpectedDeliveryDate(LocalDate.of(2026, 4, 10));
        saved.setDeliveryNote("Livraison en coordination avec le client");
        saved.setInstallationRequested(true);
        saved.setInstallationNote("A confirmer");
        orderRepository.save(saved);

        mockMvc.perform(get("/api/v1/public/orders/{publicId}", publicId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publicId").value(publicId))
            .andExpect(jsonPath("$.orderRef").isNotEmpty())
            .andExpect(jsonPath("$.status").value("PENDING_CONFIRMATION"))
            .andExpect(jsonPath("$.customerName").value("Client Tracking"))
            .andExpect(jsonPath("$.phone").value("+2*********67"))
            .andExpect(jsonPath("$.delivery.city").value("Dakar"))
            .andExpect(jsonPath("$.delivery.zone").value("Almadies"))
            .andExpect(jsonPath("$.delivery.expectedDate").value("2026-04-10"))
            .andExpect(jsonPath("$.delivery.expectedDeliveryDate").value("2026-04-10"))
            .andExpect(jsonPath("$.delivery.note").value("Livraison en coordination avec le client"))
            .andExpect(jsonPath("$.installation.requested").value(true))
            .andExpect(jsonPath("$.installation.note").value("A confirmer"))
            .andExpect(jsonPath("$.installationRequested").value(true))
            .andExpect(jsonPath("$.installationDateText").value("A confirmer"))
            .andExpect(jsonPath("$.items[0].sku").value("SPC006"))
            .andExpect(jsonPath("$.totals.total").isNumber())
            .andExpect(jsonPath("$.email").doesNotExist())
            .andExpect(jsonPath("$.customerEmail").doesNotExist())
            .andExpect(jsonPath("$.timeline[0].label").value("Commande recue"));

        String response = mockMvc.perform(get("/api/v1/public/orders/{publicId}", publicId))
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(response).doesNotContain("tracking@sopikeur.sn");
    }

    @Test
    void getByPublicId_returns404WhenUnknown() throws Exception {
        mockMvc.perform(get("/api/v1/public/orders/{publicId}", "missing-public-id"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
