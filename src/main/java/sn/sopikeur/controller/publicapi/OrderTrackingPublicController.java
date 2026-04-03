package sn.sopikeur.controller.publicapi;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.dto.response.publicapi.order.OrderTrackingResponseDto;
import sn.sopikeur.dto.response.publicapi.order.PublicOrderTrackingResponseDto;
import sn.sopikeur.service.OrderTrackingService;

@RestController
@RequestMapping(ApiConstants.V1 + "/public/orders")
@RequiredArgsConstructor
public class OrderTrackingPublicController {

    private final OrderTrackingService orderTrackingService;

    @GetMapping("/{publicId}")
    public OrderTrackingResponseDto getByPublicId(@PathVariable String publicId) {
        return orderTrackingService.getByPublicId(publicId);
    }

    @GetMapping("/{publicId}/tracking")
    public PublicOrderTrackingResponseDto getTracking(@PathVariable String publicId) {
        return orderTrackingService.getTrackingByPublicId(publicId);
    }
}
