package sn.sopikeur.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sn.sopikeur.entity.catalog.Product;

@Service
@RequiredArgsConstructor
public class ProductPricingService {

    private final Clock clock;

    public boolean isPromotionActive(Product product) {
        if (product == null || !product.isPromoActive() || product.getPromoPrice() == null || product.getPrice() == null) {
            return false;
        }
        if (product.getPromoPrice().compareTo(product.getPrice()) >= 0) {
            return false;
        }
        LocalDate today = LocalDate.now(clock);
        if (product.getPromoStartDate() != null && product.getPromoStartDate().isAfter(today)) {
            return false;
        }
        if (product.getPromoEndDate() != null && product.getPromoEndDate().isBefore(today)) {
            return false;
        }
        return true;
    }

    public BigDecimal resolveEffectivePrice(Product product) {
        if (product == null) {
            return BigDecimal.ZERO;
        }
        return isPromotionActive(product) ? product.getPromoPrice() : product.getPrice();
    }

    public Integer resolveDiscountPercent(Product product) {
        if (!isPromotionActive(product) || product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal savedAmount = product.getPrice().subtract(product.getPromoPrice());
        return savedAmount
            .multiply(BigDecimal.valueOf(100))
            .divide(product.getPrice(), 0, java.math.RoundingMode.HALF_UP)
            .intValue();
    }
}
