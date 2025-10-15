package com.agileo.AGILEO.Dtos.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConsommationResponseDTO {
    private Integer id;
    private Integer affaireId;
    private Integer userId;
    private LocalDateTime dateConsommation;
    private String commentaire;
    private String refInterne;
    private String statut;
    private String affaireCode;
    private String userLogin;
    private String createdBy;
    private LocalDateTime createdDate;

    // Informations enrichies
    private String affaireLibelle;
    private String createurNom;
}
