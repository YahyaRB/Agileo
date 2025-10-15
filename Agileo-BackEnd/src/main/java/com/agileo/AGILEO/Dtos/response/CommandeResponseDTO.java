package com.agileo.AGILEO.Dtos.response;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CommandeResponseDTO {
    private Long commande;
    private String ce;
    private String fournisseurId;
    private String fournisseur;
    private String affaireCode;
    private String affaireName;
    private LocalDate dateCommande;
    private String votreReference;
    private String piece;
    private String referenceBC; // Pour l'affichage dans le frontend

    /**
     * Méthode pour initialiser referenceBC automatiquement
     */
    public void setReferenceBC() {
        if (this.votreReference != null && !this.votreReference.trim().isEmpty()) {
            this.referenceBC = this.votreReference;
        } else if (this.commande != null) {
            this.referenceBC = "CMD-" + this.commande;
        } else {
            this.referenceBC = "N/A";
        }
    }
}