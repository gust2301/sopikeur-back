package sn.sopikeur.dto.response.admin;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import sn.sopikeur.entity.leads.QuoteStatus;

@Data
@Builder
public class QuoteRequestResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String message;
    private String projectType;
    private String cityZone;
    private String deliveryJson;
    private boolean needsInstallation;
    private QuoteStatus status;
    private OffsetDateTime createdAt;
    private List<ItemDto> items;
    private List<PackDto> packs;

    @Data
    @Builder
    public static class ItemDto {
        private Long productId;
        private String productName;
        private String productSlug;
        private String sku;
        private BigDecimal quantity;
        private String unit;
    }

    @Data
    @Builder
    public static class PackDto {
        private String code;
        private String label;
    }
}
