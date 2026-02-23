package sn.sopikeur.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.dto.response.admin.DashboardStatsDto;
import sn.sopikeur.service.DashboardAdminService;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardAdminService dashboardAdminService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR','SALES')")
    public DashboardStatsDto getStats() {
        return dashboardAdminService.getStats();
    }
}
