package sn.sopikeur.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.sopikeur.common.error.NotFoundException;
import sn.sopikeur.dto.response.publicapi.order.OrderTrackingResponseDto;
import sn.sopikeur.dto.response.publicapi.order.PublicOrderTrackingResponseDto;
import sn.sopikeur.mapper.OrderTrackingMapper;
import sn.sopikeur.repo.order.OrderRepository;

@Service
@RequiredArgsConstructor
public class OrderTrackingService {

    private final OrderRepository orderRepository;
    private final OrderTrackingMapper orderTrackingMapper;

    @Transactional(readOnly = true)
    public OrderTrackingResponseDto getByPublicId(String publicId) {
        return orderRepository.findByPublicId(publicId)
            .map(orderTrackingMapper::toDto)
            .orElseThrow(() -> new NotFoundException("Commande introuvable"));
    }

    @Transactional(readOnly = true)
    public PublicOrderTrackingResponseDto getTrackingByPublicId(String publicId) {
        return orderRepository.findByPublicId(publicId)
            .map(orderTrackingMapper::toTrackingDto)
            .orElseThrow(() -> new NotFoundException("Commande introuvable"));
    }
}
