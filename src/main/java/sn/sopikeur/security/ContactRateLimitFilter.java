package sn.sopikeur.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import sn.sopikeur.common.error.TooManyRequestsException;
import sn.sopikeur.config.AppProperties;

@Component
@RequiredArgsConstructor
public class ContactRateLimitFilter extends OncePerRequestFilter {

    private final AppProperties appProperties;
    private final ClientIpResolver clientIpResolver;
    private final Clock clock;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || !"/api/v1/contact".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        long nowMillis = clock.millis();
        long windowMillis = appProperties.getAntispam().getContact().getRateLimit().getWindowSeconds() * 1000L;
        int maxRequests = appProperties.getAntispam().getContact().getRateLimit().getRequests();
        String clientIp = clientIpResolver.resolve(request);
        String key = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp;

        Bucket bucket = buckets.compute(key, (ip, current) -> {
            if (current == null || (nowMillis - current.windowStartMillis()) >= windowMillis) {
                return new Bucket(nowMillis, 1);
            }
            return new Bucket(current.windowStartMillis(), current.count() + 1);
        });

        buckets.entrySet().removeIf(entry -> (nowMillis - entry.getValue().windowStartMillis()) >= windowMillis * 2);

        if (bucket.count() > maxRequests) {
            throw new TooManyRequestsException("Trop de requêtes, veuillez réessayer plus tard.");
        }

        filterChain.doFilter(request, response);
    }

    private record Bucket(long windowStartMillis, int count) {
    }
}
