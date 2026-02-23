package sn.sopikeur.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.sopikeur.dto.request.admin.LoginRequestDto;
import sn.sopikeur.dto.response.admin.LoginResponseDto;
import sn.sopikeur.repo.AdminUserRepository;
import sn.sopikeur.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AdminUserRepository adminUserRepository;

    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        String token = jwtService.generateToken(authentication.getName());

        var user = adminUserRepository.findByEmail(authentication.getName()).orElseThrow();
        user.setLastLoginAt(java.time.OffsetDateTime.now());

        return LoginResponseDto.builder()
            .accessToken(token)
            .tokenType("Bearer")
            .expiresIn(jwtService.getExpirationMinutes() * 60)
            .user(LoginResponseDto.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream().map(r -> r.getCode()).collect(java.util.stream.Collectors.toSet()))
                .build())
            .build();
    }
}
