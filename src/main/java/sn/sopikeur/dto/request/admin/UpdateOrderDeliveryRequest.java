package sn.sopikeur.dto.request.admin;

import java.time.LocalDate;
import lombok.Data;

@Data
public class UpdateOrderDeliveryRequest {
    private String cityZone;
    private String deliveryCity;
    private String deliveryZone;
    private String deliveryAddress;
    private LocalDate deliveryEtaDate;
    private String deliveryNote;
    private Boolean installationRequested;
    private LocalDate installationEtaDate;
    private String installationNote;
    private String internalNote;
}
