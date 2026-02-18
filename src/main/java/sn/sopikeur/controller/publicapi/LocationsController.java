package sn.sopikeur.controller.publicapi;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.dto.response.publicapi.locations.CitiesResponse;
import sn.sopikeur.service.LocationService;

@RestController
@RequestMapping(ApiConstants.V1)
@RequiredArgsConstructor
public class LocationsController {
    private final LocationService locationService;

    @GetMapping("/locations/cities")
    public ResponseEntity<CitiesResponse> listCities() {
        return ResponseEntity.ok(new CitiesResponse(locationService.listCities()));
    }
}
