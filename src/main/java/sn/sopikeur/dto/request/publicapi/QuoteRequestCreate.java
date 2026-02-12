package sn.sopikeur.dto.request.publicapi;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuoteRequestCreate {
    @NotBlank(message = "Le nom est obligatoire")
    private String fullName;

    @Email(message = "Email invalide")
    @NotBlank(message = "Email obligatoire")
    private String email;

    @Pattern(regexp = "^$|^[0-9+().\\s-]{6,20}$", message = "Téléphone invalide")
    private String phone;

    @Size(min = 10, message = "Le message doit contenir au moins 10 caractères")
    private String message;

    private String website;
}
