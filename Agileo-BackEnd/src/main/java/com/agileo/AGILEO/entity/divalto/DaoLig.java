package com.agileo.AGILEO.entity.divalto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "DAOLIG")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DaoLig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DAOLIG_ID")
    private Long daoLigId;

    @Column(name = "CE1")
    private Integer ce1;

    @Column(name = "CE2")
    private Integer ce2;

    @Column(name = "CE3")
    private Integer ce3;

    @Column(name = "CE4")
    private Integer ce4;

    @Column(name = "CE5")
    private Integer ce5;

    @Column(name = "CE6")
    private Integer ce6;

    @Column(name = "CE7")
    private Integer ce7;

    @Column(name = "CE8")
    private Integer ce8;

    @Column(name = "CE9")
    private Integer ce9;

    @Column(name = "CEA")
    private Integer cea;

    @Column(name = "DOS")
    private Integer dos;

    @Column(name = "DAOLGNO")
    private Long daoLgNo;

    @Column(name = "DAONO")
    private Long daoNo;

    @Column(name = "DAOTYP")
    private Integer daoTyp;

    @Column(name = "LILG")
    private Integer liLg;

    @Column(name = "ETB")
    private Integer etb;

    @Column(name = "DEPO", length = 3)
    private String depo;

    @Column(name = "STATUS")
    private Integer status;

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

    @Column(name = "QTEINI", precision = 18, scale = 4)
    private BigDecimal qteIni;

    @Column(name = "ACHUN", length = 4)
    private String achUn;

    @Column(name = "REFQTE", precision = 18, scale = 4)
    private BigDecimal refQte;

    @Column(name = "REFUN", length = 4)
    private String refUn;

    @Column(name = "DELDEMDT")
    private Integer delDemDt;

    @Column(name = "DAOLGNOAO")
    private Long daoLgNoAo;

    @Column(name = "TXTCOD", length = 10)
    private String txtCod;

    @Column(name = "TXTNOTE", length = 10)
    private String txtNote;

    @Column(name = "CENOTE")
    private Integer ceNote;

    @Column(name = "NOTE")
    private Long note;

    @Column(name = "CEJOINT")
    private Integer ceJoint;

    @Column(name = "JOINT")
    private Long joint;

    @Column(name = "USERCR", length = 10)
    private String userCr;

    @Column(name = "USERMO", length = 10)
    private String userMo;

    @Column(name = "USERCRDH")
    private Long userCrDh;

    @Column(name = "USERMODH")
    private Long userMoDh;

    @Column(name = "ELEMNO")
    private Long elemNo;

    @Column(name = "AFRINDICE")
    private Integer afrIndice;

    @Column(name = "BESOINNO")
    private Long besoinNo;

    @Column(name = "ARTIND")
    private Integer artInd;

    @Column(name = "UP_MATERIEL", length = 50)
    private String upMateriel;

    @PrePersist
    protected void onCreate() {
        if (dos == null) dos = 1;
        if (daoTyp == null) daoTyp = 1;
        if (status == null) status = 1;
        if (ce1 == null) ce1 = 2;
        if (ce3 == null) ce3 = 1;
        if (ceNote == null) ceNote = 1;
        if (ceJoint == null) ceJoint = 1;
        if (userCrDh == null) userCrDh = System.currentTimeMillis() / 1000;
    }
}