package sn.sopikeur.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    void getTrackingByPublicId_returnsWorkflowPayloadWithoutJwt() throws Exception {
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
        saved.setDeliveryEtaDate(LocalDate.of(2026, 4, 10));
        saved.setDeliveryNote("Livraison en coordination avec le client");
        saved.setInstallationRequested(true);
        saved.setInstallationEtaDate(LocalDate.of(2026, 4, 12));
        saved.setInstallationNote("Equipe a confirmer");
        saved.setDeliveredAt(LocalDateTime.of(2026, 4, 9, 11, 30));
        saved.setInstalledAt(LocalDateTime.of(2026, 4, 12, 16, 45));
        orderRepository.save(saved);

        mockMvc.perform(get("/api/v1/public/orders/{publicId}/tracking", publicId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publicId").value(publicId))
            .andExpect(jsonPath("$.reference").isNotEmpty())
            .andExpect(jsonPath("$.status").value("INSTALLED"))
            .andExpect(jsonPath("$.installationRequested").value(true))
            .andExpect(jsonPath("$.deliveryEtaDate").value("2026-04-10"))
            .andExpect(jsonPath("$.deliveredAt").value("2026-04-09T11:30:00"))
            .andExpect(jsonPath("$.installationEtaDate").value("2026-04-12"))
            .andExpect(jsonPath("$.installedAt").value("2026-04-12T16:45:00"))
            .andExpect(jsonPath("$.summary.customerName").value("Client Tracking"))
            .andExpect(jsonPath("$.summary.phone").value("+2*********67"))
            .andExpect(jsonPath("$.summary.city").value("Dakar"))
            .andExpect(jsonPath("$.summary.zone").value("Almadies"))
            .andExpect(jsonPath("$.payment.total").isNumber())
            .andExpect(jsonPath("$.items[0].sku").value("SPC006"))
            .andExpect(jsonPath("$.timelineSteps[3].label").value("Livree"))
            .andExpect(jsonPath("$.timelineSteps[3].state").value("DONE"))
            .andExpect(jsonPath("$.timelineSteps[5].label").value("Installation terminee"))
            .andExpect(jsonPath("$.timelineSteps[5].state").value("DONE"))
            .andExpect(jsonPath("$.internalNote").doesNotExist())
            .andExpect(jsonPath("$.summary.email").doesNotExist());

        String response = mockMvc.perform(get("/api/v1/public/orders/{publicId}/tracking", publicId))
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(response).doesNotContain("tracking@sopikeur.sn");
    }

    @Test
    void getTrackingByPublicId_returns404WhenUnknown() throws Exception {
        mockMvc.perform(get("/api/v1/public/orders/{publicId}/tracking", "missing-public-id"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
