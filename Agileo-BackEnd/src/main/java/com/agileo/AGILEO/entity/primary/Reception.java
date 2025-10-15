package com.agileo.AGILEO.entity.primary;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "M6001_Entete_bl", schema = "dbo")
public class Reception {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Numero")
    private Integer numero;

    @Column(name = "Commande")
    private Integer commande;

    @Column(name = "PJ_bc")
    private Integer pjBc;

    @Column(name = "Pinotiers", length = 50)
    private String pinotiers;

    @Column(name = "Bl_divalto")
    private Integer blDivalto;

    @Column(name = "SYSCREATIONDATE")
    private LocalDateTime sysCreationDate;

    @Column(name = "SYSCREATORID")
    private Integer sysCreatorId;

    @Column(name = "SYSMODIFICATIONDATE")
    private LocalDateTime sysModificationDate;

    @Column(name = "SYSUSERID")
    private Integer sysUserId;

    @Column(name = "SYSSYNCHRONIZATIONDATE")
    private LocalDateTime sysSynchronizationDate;

    @Column(name = "SYSSTATE")
    private Integer sysState;
}