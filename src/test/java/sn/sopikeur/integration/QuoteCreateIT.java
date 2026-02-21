package sn.sopikeur.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import sn.sopikeur.entity.leads.QuoteRequest;
import sn.sopikeur.repo.QuoteRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuoteCreateIT extends BaseMySqlIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuoteRequestRepository quoteRequestRepository;

    @Test
    void createQuote_persistsDeliveryJsonAndNeedsInstallationFromInstallRequested() throws Exception {
        String payload = """
        {
          "customer": {
            "fullName": "Client IT Quote",
            "phone": "+221770000001",
            "email": "quote-it@sopikeur.sn"
          },
          "projectType": "residential",
          "delivery": {
            "city": "Dakar",
            "area": "Almadies"
          },
          "installRequested": true,
          "message": "Merci de me rappeler",
          "intent": "quote",
          "items": [
            {
              "productId": "1002",
              "sku": "SPC006",
              "qty": 2,
              "unit": "M2"
            }
          ]
        }
        """;

        MvcResult result = mockMvc.perform(post("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String publicId = body.get("id").asText();

        QuoteRequest saved = quoteRequestRepository.findAll().stream()
                .filter(quote -> publicId.equals(quote.getPublicId()))
                .findFirst()
                .orElseThrow();

        assertThat(saved.isNeedsInstallation()).isTrue();

        assertThat(saved.getDeliveryJson()).isNotBlank();
        JsonNode delivery = objectMapper.readTree(saved.getDeliveryJson());
        assertThat(delivery.get("city").asText()).isEqualTo("Dakar");
        assertThat(delivery.get("area").asText()).isEqualTo("Almadies");
    }
}
