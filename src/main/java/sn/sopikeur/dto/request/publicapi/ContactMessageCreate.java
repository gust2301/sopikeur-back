package sn.sopikeur.dto.request.publicapi;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContactMessageCreate {
    @NotBlank(message = "Le nom est obligatoire")
    private String fullName;

    @Email(message = "Email invalide")
    @NotBlank(message = "Email obligatoire")
    private String email;

    private String phone;

    private String message;
}
