package sn.sopikeur.dto.request.publicapi.commerce;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class QuoteCreateRequest {
    @NotNull @Valid
    private CustomerPayload customer;
    @NotBlank
    private String projectType;
    @Valid
    private DeliveryDto delivery;
    private String cityZone;
    private Boolean installRequested;
    private String message;
    @NotBlank
    private String intent;
    @NotEmpty @Valid
    private List<CommerceItemCreateRequest> items;
    private List<String> packs;
}
