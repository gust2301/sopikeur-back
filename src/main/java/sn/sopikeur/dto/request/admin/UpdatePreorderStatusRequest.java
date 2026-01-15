package sn.sopikeur.dto.request.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import sn.sopikeur.entity.leads.PreorderStatus;

@Data
public class UpdatePreorderStatusRequest {
    @NotNull(message = "Statut requis")
    private PreorderStatus status;
}
