package sn.sopikeur.dto.request.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductAssetAssignmentDto {

    @NotNull
    private Long assetId;

    private boolean cover;

    private Integer sortOrder;
}
