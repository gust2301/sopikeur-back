package sn.sopikeur.dto.response.admin;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponseDto {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private UserDto user;

    @Data
    @Builder
    public static class UserDto {
        private Long id;
        private String email;
        private String fullName;
        private Set<String> roles;
    }
}
