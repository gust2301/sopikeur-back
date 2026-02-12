package sn.sopikeur.dto.response.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String tokenType;
    private long expiresInMinutes;
}
