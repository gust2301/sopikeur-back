package sn.sopikeur;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import sn.sopikeur.config.AppProperties;
import sn.sopikeur.config.TrackingProperties;

@SpringBootApplication
@EnableConfigurationProperties({AppProperties.class, TrackingProperties.class})
public class SopikeurBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(SopikeurBackApplication.class, args);
    }

}
