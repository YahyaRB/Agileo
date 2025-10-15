package com.agileo.AGILEO.Dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileGroupResponseDTO {

    private Integer groupId;
    private String storageName;
    private Integer groupState;
    private Integer sysState;

    // Champs système
    private LocalDateTime sysCreationDate;
    private LocalDateTime sysModificationDate;

    // Statistiques du groupe
    private Integer fileCount;
    private Long totalSize;
    private String totalSizeFormatted;

    // Liste des fichiers (optionnel, chargé selon les besoins)
    private List<FileResponseDTO> files;

    // État
    private boolean isActive;
}