package sn.sopikeur.dto.response.publicapi;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContactApiResponse {
    private String message;
    private String requestId;
}
