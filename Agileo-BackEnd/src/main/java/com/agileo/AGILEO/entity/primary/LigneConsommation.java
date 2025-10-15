package com.agileo.AGILEO.entity.primary;

import lombok.Data;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "M6000_LIGNE_CONSOM") // Nom de table corrigé
@Data
public class LigneConsommation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ligne") // PK, int, not null
    private Integer idLigne;

    @Column(name = "REF", length = 25) // varchar(25), null
    private String ref;

    @Column(name = "DES", length = 80) // varchar(80), null
    private String des;

    @Column(name = "QTE") // numeric(18,2), null
    private Double qte;

    @Column(name = "NUM_CONS") // int, null
    private Integer numCons;

    @Column(name = "UNITE", length = 4) // varchar(4), null
    private String unite;

    @Column(name = "SREF1", length = 8) // varchar(8), null
    private String sref1;

    @Column(name = "SREF2", length = 8) // varchar(8), null
    private String sref2;

    @Column(name = "SYSCREATIONDATE") // datetime, not null
    private LocalDateTime sysCreationDate;

    @Column(name = "SYSCREATORID") // int, not null
    private Integer sysCreatorId;

    @Column(name = "SYSMODIFICATIONDATE") // datetime, not null
    private LocalDateTime sysModificationDate;

    @Column(name = "SYSUSERID") // int, not null
    private Integer sysUserId;

    @Column(name = "SYSSYNCHRONIZATIONDATE") // datetime, null
    private LocalDateTime sysSynchronizationDate;

    @Column(name = "SYSSTATE") // int, not null
    private Integer sysState;

    // Getters et Setters (Lombok les génère automatiquement)
}