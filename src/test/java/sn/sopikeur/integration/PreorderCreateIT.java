package sn.sopikeur.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import sn.sopikeur.entity.leads.PreorderRequest;
import sn.sopikeur.repo.PreorderRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PreorderCreateIT extends BaseMySqlIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PreorderRequestRepository preorderRequestRepository;

    @Test
    void createPreorder_requiresAcceptsDelayAndPersistsDeliveryAndInstallationFlag() throws Exception {
        String payload = """
        {
          "contact": {
            "fullName": "Client IT Preorder",
            "phone": "+221770000002",
            "email": "preorder-it@sopikeur.sn"
          },
          "delivery": {
            "city": "Dakar",
            "area": "Ngor"
          },
          "installRequested": true,
          "acceptsDelay": true,
          "message": "Je peux attendre",
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

        MvcResult result = mockMvc.perform(post("/api/v1/preorders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String publicId = body.get("id").asText();

        PreorderRequest saved = preorderRequestRepository.findAll().stream()
                .filter(preorder -> publicId.equals(preorder.getPublicId()))
                .findFirst()
                .orElseThrow();

        assertThat(saved.isAcceptsDelay()).isTrue();
        assertThat(saved.isNeedsInstallation()).isTrue();

        assertThat(saved.getDeliveryJson()).isNotBlank();
        JsonNode delivery = objectMapper.readTree(saved.getDeliveryJson());
        assertThat(delivery.get("city").asText()).isEqualTo("Dakar");
        assertThat(delivery.get("area").asText()).isEqualTo("Ngor");
    }
}
