package sn.sopikeur.dto.request.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class UpdateOrderDetailsRequest {
    private String fullName;
    private String phone;
    private String email;
    private String cityZone;
    private String deliveryCity;
    private String deliveryZone;
    private String deliveryAddress;
    private String note;
    private LocalDate expectedDeliveryDate;
    private LocalDate deliveryEtaDate;
    private String deliveryNote;
    private Boolean installationRequested;
    private LocalDate installationDate;
    private LocalDate installationEtaDate;
    private String installationNote;
    private BigDecimal installationAmount;
    private String internalNote;
}
