package sn.sopikeur.unit.service.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.sopikeur.dto.request.publicapi.commerce.*;
import sn.sopikeur.entity.catalog.Product;
import sn.sopikeur.entity.catalog.ProductType;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.order.OrderStatus;
import sn.sopikeur.entity.stock.StockItem;
import sn.sopikeur.repo.*;
import sn.sopikeur.repo.order.OrderItemRepository;
import sn.sopikeur.repo.order.OrderRepository;
import sn.sopikeur.service.NotificationService;
import sn.sopikeur.service.ProductPricingService;
import sn.sopikeur.service.commerce.CommerceService;

@ExtendWith(MockitoExtension.class)
class OrderMappingTest {

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
    @Mock private ProductPricingService productPricingService;

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
            notificationService,
            productPricingService
        );
    }

    @Test
    void installRequestedTrue_mapsToNeedsInstallationTrue() {
        stubOrderDependencies();

        commerceService.createOrder(orderRequest(Boolean.TRUE));

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().isNeedsInstallation()).isTrue();
    }

    @Test
    void installRequestedNull_mapsToNeedsInstallationFalse() {
        stubOrderDependencies();

        commerceService.createOrder(orderRequest(null));

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().isNeedsInstallation()).isFalse();
    }

    private void stubOrderDependencies() {
        Product product = product();
        StockItem stockItem = stockItem();

        when(productRepository.findBySku("SPC006")).thenReturn(Optional.of(product));
        when(stockItemRepository.findByProductId(product.getId())).thenReturn(Optional.of(stockItem));
        when(productPricingService.resolveEffectivePrice(product)).thenReturn(product.getPrice());
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            order.setStatus(OrderStatus.PENDING_CONFIRMATION);
            order.setPublicId("public-id");
            order.setCreatedAt(OffsetDateTime.now());
            return order;
        });
    }

    private Product product() {
        Product product = new Product();
        product.setId(1002L);
        product.setSku("SPC006");
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

    private OrderCreateRequest orderRequest(Boolean installRequested) {
        CustomerPayload customer = new CustomerPayload();
        customer.setFullName("Client Unit Test");
        customer.setPhone("+221770000003");
        customer.setEmail("unit-order@sopikeur.sn");

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
        request.setInstallRequested(installRequested);
        request.setItems(List.of(item));
        return request;
    }
}
