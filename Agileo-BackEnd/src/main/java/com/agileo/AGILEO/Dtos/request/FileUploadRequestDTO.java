package com.agileo.AGILEO.Dtos.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRequestDTO {

    @NotNull(message = "L'ID de la demande d'achat est obligatoire")
    private Integer demandeAchatId;

    private String description;

    private String category; // Type de document (devis, facture, bon de commande, etc.)

    // Constructeur simplifié
    public FileUploadRequestDTO(Integer demandeAchatId) {
        this.demandeAchatId = demandeAchatId;
    }
}