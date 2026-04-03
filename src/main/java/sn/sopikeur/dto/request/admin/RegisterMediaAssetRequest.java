package sn.sopikeur.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterMediaAssetRequest {

    /** Chemin relatif dans R2, ex: accessories/mon-produit.jpg */
    @NotBlank
    private String path;

    /** Texte alternatif (optionnel) */
    private String alt;
}
