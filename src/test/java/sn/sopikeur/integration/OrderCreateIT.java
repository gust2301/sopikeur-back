package sn.sopikeur.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.order.OrderItem;
import sn.sopikeur.repo.order.OrderRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderCreateIT extends BaseMySqlIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void createOrder_persistsDeliveryJsonAndNeedsInstallation() throws Exception {
        String payload = """
            {
              "customer": {
                "fullName": "Client IT Order",
                "phone": "+221771234567",
                "email": "order-it@sopikeur.sn"
              },
              "delivery": {
                "city": "Dakar",
                "area": "Mermoz",
                "address": "Rue 10",
                "notes": "Appeler avant livraison"
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

        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.status").isNotEmpty())
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.orderNumber").isNotEmpty())
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String publicId = body.get("id").asText();

        OrderEntity saved = orderRepository.findAll().stream()
            .filter(order -> publicId.equals(order.getPublicId()))
            .findFirst()
            .orElseThrow();

        JsonNode delivery = objectMapper.readTree(saved.getDeliveryJson());

        assertThat(delivery.get("city").asText()).isEqualTo("Dakar");
        assertThat(delivery.get("area").asText()).isEqualTo("Mermoz");
        assertThat(delivery.get("address").asText()).isEqualTo("Rue 10");
        assertThat(delivery.get("notes").asText()).isEqualTo("Appeler avant livraison");
    }

    @Test
    void createOrder_keepsDecimalQuantityForSpcItems() throws Exception {
        String payload = """
            {
              "customer": {
                "fullName": "Client Decimal",
                "phone": "+221771234567",
                "email": "order-decimal@sopikeur.sn"
              },
              "delivery": {
                "city": "Dakar"
              },
              "installRequested": false,
              "items": [
                {
                  "productId": "1002",
                  "sku": "SPC006",
                  "qty": 17.5,
                  "unit": "M2"
                }
              ]
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String publicId = body.get("id").asText();

        OrderEntity saved = orderRepository.findAll().stream()
            .filter(order -> publicId.equals(order.getPublicId()))
            .findFirst()
            .orElseThrow();

        OrderItem item = orderRepository.findDetailedById(saved.getId())
            .orElseThrow()
            .getItems()
            .stream()
            .findFirst()
            .orElseThrow();
        assertThat(item.getQty()).isEqualByComparingTo("17.50");
        assertThat(item.getLineTotalSnapshot()).isEqualByComparingTo(new BigDecimal("350000.00"));
        assertThat(saved.getAmountTotal()).isEqualByComparingTo(new BigDecimal("350000.00"));
    }
}
