package sn.sopikeur.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class LeadRateLimiter {

    private static final Duration WINDOW = Duration.ofSeconds(30);
    private final Map<String, Instant> lastSeen = new ConcurrentHashMap<>();

    public void assertAllowed(String key) {
        Instant now = Instant.now();
        Instant last = lastSeen.get(key);
        if (last != null && last.plus(WINDOW).isAfter(now)) {
            throw new IllegalArgumentException("Trop de demandes, veuillez réessayer plus tard.");
        }
        lastSeen.put(key, now);
    }
}
