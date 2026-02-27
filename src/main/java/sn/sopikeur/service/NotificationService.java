package sn.sopikeur.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sn.sopikeur.entity.leads.ContactMessage;
import sn.sopikeur.entity.leads.PreorderRequest;
import sn.sopikeur.entity.leads.QuoteRequest;
import sn.sopikeur.entity.order.OrderEntity;
import sn.sopikeur.entity.push.PushSubscription;
import sn.sopikeur.repo.push.PushSubscriptionRepository;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.push.public-key:}")
    private String vapidPublicKey;

    @Value("${app.push.private-key:}")
    private String vapidPrivateKey;

    @Value("${app.push.subject:mailto:admin@sopikeur.sn}")
    private String vapidSubject;

    // ─── Public hooks ─────────────────────────────────────────────────────────

    public void notifyOrderCreated(OrderEntity order) {
        sendPush("Nouvelle commande \uD83D\uDED2", order.getFullName(), "https://admin.sopikeur.sn/orders");
    }

    public void notifyQuoteCreated(QuoteRequest quote) {
        sendPush("Nouveau devis \uD83D\uDCCB", quote.getFullName(), "https://admin.sopikeur.sn/leads/quotes");
    }

    public void notifyContactCreated(ContactMessage contact) {
        sendPush("Nouveau contact \uD83D\uDCE9", contact.getFullName(), "https://admin.sopikeur.sn/leads/contacts");
    }

    public void notifyPreorderCreated(PreorderRequest preorder) {
        sendPush("Nouvelle pr\u00E9commande \u23F3", preorder.getFullName(), "https://admin.sopikeur.sn/leads/preorders");
    }

    // ─── Subscription management ──────────────────────────────────────────────

    @Transactional
    public void subscribe(String endpoint, String p256dh, String auth, String adminEmail) {
        PushSubscription sub = pushSubscriptionRepository.findByEndpoint(endpoint)
            .orElseGet(PushSubscription::new);
        sub.setEndpoint(endpoint);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        sub.setAdminEmail(adminEmail);
        pushSubscriptionRepository.save(sub);
        log.info("Push subscription saved for admin={}", adminEmail);
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        pushSubscriptionRepository.deleteByEndpoint(endpoint);
    }

    // ─── Internal push dispatch ───────────────────────────────────────────────

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void sendPush(String title, String body, String url) {
        if (vapidPublicKey == null || vapidPublicKey.isBlank()
                || vapidPrivateKey == null || vapidPrivateKey.isBlank()) {
            log.warn("VAPID keys not configured — skipping push notification");
            return;
        }

        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAll();
        if (subscriptions.isEmpty()) {
            return;
        }

        String payload;
        try {
            // NGSW lit notification.data.url pour la navigation au clic.
            // Le champ "url" doit donc être dans l'objet "data" imbriqué.
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("title", title);
            data.put("body", body);
            data.put("data", Map.of("url", url));
            payload = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Failed to serialize push payload", e);
            return;
        }

        PushService pushService;
        try {
            pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
        } catch (Exception e) {
            log.error("Failed to initialize PushService with VAPID keys", e);
            return;
        }

        for (PushSubscription sub : subscriptions) {
            try {
                Subscription webSub = new Subscription(
                    sub.getEndpoint(),
                    new Subscription.Keys(sub.getP256dh(), sub.getAuth())
                );
                Notification notification = new Notification(webSub, payload);
                pushService.send(notification);
            } catch (Exception e) {
                log.warn("Failed to send push to subscription id={}: {}", sub.getId(), e.getMessage());
            }
        }
    }

    public String getVapidPublicKey() {
        return vapidPublicKey;
    }
}
