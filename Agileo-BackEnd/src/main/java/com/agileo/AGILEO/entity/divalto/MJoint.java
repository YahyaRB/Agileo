package com.agileo.AGILEO.entity.divalto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "MJOINT", schema = "dbo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MJoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MJOINT_ID")
    private Integer mJointId;

    @Column(name = "APPLIC", length = 8)
    private String applic;

    @Column(name = "JOINT")
    private BigDecimal joint;

    @Column(name = "FICC", length = 231)
    private String ficc;

    @Column(name = "CHEMIN", length = 256)
    private String chemin;

    @Column(name = "JOINTOBJ", length = 32)
    private String jointobj;

    @Column(name = "LIB80", length = 80)
    private String lib80;

    @Column(name = "NATUREJOINTCOD", length = 20)
    private String naturejointcod;

    @Column(name = "CRYPST")
    private BigDecimal crypst;

    @Column(name = "CONFL", length = 4)
    private String confl;

    @Column(name = "CONFFIC", length = 4)
    private String conffic;

    @Column(name = "USERCR", length = 20)
    private String usercr;

    // ✅ CORRECTION : datetime2 au lieu de Integer
    @Column(name = "USERCRDH")
    private LocalDateTime usercrdh;

    @Column(name = "MOTCLE", length = 8)
    private String motcle;

    // ✅ CORRECTION : datetime2 au lieu de Integer
    @Column(name = "CREDH")
    private LocalDateTime credh;

    @Column(name = "IDAGILEO")
    private BigDecimal idagileo;

    @Column(name = "IDAGILEODOC")
    private BigDecimal idagileodoc;

    @Column(name = "IDAGILEOFILE")
    private BigDecimal idagileofile;

    // ✅ CORRECTION : datetime2 au lieu de Integer
    @Column(name = "MODIFDH")
    private LocalDateTime modifdh;

    @Column(name = "SUPPFL")
    private BigDecimal suppfl;

    @Column(name = "TAILLEFIC")
    private BigDecimal taillefic;

    @Column(name = "USERMO", length = 20)
    private String usermo;

    // ✅ CORRECTION : datetime2 au lieu de Integer
    @Column(name = "USERMODH")
    private LocalDateTime usermodh;

    @Column(name = "VERSIONAGILEO")
    private BigDecimal versionagileo;
}