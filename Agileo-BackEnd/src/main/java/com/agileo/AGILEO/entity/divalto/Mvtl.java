package com.agileo.AGILEO.entity.divalto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "MVTL", schema = "dbo")
public class Mvtl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MVTL_ID")
    private Integer mvtlId;

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

    @Column(name = "REF", length = 25, nullable = false)
    private String ref;

    @Column(name = "SREF1", length = 8, nullable = false)
    private String sref1;

    @Column(name = "SREF2", length = 8, nullable = false)
    private String sref2;

    @Column(name = "TICOD", length = 1, nullable = false)
    private String ticod;

    @Column(name = "PICOD")
    private BigDecimal picod;

    @Column(name = "TIERS", length = 20, nullable = false)
    private String tiers;

    @Column(name = "OP", length = 3, nullable = false)
    private String op;

    @Column(name = "USERCR", length = 20, nullable = false)
    private String usercr;

    @Column(name = "USERMO", length = 20, nullable = false)
    private String usermo;

    @Column(name = "ENRNO")
    private BigDecimal enrno;

    @Column(name = "LILG")
    private BigDecimal lilg;

    @Column(name = "ETB", length = 3, nullable = false)
    private String etb;

    @Column(name = "DEPO", length = 3, nullable = false)
    private String depo;

    @Column(name = "LIEU", length = 20, nullable = false)
    private String lieu;

    @Column(name = "TICKETRES")
    private BigDecimal ticketres;

    @Column(name = "BLDT")
    private LocalDate bldt;

    @Column(name = "DELDT")
    private LocalDate deldt;

    @Column(name = "DELDEMDT")

    private LocalDate deldemdt;

    @Column(name = "DELACCDT")

    private LocalDate delaccdt;

    @Column(name = "DELREPDT")

    private LocalDate delrepdt;

    @Column(name = "VTLNO")
    private BigDecimal vtlno;

    @Column(name = "VTLNA")
    private BigDecimal vtlna;

    @Column(name = "COLINO", length = 9, nullable = false)
    private String colino;

    @Column(name = "SERIE", length = 30, nullable = false)
    private String serie;

    @Column(name = "NST", length = 2, nullable = false)
    private String nst;

    @Column(name = "STDTSQL", length = 8, nullable = false)
    private String stdtsql;

    @Column(name = "SENS")
    private BigDecimal sens;

    @Column(name = "PREFPINO", length = 10, nullable = false)
    private String prefpino;

    @Column(name = "PINO")
    private BigDecimal pino;

    @Column(name = "BLASLIEU", length = 20, nullable = false)
    private String blaslieu = "                    ";

    @Column(name = "CDVTLNO")
    private BigDecimal cdvtlno;

    @Column(name = "BLASVTLNO")
    private BigDecimal blasvtlno;

    @Column(name = "PEREMPDT")
    private LocalDate perempdt;

    @Column(name = "TIERSSTOCK", length = 20, nullable = false)
    private String tiersstock;

    @Column(name = "RCONO")
    private BigDecimal rcono;

    @Column(name = "USERCRDH")
    private LocalDateTime usercrdh;

    @Column(name = "USERMODH")
    private LocalDateTime usermodh;

    @Column(name = "CR")
    private BigDecimal cr;

    @Column(name = "CNCR")
    private BigDecimal cncr;

    @Column(name = "CMP")
    private BigDecimal cmp;

    @Column(name = "CRGAM")
    private BigDecimal crgam;

    @Column(name = "QTE")
    private BigDecimal qte;

    @Column(name = "REFQTE")
    private BigDecimal refqte;

    @Column(name = "STQTE")
    private BigDecimal stqte;

    @Column(name = "RESQTE")
    private BigDecimal resqte;

    @Column(name = "STRES")
    private BigDecimal stres;

    @Column(name = "STATUS")
    private BigDecimal status;

    @Column(name = "OFRESCOD")
    private BigDecimal ofrescod;

    @Column(name = "PREVFLG")
    private BigDecimal prevflg;

    @Column(name = "BPDETNO")
    private BigDecimal bpdetno;

    @Column(name = "TICKETMRESS")
    private BigDecimal ticketmress;

    @Column(name = "MANUTCOD", length = 8, nullable = false)
    private String manutcod;

    @Column(name = "SERIEFOU", length = 30, nullable = false)
    private String seriefou;

    @Column(name = "CONTRATNO")
    private BigDecimal contratno;

    @Column(name = "MATLILG")
    private BigDecimal matlilg;

    @Column(name = "RMNO")
    private BigDecimal rmno;

    @Column(name = "ACTNO")
    private BigDecimal actno;

    @Column(name = "ARTIND", length = 8, nullable = false)
    private String artind = "        ";
}