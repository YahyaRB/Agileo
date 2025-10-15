package com.agileo.AGILEO.entity.primary;

import lombok.Data;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "Livraison_ERP") // Nom de table corrigé
@Data
public class ArticleReception {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REF",nullable = false, length = 25) // PK, int, not null
    private String articleId;
    @Column(name = "DES", nullable = false, length = 80)
    private String designation;
    @Column(name = "CDNO", nullable = false)
    private Long commande;
    @Column(name = "REFUN", nullable = false, length = 4)
    private String unite;
    @Column(name = "PROJET", nullable = false, length = 8)
    private String affaireCode;
    @Column(name = "TIERS", nullable = false, length = 20)
    private String fournisseurRef;
    @Column(name = "QteCommandee", nullable = false, precision = 38, scale = 3)
    private BigDecimal qteCommandee;
    @Column(name = "QteLivree", nullable = false, precision = 38, scale = 3)
    private BigDecimal qteLivree;
    @Column(name = "QteRest", precision = 38, scale = 2)
    private BigDecimal qteRest;
}
