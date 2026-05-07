package sn.sopikeur.dto.request.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserUpsertRequestDto {
    @NotBlank @Email
    private String email;
    private String fullName;
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres")
    private String password;
    private Boolean enabled;
}
