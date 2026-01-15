package sn.sopikeur.dto.request.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import sn.sopikeur.entity.leads.QuoteStatus;

@Data
public class UpdateQuoteStatusRequest {
    @NotNull(message = "Statut requis")
    private QuoteStatus status;
}
