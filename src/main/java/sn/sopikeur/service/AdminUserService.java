package sn.sopikeur.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.sopikeur.common.error.NotFoundException;
import sn.sopikeur.dto.request.admin.AdminMeUpdateRequestDto;
import sn.sopikeur.dto.request.admin.AdminUserUpsertRequestDto;
import sn.sopikeur.dto.request.admin.AssignRolesRequestDto;
import sn.sopikeur.dto.response.admin.AdminUserResponseDto;
import sn.sopikeur.entity.auth.AdminUserEntity;
import sn.sopikeur.repo.AdminUserRepository;
import sn.sopikeur.repo.RoleRepository;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final AdminUserRepository adminUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<AdminUserResponseDto> list() {
        return adminUserRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponseDto getById(Long id) {
        return adminUserRepository.findById(id)
            .map(this::toDto)
            .orElseThrow(() -> new NotFoundException("Admin user introuvable"));
    }

    @Transactional(readOnly = true)
    public AdminUserResponseDto getMe(String email) {
        return adminUserRepository.findByEmail(email)
            .map(this::toDto)
            .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
    }

    @Transactional
    public AdminUserResponseDto updateMe(String email, AdminMeUpdateRequestDto dto) {
        AdminUserEntity user = adminUserRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
            if (dto.getCurrentPassword() == null
                    || !passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
                throw new IllegalArgumentException("Mot de passe actuel incorrect");
            }
            user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        }
        return toDto(adminUserRepository.save(user));
    }

    @Transactional
    public AdminUserResponseDto create(AdminUserUpsertRequestDto dto) {
        AdminUserEntity user = new AdminUserEntity();
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setEnabled(dto.getEnabled() == null || dto.getEnabled());
        return toDto(adminUserRepository.save(user));
    }

    @Transactional
    public AdminUserResponseDto update(Long id, AdminUserUpsertRequestDto dto) {
        AdminUserEntity user = adminUserRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Admin user introuvable"));
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setEnabled(dto.getEnabled() == null || dto.getEnabled());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        return toDto(adminUserRepository.save(user));
    }

    @Transactional
    public void delete(Long id) { adminUserRepository.deleteById(id); }

    @Transactional
    public AdminUserResponseDto assignRoles(Long id, AssignRolesRequestDto dto) {
        AdminUserEntity user = adminUserRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Admin user introuvable"));
        user.setRoles(dto.getRoles().stream()
            .map(code -> roleRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Role introuvable: " + code)))
            .collect(Collectors.toSet()));
        return toDto(adminUserRepository.save(user));
    }

    @Transactional(readOnly = true)
    public Set<String> roles() {
        return roleRepository.findAll().stream().map(r -> r.getCode()).collect(Collectors.toSet());
    }

    private AdminUserResponseDto toDto(AdminUserEntity user) {
        return AdminUserResponseDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .enabled(user.isEnabled())
            .roles(user.getRoles().stream().map(r -> r.getCode()).collect(Collectors.toSet()))
            .lastLoginAt(user.getLastLoginAt())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
}
