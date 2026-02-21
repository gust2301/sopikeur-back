package sn.sopikeur.dto.request.publicapi.commerce;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeliveryDto {
    @NotBlank
    private String city;
    private String area;
    private String address;
    private String notes;
}
