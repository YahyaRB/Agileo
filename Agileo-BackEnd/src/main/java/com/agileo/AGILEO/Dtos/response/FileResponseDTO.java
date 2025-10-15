package com.agileo.AGILEO.Dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileResponseDTO {

    private Integer fileId;
    private Integer groupId;
    private String name;
    private String extension;
    private String fullFileName;
    private String alt;
    private Integer size;
    private String sizeFormatted;
    private Integer nbOpen;
    private String storageName;
    private String hash;
    private String hashType;
    private String tempPath;

    // Champs système
    private LocalDateTime sysCreationDate;
    private Integer sysCreatorId;
    private LocalDateTime sysModificationDate;
    private Integer sysUserId;
    private Integer sysState;

    // Informations utilisateur
    private String createurNom;
    private String createurLogin;

    // Utilitaires
    private String downloadUrl;
    private boolean canDelete;
    private boolean canDownload;
}
