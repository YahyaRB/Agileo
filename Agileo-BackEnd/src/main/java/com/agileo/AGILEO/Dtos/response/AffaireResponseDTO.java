package com.agileo.AGILEO.Dtos.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AffaireResponseDTO {
    private Integer numero;
    private Integer accessoirId;
    private String affaire;
    private String libelle; // depuis la vue AffaireLieUser
    private String commentaire;
    private LocalDateTime sysCreationDate;
    private Integer sysCreatorId;
    private LocalDateTime sysModificationDate;
    private Integer sysUserId;
    private LocalDateTime sysSynchronizationDate;
    private Integer sysState;

    // Informations utilisateur (depuis KdnsAccessor)
    private String creatorFullName;
    private String userFullName;
}
