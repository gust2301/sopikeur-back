package sn.sopikeur.dto.response.admin;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;
import sn.sopikeur.entity.leads.PreorderStatus;

@Data
@Builder
public class PreorderRequestResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String productSlug;
    private Integer quantity;
    private String message;
    private PreorderStatus status;
    private OffsetDateTime createdAt;
}
