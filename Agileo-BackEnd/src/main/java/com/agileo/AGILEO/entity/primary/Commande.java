package com.agileo.AGILEO.entity.primary;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "Commandes_fou")
public class Commande {
    @Id
    @Column(name = "Commande", nullable = false)
    private Long commande;
    @Column(name = "Ce4", nullable = false, length = 1)
    private String ce;
    @Column(name = "TIERS", nullable = false, length = 20)
    private String fournisseurId;
    @Column(name = "Fournisseur", length = 80)
    private String fournisseur;
    @Column(name = "Affaire", nullable = false, length = 8)
    private String affaireCode;
    @Column(name = "NomAffaire", length = 80)
    private String affaireName;
    @Column(name = "[Date Commande]")
    private LocalDate dateCommande;
    @Column(name = "[Votre référence]", nullable = false, length = 40)
    private String votreReference;
    @Column(name = "[votre pièce]", nullable = false, length = 20)
    private String piece;
//   @Column(name = "ACCESSOIRID")
//   private Integer accessoirId;
}
