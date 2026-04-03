package sn.sopikeur.dto.request.admin;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MarkOrderInstalledRequest {
    private LocalDateTime installedAt;
    private String note;
}
