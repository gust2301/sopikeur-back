package sn.sopikeur.dto.response.admin;

import java.time.OffsetDateTime;
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
    private QuoteStatus status;
    private OffsetDateTime createdAt;
}
