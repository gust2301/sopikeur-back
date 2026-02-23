package sn.sopikeur.dto.request.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminMeUpdateRequestDto {
    @NotBlank
    @Email
    private String email;
    private String fullName;
    private String currentPassword;
    private String newPassword;
}
