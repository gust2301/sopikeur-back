package sn.sopikeur.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import sn.sopikeur.common.constants.ApiConstants;
import sn.sopikeur.service.NotificationService;

import java.util.Map;

@RestController
@RequestMapping(ApiConstants.V1 + "/admin/push")
@RequiredArgsConstructor
public class PushController {

    private final NotificationService notificationService;

    /** Returns the VAPID public key so the Angular client can subscribe. */
    @GetMapping("/vapid-public-key")
    public ResponseEntity<Map<String, String>> getVapidPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", notificationService.getVapidPublicKey()));
    }

    /** Save a new push subscription from the browser PushManager. */
    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(
            @Valid @RequestBody SubscribeRequest body,
            @AuthenticationPrincipal UserDetails user) {
        String email = user != null ? user.getUsername() : null;
        notificationService.subscribe(body.endpoint(), body.p256dh(), body.auth(), email);
        return ResponseEntity.ok().build();
    }

    /** Remove a push subscription (called on logout or manual revocation). */
    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@Valid @RequestBody UnsubscribeRequest body) {
        notificationService.unsubscribe(body.endpoint());
        return ResponseEntity.noContent().build();
    }

    // ─── Request records ─────────────────────────────────────────────────────

    public record SubscribeRequest(
        @NotBlank String endpoint,
        @NotBlank String p256dh,
        @NotBlank String auth
    ) {}

    public record UnsubscribeRequest(
        @NotBlank String endpoint
    ) {}
}
