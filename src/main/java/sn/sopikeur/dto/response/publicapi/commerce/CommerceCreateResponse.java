package sn.sopikeur.dto.response.publicapi.commerce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommerceCreateResponse {
    private String id;
    private String orderNumber;
    private String status;
    private String paymentPlan;
    private String paymentStatus;
    private String createdAt;
}
