package sn.sopikeur.dto.request.publicapi.commerce;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

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
}
