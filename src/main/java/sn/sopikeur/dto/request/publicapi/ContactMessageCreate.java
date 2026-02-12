package sn.sopikeur.dto.request.publicapi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContactMessageCreate {
    private String customerType;

    @NotBlank(message = "Name is required")
    @Size(min = 2, message = "Name must contain at least 2 characters")
    private String name;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9+().\\s-]{8,20}$", message = "Phone is invalid")
    private String phone;

    @Pattern(regexp = "^$|^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Email is invalid")
    private String email;

    private String message;

    private String website;
}
