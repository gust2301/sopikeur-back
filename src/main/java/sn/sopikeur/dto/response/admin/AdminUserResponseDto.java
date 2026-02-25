package sn.sopikeur.dto.response.admin;

import java.time.OffsetDateTime;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserResponseDto {
    private Long id;
    private String email;
    private String fullName;
    private Boolean enabled;
    private Set<String> roles;
    private OffsetDateTime lastLoginAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
