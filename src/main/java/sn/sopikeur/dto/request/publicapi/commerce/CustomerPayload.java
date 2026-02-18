package sn.sopikeur.dto.request.publicapi.commerce;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerPayload {
    @NotBlank
    private String fullName;
    @NotBlank
    private String phone;
    @Email
    private String email;
}
