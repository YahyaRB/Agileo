package com.agileo.AGILEO.Dtos.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DemandeAchatResponseDTO {

    private Integer id;
    private String chantier;
    private String chantierLibelle; // NOUVEAU : Libellé de l'affaire
    private LocalDateTime delaiSouhaite;
    private String commentaire;
    private Integer login;
    private String demandeurNom; // NOUVEAU : Nom complet du demandeur
    private String demandeurLogin; // NOUVEAU : Login du demandeur
    private String createurNom; // NOUVEAU : Nom complet du créateur
    private LocalDateTime dateDa;
    private Integer statut;
    private String statutLabel;
    private String numDa;
    private String dsDivalto;
    private Integer pjDa;

    // Champs système
    private LocalDateTime sysCreationDate;
    private Integer sysCreatorId;
    private LocalDateTime sysModificationDate;
    private Integer sysUserId;
    private LocalDateTime sysSynchronizationDate;
    private Integer sysState;

    // Lignes de demande et pièces jointes
    private List<LigneDemandeAchatResponseDTO> lignesDemande;

}