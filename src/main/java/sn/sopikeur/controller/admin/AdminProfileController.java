package sn.sopikeur.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sn.sopikeur.dto.request.admin.AdminMeUpdateRequestDto;
import sn.sopikeur.dto.response.admin.AdminUserResponseDto;
import sn.sopikeur.service.AdminUserService;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminProfileController {

    private final AdminUserService service;

    @GetMapping("/me")
    public AdminUserResponseDto getMe(Authentication auth) {
        return service.getMe(auth.getName());
    }

    @PutMapping("/me")
    public AdminUserResponseDto updateMe(Authentication auth,
                                          @Valid @RequestBody AdminMeUpdateRequestDto dto) {
        return service.updateMe(auth.getName(), dto);
    }
}
