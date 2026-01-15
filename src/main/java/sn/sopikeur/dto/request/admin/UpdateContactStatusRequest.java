package sn.sopikeur.dto.request.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import sn.sopikeur.entity.leads.ContactStatus;

@Data
public class UpdateContactStatusRequest {
    @NotNull(message = "Statut requis")
    private ContactStatus status;
}
