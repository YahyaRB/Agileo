package com.agileo.AGILEO.Dtos.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReceptionResponseDTO {
    private Integer id;
    private Integer affaireId;
    private Integer userId;
    private LocalDateTime dateReception;
    private String referenceBl;
    private LocalDateTime dateBl;
    private String refFournisseur;
    private String nomFournisseur;
    private Integer idAgelio;
    private Integer blDivalto;
    private String statut;
    private String affaireCode;
    private String userLogin;
    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;
    private String affaireLibelle;
    private String createurNom;
    private Integer commandeCode;
}