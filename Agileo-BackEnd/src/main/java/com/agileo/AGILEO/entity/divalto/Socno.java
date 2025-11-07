package com.agileo.AGILEO.entity.divalto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "SOCNO", schema = "dbo")
public class Socno {
    @Id
    @Column(name = "SOCNO_ID")
    private Integer socnoId;

    @Column(name = "CE1", length = 1, nullable = false)
    private String ce1;

    @Column(name = "CE2", length = 1, nullable = false)
    private String ce2;

    @Column(name = "CE3", length = 1, nullable = false)
    private String ce3;

    @Column(name = "CE4", length = 1, nullable = false)
    private String ce4;

    @Column(name = "CE5", length = 1, nullable = false)
    private String ce5;

    @Column(name = "CE6", length = 1, nullable = false)
    private String ce6;

    @Column(name = "CE7", length = 1, nullable = false)
    private String ce7;

    @Column(name = "CE8", length = 1, nullable = false)
    private String ce8;

    @Column(name = "CE9", length = 1, nullable = false)
    private String ce9;

    @Column(name = "CEA", length = 1, nullable = false)
    private String cea;

    @Column(name = "DOS", length = 8, nullable = false)
    private String dos;

    @Column(name = "BPNOC")
    private BigDecimal bpnoc;

    @Column(name = "TVAPINO")
    private BigDecimal tvapino;

    @Column(name = "ENRNO")
    private BigDecimal enrno;

    @Column(name = "TICKETRES")
    private BigDecimal ticketres;

    @Column(name = "VTLNO")
    private BigDecimal vtlno;

    @Column(name = "RFNO", length = 25, nullable = false)
    private String rfno;

    @Column(name = "PRNO", length = 20, nullable = false)
    private String prno;

    @Column(name = "CLNO", length = 20, nullable = false)
    private String clno;

    @Column(name = "FONO", length = 20, nullable = false)
    private String fono;

    @Column(name = "PROJET", length = 8, nullable = false)
    private String projet;

    @Column(name = "PRPROVNO", length = 20, nullable = false)
    private String prprovno;

    @Column(name = "CLPROVNO", length = 20, nullable = false)
    private String clprovno;

    @Column(name = "FOPROVNO", length = 20, nullable = false)
    private String foprovno;

    @Column(name = "PRPROVD", length = 20, nullable = false)
    private String prprovd;

    @Column(name = "CLPROVD", length = 20, nullable = false)
    private String clprovd;

    @Column(name = "FOPROVD", length = 20, nullable = false)
    private String foprovd;

    @Column(name = "PROJETPROV", length = 8, nullable = false)
    private String projetprov;

    @Column(name = "PROJETPROVD", length = 8, nullable = false)
    private String projetprovd;

    @Column(name = "BPNO")
    private BigDecimal bpno;

    @Column(name = "BPDETNO")
    private BigDecimal bpdetno;

    @Column(name = "COLINO", length = 9, nullable = false)
    private String colino;

    @Column(name = "PALNO", length = 18, nullable = false)
    private String palno;

    @Column(name = "CONTNO", length = 18, nullable = false)
    private String contno;

    @Column(name = "ENACDT")
    @Temporal(TemporalType.DATE)
    private Date enacdt;

    @Column(name = "CONFIGURATEURPINO")
    private BigDecimal configurateurpino;

    @Column(name = "CONFIGURATEURLINO")
    private BigDecimal configurateurlino;

    @Column(name = "LOTNO")
    private BigDecimal lotno;

    @Column(name = "BRNO")
    private BigDecimal brno;

    @Column(name = "PREPANOC")
    private BigDecimal prepanoc;

    @Column(name = "PREPANOF")
    private BigDecimal prepanof;

    @Column(name = "PDPNO")
    private BigDecimal pdpno;

    @Column(name = "SITECOD", length = 8, nullable = false)
    private String sitecod;

    @Column(name = "CONTRATNO")
    private BigDecimal contratno;

    @Column(name = "PFCNO")
    private BigDecimal pfcno;

    @Column(name = "DAONO")
    private BigDecimal daono;

    @Column(name = "DAOLGNO")
    private BigDecimal daolgno;

    @Column(name = "ELEMNO")
    private BigDecimal elemno;

    @Column(name = "NDFNO")
    private BigDecimal ndfno;

    @Column(name = "NDFLGNO")
    private BigDecimal ndflgno;

    @Column(name = "CTRLFANO")
    private BigDecimal ctrlfano;

    @Column(name = "EANNO", length = 13, nullable = false)
    private String eanno;

    @Column(name = "UP_RG_CPT")
    private BigDecimal upRgCpt;

    @Column(name = "UP_ACOMPTE_CPT")
    private BigDecimal upAcompteCpt;

    @Column(name = "BEXNO")
    private BigDecimal bexno;

    @Column(name = "EPHERFNO", length = 25, nullable = false)
    private String epherfno;

    @Column(name = "DTRENRNO")
    private BigDecimal dtrenrno;

    @Column(name = "DTRFASITNO")
    private BigDecimal dtrfasitno;

    @Column(name = "GIMCONTNO")
    private BigDecimal gimcontno;

    @Column(name = "FRAISAPPCODNO", length = 10, nullable = false)
    private String fraisappcodno;

    @Column(name = "INTERENRNO")
    private BigDecimal interenrno;

    @Column(name = "INTERNO")
    private BigDecimal interno;

    @Column(name = "ARTMASTERNO")
    private BigDecimal artmasterno;

    @Column(name = "CFGRFNO")
    private BigDecimal cfgrfno;
}