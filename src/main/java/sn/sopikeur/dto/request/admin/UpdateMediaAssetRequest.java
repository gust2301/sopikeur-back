package sn.sopikeur.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMediaAssetRequest {

    @NotBlank
    private String path;

    private String alt;
}
