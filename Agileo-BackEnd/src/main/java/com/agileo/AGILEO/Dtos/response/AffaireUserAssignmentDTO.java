package com.agileo.AGILEO.Dtos.response;

import lombok.Data;

@Data
public class AffaireUserAssignmentDTO {
    private Integer accessoirId;
    private String affaire;
    private String libelle;
    private String userFullName;
    private String userLogin;
    private String userEmail;
}