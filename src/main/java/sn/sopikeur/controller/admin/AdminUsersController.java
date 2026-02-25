package sn.sopikeur.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.sopikeur.dto.request.admin.AdminUserUpsertRequestDto;
import sn.sopikeur.dto.request.admin.AssignRolesRequestDto;
import sn.sopikeur.dto.response.admin.AdminUserResponseDto;
import sn.sopikeur.service.AdminUserService;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUsersController {

    private final AdminUserService service;

    @GetMapping("/users")
    public java.util.List<AdminUserResponseDto> listUsers() { return service.list(); }

    @GetMapping("/users/{id}")
    public AdminUserResponseDto getUser(@PathVariable Long id) { return service.getById(id); }

    @PostMapping("/users")
    public AdminUserResponseDto create(@Valid @RequestBody AdminUserUpsertRequestDto dto) { return service.create(dto); }

    @PutMapping("/users/{id}")
    public AdminUserResponseDto update(@PathVariable Long id, @Valid @RequestBody AdminUserUpsertRequestDto dto) { return service.update(id, dto); }

    @DeleteMapping("/users/{id}")
    public void delete(@PathVariable Long id) { service.delete(id); }

    @PutMapping("/users/{id}/roles")
    public AdminUserResponseDto assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRolesRequestDto dto) {
        return service.assignRoles(id, dto);
    }

    @GetMapping("/roles")
    public java.util.Set<String> listRoles() { return service.roles(); }
}
