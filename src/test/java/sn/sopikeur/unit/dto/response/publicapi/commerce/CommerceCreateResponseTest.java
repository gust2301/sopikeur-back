package sn.sopikeur.unit.dto.response.publicapi.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.sopikeur.dto.request.publicapi.commerce.*;
import sn.sopikeur.dto.response.publicapi.commerce.CommerceCreateResponse;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.entity.leads.QuoteRequest;
import sn.sopikeur.entity.leads.QuoteStatus;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.order.OrderStatus;
import sn.sopikeur.entity.stock.StockItem;
import sn.sopikeur.repo.*;
import sn.sopikeur.repo.order.OrderItemRepository;
import sn.sopikeur.repo.order.OrderRepository;
import sn.sopikeur.service.NotificationService;
import sn.sopikeur.service.commerce.CommerceService;

@ExtendWith(MockitoExtension.class)
class CommerceCreateResponseTest {

    @Mock private QuoteRequestRepository quoteRequestRepository;
    @Mock private PreorderRequestRepository preorderRequestRepository;
    @Mock private QuoteRequestItemRepository quoteRequestItemRepository;
    @Mock private QuoteRequestPackRepository quoteRequestPackRepository;
    @Mock private PreorderRequestItemRepository preorderRequestItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StockItemRepository stockItemRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private NotificationService notificationService;

    private CommerceService commerceService;

    @BeforeEach
    void setUp() {
        commerceService = new CommerceService(
            quoteRequestRepository,
            preorderRequestRepository,
            quoteRequestItemRepository,
            quoteRequestPackRepository,
            preorderRequestItemRepository,
            productRepository,
            stockItemRepository,
            orderRepository,
            orderItemRepository,
            new ObjectMapper(),
            notificationService
        );
    }

    @Test
    void createOrder_responseContainsOrderNumberWhenPresent() {
        Product product = product();
        StockItem stockItem = stockItem();

        when(productRepository.findBySku("SPC006")).thenReturn(Optional.of(product));
        when(stockItemRepository.findByProductId(product.getId())).thenReturn(Optional.of(stockItem));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            order.setPublicId("order-public-id");
            order.setOrderNumber("SK-12345-ABCDE");
            order.setStatus(OrderStatus.PENDING_CONFIRMATION);
            order.setCreatedAt(OffsetDateTime.now());
            return order;
        });

        CommerceCreateResponse response = commerceService.createOrder(orderRequest());

        assertThat(response.getId()).isEqualTo("order-public-id");
        assertThat(response.getOrderNumber()).isEqualTo("SK-12345-ABCDE");
    }

    @Test
    void createQuote_responseHasIdEvenWithoutOrderNumber() {
        Product product = product();

        when(productRepository.findBySku("SPC006")).thenReturn(Optional.of(product));
        when(quoteRequestRepository.save(any(QuoteRequest.class))).thenAnswer(invocation -> {
            QuoteRequest quote = invocation.getArgument(0);
            quote.setPublicId("quote-public-id");
            quote.setStatus(QuoteStatus.NEW);
            quote.setCreatedAt(OffsetDateTime.now());
            return quote;
        });

        CommerceCreateResponse response = commerceService.createQuote(quoteRequest());

        assertThat(response.getId()).isEqualTo("quote-public-id");
        assertThat(response.getOrderNumber()).isNull();
    }

    private Product product() {
        Product product = new Product();
        product.setId(1002L);
        product.setSku("SPC006");
        product.setSlug("spc006");
        product.setType(ProductType.SPC);
        product.setPrice(BigDecimal.valueOf(20000));
        return product;
    }

    private StockItem stockItem() {
        StockItem stockItem = new StockItem();
        stockItem.setQuantity(10);
        stockItem.setReserved(0);
        return stockItem;
    }

    private OrderCreateRequest orderRequest() {
        CustomerPayload customer = new CustomerPayload();
        customer.setFullName("Client Response Test");
        customer.setPhone("+221770000004");
        customer.setEmail("order-response@sopikeur.sn");

        DeliveryDto delivery = new DeliveryDto();
        delivery.setCity("Dakar");

        CommerceItemCreateRequest item = new CommerceItemCreateRequest();
        item.setProductId("1002");
        item.setSku("SPC006");
        item.setQty(1d);
        item.setUnit(ProductUnit.M2);

        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomer(customer);
        request.setDelivery(delivery);
        request.setItems(List.of(item));
        return request;
    }

    private QuoteCreateRequest quoteRequest() {
        CustomerPayload customer = new CustomerPayload();
        customer.setFullName("Client Quote Test");
        customer.setPhone("+221770000005");
        customer.setEmail("quote-response@sopikeur.sn");

        DeliveryDto delivery = new DeliveryDto();
        delivery.setCity("Dakar");

        CommerceItemCreateRequest item = new CommerceItemCreateRequest();
        item.setProductId("1002");
        item.setSku("SPC006");
        item.setQty(1d);
        item.setUnit(ProductUnit.M2);

        QuoteCreateRequest request = new QuoteCreateRequest();
        request.setCustomer(customer);
        request.setProjectType("residential");
        request.setDelivery(delivery);
        request.setIntent("quote");
        request.setItems(List.of(item));
        return request;
    }
}
