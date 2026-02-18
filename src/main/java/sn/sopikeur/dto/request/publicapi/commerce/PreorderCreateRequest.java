package sn.sopikeur.dto.request.publicapi.commerce;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class PreorderCreateRequest {
    @NotNull @Valid
    private CustomerPayload contact;
    @Valid
    private DeliveryDto delivery;
    private String cityZone;
    private Boolean installRequested;
    @NotNull
    private Boolean acceptsDelay;
    private String message;
    @NotEmpty @Valid
    private List<CommerceItemCreateRequest> items;
}
