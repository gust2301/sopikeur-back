package sn.sopikeur.dto.request.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import sn.sopikeur.entity.catalog.ProductStatus;
import sn.sopikeur.entity.catalog.ProductType;

@Data
public class ProductUpsertRequestDto {
    @NotBlank
    private String sku;
    @NotBlank
    private String slug;
    @NotBlank
    private String name;
    @NotNull
    private ProductType type;
    @NotNull
    private ProductStatus status;
    @NotNull
    private Boolean featured;
    private Boolean discountService;
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal price;
    private Boolean promoActive;
    @DecimalMin("0.0")
    private BigDecimal promoPrice;
    private LocalDate promoStartDate;
    private LocalDate promoEndDate;
    private String promoLabel;
    private String unit;
    private String dimensions;
    private String descriptionShort;
    private String descriptionLong;
}
