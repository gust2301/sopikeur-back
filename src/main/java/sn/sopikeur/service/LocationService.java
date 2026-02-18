package sn.sopikeur.service;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LocationService {
    private static final List<String> CITIES = List.of(
        "Dakar", "Saly", "Mbour", "Somone", "Ngaparou", "Thiès", "Rufisque",
        "Diamniadio", "Bargny", "Saint-Louis", "Kaolack", "Ziguinchor", "Autre"
    );

    public List<String> listCities() {
        return CITIES;
    }
}
