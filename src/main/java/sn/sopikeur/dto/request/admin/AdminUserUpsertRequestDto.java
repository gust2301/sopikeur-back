package sn.sopikeur.dto.request.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminUserUpsertRequestDto {
    @NotBlank @Email
    private String email;
    private String fullName;
    @NotBlank
    private String password;
    private Boolean enabled;
}
