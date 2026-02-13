package sn.sopikeur.security;

import lombok.Data;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import sn.sopikeur.config.AppProperties;

@Component
public class TurnstileVerifier {

    private static final String TURNSTILE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final AppProperties appProperties;
    private final RestClient restClient;

    public TurnstileVerifier(AppProperties appProperties, RestClientBuilderConfigurer restClientBuilderConfigurer) {
        this.appProperties = appProperties;
        this.restClient = restClientBuilderConfigurer.configure(RestClient.builder()).build();
    }

    public boolean isEnabled() {
        String secret = appProperties.getTurnstile().getSecret();
        return secret != null && !secret.isBlank();
    }

    public boolean verify(String token, String remoteIp) {
        if (!isEnabled()) {
            return true;
        }

        if (token == null || token.isBlank()) {
            return false;
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("secret", appProperties.getTurnstile().getSecret());
        formData.add("response", token);
        if (remoteIp != null && !remoteIp.isBlank()) {
            formData.add("remoteip", remoteIp);
        }

        TurnstileResponse response = restClient.post()
            .uri(TURNSTILE_VERIFY_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .retrieve()
            .body(TurnstileResponse.class);

        return response != null && Boolean.TRUE.equals(response.getSuccess());
    }

    @Data
    public static class TurnstileResponse {
        private Boolean success;
        private String challengeTs;
        private String hostname;
        private String action;
        private String cdata;
        private String[] errorCodes;

        @com.fasterxml.jackson.annotation.JsonProperty("error-codes")
        public void setErrorCodes(String[] errorCodes) {
            this.errorCodes = errorCodes;
        }
    }
}
