package com.agileo.AGILEO.entity.divalto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "DAOENT", schema = "dbo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DaoEnt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DAOENT_ID")
    private Integer daoEntId;

    @Column(name = "CE1", length = 1)
    private String ce1;

    @Column(name = "CE2", length = 1)
    private String ce2;

    @Column(name = "CE3", length = 1)
    private String ce3;

    @Column(name = "CE4", length = 1)
    private String ce4;

    @Column(name = "CE5", length = 1)
    private String ce5;

    @Column(name = "CE6", length = 1)
    private String ce6;

    @Column(name = "CE7", length = 1)
    private String ce7;

    @Column(name = "CE8", length = 1)
    private String ce8;

    @Column(name = "CE9", length = 1)
    private String ce9;

    @Column(name = "CEA", length = 1)
    private String cea;

    @Column(name = "DOS", length = 8)
    private String dos;

    @Column(name = "DAONO")
    private BigDecimal daoNo;

    @Column(name = "ETB", length = 3)
    private String etb;

    @Column(name = "DAOTYP")
    private BigDecimal daoTyp;

    @Column(name = "DAOREF", length = 40)
    private String daoRef;

    @Column(name = "DAOREFEXT", length = 40)
    private String daoRefExt;

    // ✅ CORRECTION : date au lieu de Integer
    @Column(name = "DAODT")
    private LocalDate daodt;

    // ✅ CORRECTION : date au lieu de Integer
    @Column(name = "DELDEMDT")
    private LocalDate deldemdt;

    @Column(name = "STATUS")
    private BigDecimal status;

    @Column(name = "PROJET", length = 8)
    private String projet;

    @Column(name = "DAOORIG")
    private BigDecimal daoOrig;

    @Column(name = "CATCOD", length = 8)
    private String catcod;

    @Column(name = "ADRTIERS", length = 20)
    private String adrtiers;

    @Column(name = "ADRCOD", length = 8)
    private String adrcod;

    @Column(name = "SERVCOD", length = 8)
    private String servcod;

    @Column(name = "SALCOD", length = 20)
    private String salcod;

    @Column(name = "USERDAO", length = 20)
    private String userdao;

    @Column(name = "CONF", length = 4)
    private String conf;

    @Column(name = "TXTCODD")
    private BigDecimal txtcodd;

    @Column(name = "TXTCODF")
    private BigDecimal txtcodf;

    @Column(name = "TXTNOTED")
    private BigDecimal txtnoted;

    @Column(name = "TXTNOTEF")
    private BigDecimal txtnotef;

    @Column(name = "CENOTE")
    private BigDecimal cenote;

    @Column(name = "NOTE")
    private BigDecimal note;

    @Column(name = "CEJOINT")
    private BigDecimal cejoint;

    @Column(name = "JOINT")
    private BigDecimal joint;

    @Column(name = "USERCR", length = 20)
    private String usercr;

    @Column(name = "USERMO", length = 20)
    private String usermo;

    // ✅ CORRECTION : datetime2 au lieu de Integer
    @Column(name = "USERCRDH")
    private LocalDateTime usercrdh;

    // ✅ CORRECTION : datetime2 au lieu de Integer
    @Column(name = "USERMODH")
    private LocalDateTime usermodh;

    @Column(name = "DEPO", length = 3)
    private String depo;

    // ✅ CORRECTION : date au lieu de Integer
    @Column(name = "DELREPSDT")
    private LocalDate delrepsdt;

    @Column(name = "ELEMNO")
    private BigDecimal elemno;

    @Column(name = "AFRINDICE", length = 4)
    private String afrindice;

    // ✅ CORRECTION : date au lieu de Integer
    @Column(name = "TRANSMISDT")
    private LocalDate transmisdt;

    @Column(name = "UP_ID_AGILEO")
    private BigDecimal upIdAgileo;

    @Column(name = "UP_ID_WEAVY")
    private BigDecimal upIdWeavy;
}