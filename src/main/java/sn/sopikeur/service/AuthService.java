package sn.sopikeur.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import sn.sopikeur.dto.request.admin.LoginRequest;
import sn.sopikeur.dto.response.admin.LoginResponse;
import sn.sopikeur.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        String token = jwtService.generateToken(authentication.getName());
        return LoginResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .expiresInMinutes(jwtService.getExpirationMinutes())
            .build();
    }
}
