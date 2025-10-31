package com.agileo.AGILEO.entity.divalto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "DAOLIG", schema = "dbo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DaoLig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DAOLIG_ID")
    private Integer daoLigId;

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

    @Column(name = "DAOLGNO")
    private BigDecimal daolgno;

    @Column(name = "DAONO")
    private BigDecimal daoNo;

    @Column(name = "DAOTYP")
    private BigDecimal daoTyp;

    @Column(name = "LILG")
    private BigDecimal lilg;

    @Column(name = "ETB", length = 3)
    private String etb;

    @Column(name = "DEPO", length = 3)
    private String depo;

    @Column(name = "STATUS")
    private BigDecimal status;

    @Column(name = "REF", length = 25)
    private String ref;

    @Column(name = "SREF1", length = 8)
    private String sref1;

    @Column(name = "SREF2", length = 8)
    private String sref2;

    @Column(name = "DES", length = 80)
    private String des;

    @Column(name = "PROJET", length = 8)
    private String projet;

    @Column(name = "QTEINI")
    private BigDecimal qteini;

    @Column(name = "ACHUN", length = 4)
    private String achun;

    @Column(name = "REFQTE")
    private BigDecimal refqte;

    @Column(name = "REFUN", length = 4)
    private String refun;

    // ✅ CORRECTION : date au lieu de Integer
    @Column(name = "DELDEMDT")
    private LocalDate deldemdt;

    @Column(name = "DAOLGNOAO")
    private BigDecimal daolgnoao;

    @Column(name = "TXTCOD")
    private BigDecimal txtcod;

    @Column(name = "TXTNOTE")
    private BigDecimal txtnote;

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

    @Column(name = "ELEMNO")
    private BigDecimal elemno;

    @Column(name = "AFRINDICE", length = 4)
    private String afrindice;

    @Column(name = "BESOINNO")
    private BigDecimal besoinno;

    @Column(name = "ARTIND", length = 8)
    private String artind;

    @Column(name = "UP_MATERIEL", length = 8)
    private String upMateriel;
}