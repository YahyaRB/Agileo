package com.agileo.AGILEO.Dtos.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
@Data
public class LigneDemandeAchatRequestDTO {

    @NotBlank(message = "La référence est obligatoire")
    private String ref;

    private String designation;

    @NotNull(message = "La quantité est obligatoire")
    @Positive(message = "La quantité doit être positive")
    private BigDecimal qte;

    @NotBlank(message = "L'unité est obligatoire")
    private String unite;

    private String fam0001;
    private String fam0002;
    private String fam0003;
    private String sref1;
    private String sref2;

    @NotNull(message = "La demande d'achat associée est obligatoire")
    private Integer da;
}
