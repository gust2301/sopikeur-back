package sn.sopikeur.dto.response.admin;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;
import sn.sopikeur.entity.leads.ContactStatus;

@Data
@Builder
public class ContactMessageResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String message;
    private ContactStatus status;
    private OffsetDateTime createdAt;
}
