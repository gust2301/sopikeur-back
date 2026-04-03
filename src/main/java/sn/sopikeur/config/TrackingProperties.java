package sn.sopikeur.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tracking")
public record TrackingProperties(String baseUrl) {
}
