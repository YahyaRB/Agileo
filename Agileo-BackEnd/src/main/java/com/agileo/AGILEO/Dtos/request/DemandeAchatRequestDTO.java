package com.agileo.AGILEO.Dtos.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DemandeAchatRequestDTO {

    private Integer id;

    @NotBlank(message = "Le chantier est obligatoire")
    private String chantier;

    @NotNull(message = "Le délai souhaité est obligatoire")
    private LocalDateTime delaiSouhaite;

    private String commentaire;

    @NotNull(message = "Le login utilisateur est obligatoire")
    private Integer login;

    private Integer statut;
    private String numDa;
    private String dsDivalto;
    private Integer pjDa;
    private LocalDateTime dateDa;

    // lignes de demande
    private List<LigneDemandeAchatRequestDTO> lignesDemande;
}
