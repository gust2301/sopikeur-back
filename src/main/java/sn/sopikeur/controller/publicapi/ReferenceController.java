package sn.sopikeur.controller.publicapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.sopikeur.common.constants.ApiConstants;

import java.util.List;
import java.util.Map;

/**
 * Provides reference data (enums with labels) for the front and backoffice,
 * so status labels are driven by the backend and never diverge.
 */
@RestController
@RequestMapping(ApiConstants.V1 + "/reference")
public class ReferenceController {

    @GetMapping("/order-statuses")
    public List<Map<String, String>> orderStatuses() {
        return List.of(
            Map.of("value", "SUBMITTED",   "label", "Soumise"),
            Map.of("value", "CONFIRMED",   "label", "Confirmée"),
            Map.of("value", "IN_PROGRESS", "label", "En cours"),
            Map.of("value", "DELIVERED",   "label", "Livrée"),
            Map.of("value", "CANCELED",    "label", "Annulée")
        );
    }

    @GetMapping("/payment-plans")
    public List<Map<String, String>> paymentPlans() {
        return List.of(
            Map.of("value", "CASH_ON_DELIVERY", "label", "Espèces à la livraison",
                   "description", "Aucune avance requise. Vous payez à la réception."),
            Map.of("value", "DEPOSIT_50", "label", "Acompte 50% en ligne",
                   "description", "Réservez avec 50% maintenant, le reste à la livraison."),
            Map.of("value", "FULL_ONLINE", "label", "Paiement intégral en ligne",
                   "description", "Payez 100% immédiatement.")
        );
    }

    @GetMapping("/payment-methods")
    public List<Map<String, String>> paymentMethods() {
        return List.of(
            Map.of("value", "STRIPE", "label", "Carte bancaire (Stripe)", "available", "true"),
            Map.of("value", "WAVE",   "label", "Wave",          "available", "false"),
            Map.of("value", "ORANGE_MONEY", "label", "Orange Money", "available", "false")
        );
    }
}
