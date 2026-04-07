package sn.sopikeur.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.sopikeur.common.error.NotFoundException;
import sn.sopikeur.dto.request.admin.ServiceTypeUpsertRequest;
import sn.sopikeur.dto.response.admin.ServiceTypeResponseDto;
import sn.sopikeur.entity.catalog.ServiceTypeEntity;
import sn.sopikeur.repo.ServiceTypeRepository;

@Service
@RequiredArgsConstructor
public class AdminServiceTypeService {

    private final ServiceTypeRepository serviceTypeRepository;

    @Transactional(readOnly = true)
    public List<ServiceTypeResponseDto> list(boolean includeInactive) {
        return (includeInactive ? serviceTypeRepository.findAllByOrderByNameAsc() : serviceTypeRepository.findByActiveTrueOrderByNameAsc())
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public ServiceTypeResponseDto create(ServiceTypeUpsertRequest request) {
        serviceTypeRepository.findByCodeIgnoreCase(request.getCode().trim())
            .ifPresent(existing -> {
                throw new IllegalArgumentException("Un type de service avec ce code existe deja.");
            });

        ServiceTypeEntity entity = new ServiceTypeEntity();
        apply(entity, request);
        return toDto(serviceTypeRepository.save(entity));
    }

    @Transactional
    public ServiceTypeResponseDto update(Long id, ServiceTypeUpsertRequest request) {
        ServiceTypeEntity entity = serviceTypeRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Type de service introuvable"));

        serviceTypeRepository.findByCodeIgnoreCase(request.getCode().trim())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new IllegalArgumentException("Un type de service avec ce code existe deja.");
            });

        apply(entity, request);
        return toDto(serviceTypeRepository.save(entity));
    }

    @Transactional
    public void deactivate(Long id) {
        ServiceTypeEntity entity = serviceTypeRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Type de service introuvable"));
        entity.setActive(false);
        serviceTypeRepository.save(entity);
    }

    private void apply(ServiceTypeEntity entity, ServiceTypeUpsertRequest request) {
        entity.setCode(request.getCode().trim().toUpperCase());
        entity.setName(request.getName().trim());
        entity.setUnit(request.getUnit() != null && !request.getUnit().isBlank() ? request.getUnit().trim() : null);
        entity.setDefaultPrice(request.getDefaultPrice());
        entity.setActive(request.getActive() == null || request.getActive());
    }

    private ServiceTypeResponseDto toDto(ServiceTypeEntity entity) {
        return ServiceTypeResponseDto.builder()
            .id(entity.getId())
            .code(entity.getCode())
            .name(entity.getName())
            .unit(entity.getUnit())
            .defaultPrice(entity.getDefaultPrice())
            .active(entity.isActive())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
