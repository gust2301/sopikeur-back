package sn.sopikeur.dto.request.admin;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import lombok.Data;

@Data
public class AssignRolesRequestDto {
    @NotEmpty
    private Set<String> roles;
}
