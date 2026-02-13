package sn.sopikeur.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Antispam antispam = new Antispam();
    private Turnstile turnstile = new Turnstile();

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMinutes;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins;
    }

    @Data
    public static class Antispam {
        private Contact contact = new Contact();

        @Data
        public static class Contact {
            private RateLimit rateLimit = new RateLimit();

            @Data
            public static class RateLimit {
                private int requests = 5;
                private int windowSeconds = 60;
            }
        }
    }

    @Data
    public static class Turnstile {
        private String secret;
    }
}
