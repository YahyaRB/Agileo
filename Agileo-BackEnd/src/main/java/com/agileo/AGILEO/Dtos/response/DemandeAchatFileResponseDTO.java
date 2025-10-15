package com.agileo.AGILEO.Dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandeAchatFileResponseDTO {

    // Info fichier principal
    private Integer fileId;
    private String name;
    private String extension;
    private String fullFileName;
    private Integer size;
    private String sizeFormatted;
    private String alt;

    // Info upload/création
    private LocalDateTime uploadDate;
    private String uploadedBy;
    private String uploadedByNom;

    // Info téléchargement
    private String downloadUrl;
    private Integer nbOpen;

    // Permissions utilisateur
    private boolean canDelete;
    private boolean canDownload;

    // Classification
    private String category;
    private String documentType;
}