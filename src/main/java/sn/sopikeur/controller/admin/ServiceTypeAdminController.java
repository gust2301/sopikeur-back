package sn.sopikeur.controller.admin;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.dto.request.admin.ServiceTypeUpsertRequest;
import sn.sopikeur.dto.response.admin.ServiceTypeResponseDto;
import sn.sopikeur.service.AdminServiceTypeService;

@RestController
@RequestMapping("/api/v1/admin/service-types")
@RequiredArgsConstructor
public class ServiceTypeAdminController {

    private final AdminServiceTypeService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR','SALES')")
    public List<ServiceTypeResponseDto> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.list(includeInactive);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR')")
    public ServiceTypeResponseDto create(@Valid @RequestBody ServiceTypeUpsertRequest body) {
        return service.create(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR')")
    public ServiceTypeResponseDto update(@PathVariable Long id, @Valid @RequestBody ServiceTypeUpsertRequest body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public void delete(@PathVariable Long id) {
        service.deactivate(id);
    }
}
