
package sn.sopikeur.dto.request.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MarkOrderDeliveredRequest {
    private LocalDateTime deliveredAt;
    private String note;
}
