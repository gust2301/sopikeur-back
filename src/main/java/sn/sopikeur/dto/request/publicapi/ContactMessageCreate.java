package sn.sopikeur.dto.request.publicapi;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContactMessageCreate {
    @NotNull(message = "Customer type is required")
    @Pattern(regexp = "^(Particulier|Professionnel)$", message = "Customer type is invalid")
    private String customerType;

    @NotBlank(message = "Name is required")
    @Size(min = 2, message = "Name must contain at least 2 characters")
    private String name;

    @NotBlank(message = "Phone is required")
    @Size(min = 6, max = 20, message = "Phone is invalid")
    @Pattern(regexp = "^[0-9+().\\s-]{6,20}$", message = "Phone is invalid")
    private String phone;

    @Pattern(regexp = "^$|^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Email is invalid")
    private String email;

    @Size(max = 2000, message = "Message must contain at most 2000 characters")
    private String message;

    private String website;

    private String turnstileToken;
}
