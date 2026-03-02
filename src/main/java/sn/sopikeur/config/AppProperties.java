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
    private Stripe stripe = new Stripe();
    private Wave wave = new Wave();
    private OrangeMoney orangeMoney = new OrangeMoney();

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

    @Data
    public static class Stripe {
        private String secretKey;
        private String webhookSecret;
        private String successUrl;
        private String cancelUrl;
        private String currency = "xof";
        private long webhookToleranceSeconds = 300;
    }

    @Data
    public static class Wave {
        private String apiKey        = "placeholder";
        private String webhookSecret = "placeholder";
        private String successUrl;
        private String cancelUrl;
    }

    @Data
    public static class OrangeMoney {
        private String clientId     = "placeholder";
        private String clientSecret = "placeholder";
        private String merchantKey  = "placeholder";
        private String country      = "SN";
        private String returnUrl;
        private String cancelUrl;
        private String notifUrl;
    }
}
