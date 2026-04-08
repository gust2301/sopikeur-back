package sn.sopikeur.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.sopikeur.common.error.NotFoundException;
import sn.sopikeur.dto.request.admin.OrderExpenseUpsertRequest;
import sn.sopikeur.dto.response.admin.OrderExpenseAdminResponseDto;
import sn.sopikeur.dto.response.admin.OrderExpenseListAdminResponseDto;
import sn.sopikeur.entity.auth.AdminUserEntity;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.order.OrderExpenseEntity;
import sn.sopikeur.entity.order.OrderExpenseKind;
import sn.sopikeur.repo.AdminUserRepository;
import sn.sopikeur.repo.order.OrderExpenseRepository;
import sn.sopikeur.repo.order.OrderRepository;

@Service
@RequiredArgsConstructor
public class OrderExpenseAdminService {

    private final OrderRepository orderRepository;
    private final OrderExpenseRepository orderExpenseRepository;
    private final AdminUserRepository adminUserRepository;

    @Transactional(readOnly = true)
    public OrderExpenseListAdminResponseDto list(Long orderId) {
        ensureOrderExists(orderId);
        List<OrderExpenseAdminResponseDto> items = orderExpenseRepository.findByOrderIdOrderByExpenseDateDescIdDesc(orderId)
            .stream()
            .map(this::toDto)
            .toList();
        return OrderExpenseListAdminResponseDto.builder()
            .items(items)
            .actualCosts(sumCosts(orderId, OrderExpenseKind.ACTUAL))
            .forecastCosts(sumCosts(orderId, OrderExpenseKind.FORECAST))
            .build();
    }

    @Transactional
    public OrderExpenseAdminResponseDto create(Long orderId, OrderExpenseUpsertRequest request) {
        OrderEntity order = ensureOrderExists(orderId);
        OrderExpenseEntity entity = new OrderExpenseEntity();
        entity.setOrder(order);
        apply(entity, request);
        entity.setCreatedBy(resolveActor());
        entity.setCreatedByAdminUserId(resolveActorAdminUserId());
        return toDto(orderExpenseRepository.save(entity));
    }

    @Transactional
    public OrderExpenseAdminResponseDto update(Long orderId, Long expenseId, OrderExpenseUpsertRequest request) {
        ensureOrderExists(orderId);
        OrderExpenseEntity entity = orderExpenseRepository.findByIdAndOrderId(expenseId, orderId)
            .orElseThrow(() -> new NotFoundException("Depense introuvable"));
        apply(entity, request);
        return toDto(orderExpenseRepository.save(entity));
    }

    @Transactional
    public void delete(Long orderId, Long expenseId) {
        ensureOrderExists(orderId);
        OrderExpenseEntity entity = orderExpenseRepository.findByIdAndOrderId(expenseId, orderId)
            .orElseThrow(() -> new NotFoundException("Depense introuvable"));
        orderExpenseRepository.delete(entity);
    }

    private OrderEntity ensureOrderExists(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Commande introuvable"));
    }

    private BigDecimal sumCosts(Long orderId, OrderExpenseKind kind) {
        BigDecimal amount = orderExpenseRepository.sumAmountsByOrderIdAndKind(orderId, kind);
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private void apply(OrderExpenseEntity entity, OrderExpenseUpsertRequest request) {
        entity.setAmount(request.getAmount());
        entity.setCategory(request.getCategory().trim());
        entity.setKind(request.getKind());
        entity.setExpenseDate(request.getDate() != null ? request.getDate() : LocalDate.now());
        entity.setNote(trimToNull(request.getNote()));
    }

    private OrderExpenseAdminResponseDto toDto(OrderExpenseEntity entity) {
        return OrderExpenseAdminResponseDto.builder()
            .id(entity.getId())
            .amount(entity.getAmount())
            .category(entity.getCategory())
            .kind(entity.getKind() != null ? entity.getKind().name() : null)
            .date(entity.getExpenseDate())
            .note(entity.getNote())
            .createdBy(entity.getCreatedBy())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }
        return authentication.getName();
    }

    private Long resolveActorAdminUserId() {
        String actor = resolveActor();
        if (actor == null) {
            return null;
        }
        return adminUserRepository.findByEmail(actor)
            .map(AdminUserEntity::getId)
            .orElse(null);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
