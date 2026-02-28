package sn.sopikeur.dto.request.publicapi.commerce;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import sn.sopikeur.entity.order.OrderPaymentPlan;

@Data
public class OrderCreateRequest {
    @NotNull @Valid
    private CustomerPayload customer;
    @Valid
    private DeliveryDto delivery;
    private String cityZone;
    private Boolean installRequested;
    @NotEmpty @Valid
    private List<CommerceItemCreateRequest> items;

    /** Payment plan chosen by the customer. Defaults to CASH_ON_DELIVERY if null. */
    private OrderPaymentPlan paymentPlan;

    /** Payment method chosen. Defaults to "STRIPE" if null. */
    private String paymentMethodSelected;
}
