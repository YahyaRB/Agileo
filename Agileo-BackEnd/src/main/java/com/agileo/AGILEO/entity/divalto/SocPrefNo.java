package com.agileo.AGILEO.entity.divalto;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "SOCPREFNO")
@Data
public class SocPrefNo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SOCPREFNO_ID")
    private Integer socprefnoId;

    @Column(name = "CE1")
    private String ce1;

    @Column(name = "CE2")
    private String ce2;

    @Column(name = "CE3")
    private String ce3;

    @Column(name = "CE4")
    private String ce4;

    @Column(name = "CE5")
    private String ce5;

    @Column(name = "CE6")
    private String ce6;

    @Column(name = "CE7")
    private String ce7;

    @Column(name = "CE8")
    private String ce8;

    @Column(name = "CE9")
    private String ce9;

    @Column(name = "CEA")
    private String cea;

    @Column(name = "DOS")
    private String dos;

    @Column(name = "ETB")
    private String etb;

    @Column(name = "TICOD")
    private String ticod;

    @Column(name = "PICOD")
    private BigDecimal picod;

    @Column(name = "PREFPINO")
    private String prefpino;

    @Column(name = "PINO")
    private BigDecimal pino;

    @Column(name = "USERCRDH")
    private LocalDateTime usercrdh;

    @Column(name = "USERMODH")
    private LocalDateTime usermodh;

    @Column(name = "USERCR")
    private String usercr;

    @Column(name = "USERMO")
    private String usermo;

    @Column(name = "PREFDEFFLG")
    private BigDecimal prefdefflg;

    @Column(name = "PREFTYP")
    private BigDecimal preftyp;
}