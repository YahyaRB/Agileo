package com.agileo.AGILEO.entity.divalto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ENT", schema = "dbo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ENT {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ENT_ID")
    private Integer entId;

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

    @Column(name = "CEB", length = 1)
    private String ceb;

    @Column(name = "CEC", length = 1)
    private String cec;

    @Column(name = "CED", length = 1)
    private String ced;

    @Column(name = "CEE", length = 1)
    private String cee;

    @Column(name = "CEF", length = 1)
    private String cef;

    @Column(name = "DOS", length = 8)
    private String dos;

    @Column(name = "TICOD", length = 1)
    private String ticod;

    @Column(name = "PICOD")
    private BigDecimal picod;

    @Column(name = "TIERS", length = 20)
    private String tiers;

    @Column(name = "PREFPINO", length = 10)
    private String prefpino;

    @Column(name = "PINO")
    private BigDecimal pino;

    @Column(name = "PIDT")
    private LocalDate pidt;

    @Column(name = "ETB", length = 3)
    private String etb;

    @Column(name = "STATUS")
    private BigDecimal status;

    @Column(name = "DEV", length = 4)
    private String dev;

    @Column(name = "OP", length = 3)
    private String op;

    @Column(name = "USERCR", length = 20)
    private String usercr;

    @Column(name = "USERMO", length = 20)
    private String usermo;

    @Column(name = "REPR_0001", length = 20)
    private String repr0001;

    @Column(name = "REPR_0002", length = 20)
    private String repr0002;

    @Column(name = "REPR_0003", length = 20)
    private String repr0003;

    @Column(name = "RIBCOD", length = 8)
    private String ribcod;

    @Column(name = "MARCHE", length = 8)
    private String marche;

    @Column(name = "PROJET", length = 8)
    private String projet;

    @Column(name = "DEPO", length = 3)
    private String depo;

    @Column(name = "ADRTIERS_0001", length = 20)
    private String adrtiers0001;

    @Column(name = "ADRTIERS_0002", length = 20)
    private String adrtiers0002;

    @Column(name = "ADRTIERS_0003", length = 20)
    private String adrtiers0003;

    @Column(name = "ADRTIERS_0004", length = 20)
    private String adrtiers0004;

    @Column(name = "ADRTIERS_0005", length = 20)
    private String adrtiers0005;

    @Column(name = "ADRCOD_0001", length = 8)
    private String adrcod0001;

    @Column(name = "ADRCOD_0002", length = 8)
    private String adrcod0002;

    @Column(name = "ADRCOD_0003", length = 8)
    private String adrcod0003;

    @Column(name = "ADRCOD_0004", length = 8)
    private String adrcod0004;

    @Column(name = "ADRCOD_0005", length = 8)
    private String adrcod0005;

    @Column(name = "BLMOD", length = 8)
    private String blmod;

    @Column(name = "REGL", length = 4)
    private String regl;

    @Column(name = "TOUR", length = 6)
    private String tour;

    @Column(name = "PIREF", length = 40)
    private String piref;

    @Column(name = "PINOTIERS", length = 20)
    private String pinotiers;

    @Column(name = "TIERSPAYER", length = 20)
    private String tierspayer;

    @Column(name = "TIERSGRP", length = 20)
    private String tiersgrp;

    @Column(name = "TIERSRLV", length = 20)
    private String tiersrlv;

    @Column(name = "BAPSALCOD", length = 20)
    private String bapsalcod;

    @Column(name = "SALCOD", length = 20)
    private String salcod;

    @Column(name = "PREFRLVNO", length = 10)
    private String prefrlvno;

    @Column(name = "RLVNO")
    private BigDecimal rlvno;

    @Column(name = "RLVDT")
    private LocalDate rlvdt;

    @Column(name = "DELDEMDT")
    private LocalDate deldemdt;

    @Column(name = "DELACCDT")
    private LocalDate delaccdt;

    @Column(name = "DELREPDT")
    private LocalDate delrepdt;

    @Column(name = "ECHDT")
    private LocalDate echdt;

    @Column(name = "TAFAM", length = 8)
    private String tafam;

    @Column(name = "TAFAMX", length = 8)
    private String tafamx;

    @Column(name = "REFAM", length = 8)
    private String refam;

    @Column(name = "REFAMX", length = 8)
    private String refamx;

    @Column(name = "TACOD", length = 8)
    private String tacod;

    @Column(name = "REMCOD", length = 8)
    private String remcod;

    @Column(name = "COFAM", length = 4)
    private String cofam;

    @Column(name = "COFAMV_0001", length = 4)
    private String cofamv0001;

    @Column(name = "COFAMV_0002", length = 4)
    private String cofamv0002;

    @Column(name = "COFAMV_0003", length = 4)
    private String cofamv0003;

    @Column(name = "AXE_0001", length = 8)
    private String axe0001;

    @Column(name = "AXE_0002", length = 8)
    private String axe0002;

    @Column(name = "AXE_0003", length = 8)
    private String axe0003;

    @Column(name = "AXE_0004", length = 8)
    private String axe0004;

    @Column(name = "ETANO", length = 1)
    private String etano;

    @Column(name = "TXTEDCODD", length = 5)
    private String txtedcodd;

    @Column(name = "TXTEDCODF", length = 5)
    private String txtedcodf;

    @Column(name = "CONTACT", length = 8)
    private String contact;

    @Column(name = "PREFBLASNO", length = 10)
    private String prefblasno;

    @Column(name = "BLASNO")
    private BigDecimal blasno;

    @Column(name = "BLASDEPO", length = 3)
    private String blasdepo;

    @Column(name = "TPFT", length = 1)
    private String tpft;

    @Column(name = "AVENANT", length = 8)
    private String avenant;

    @Column(name = "CESINTCOD")
    private BigDecimal cesintcod;

    @Column(name = "PROMOTACOD", length = 8)
    private String promotacod;

    @Column(name = "PROMOREMCOD", length = 8)
    private String promoremcod;

    @Column(name = "PREFCDNOPERE", length = 10)
    private String prefcdnopere;

    @Column(name = "CDNOPERE")
    private BigDecimal cdnopere;

    @Column(name = "TPVBL")
    private BigDecimal tpvbl;

    @Column(name = "DEEEINCCOD")
    private BigDecimal deeeinccod;

    @Column(name = "PREFPINA", length = 10)
    private String prefpina;

    @Column(name = "PINA")
    private BigDecimal pina;

    @Column(name = "USERCRDH")
    private LocalDateTime usercrdh;

    @Column(name = "USERMODH")
    private LocalDateTime usermodh;

    @Column(name = "CENOTE")
    private BigDecimal cenote;

    @Column(name = "NOTE")
    private BigDecimal note;

    @Column(name = "TXTCODD")
    private BigDecimal txtcodd;

    @Column(name = "TXTCODF")
    private BigDecimal txtcodf;

    @Column(name = "TXTNOTED")
    private BigDecimal txtnoted;

    @Column(name = "TXTNOTEF")
    private BigDecimal txtnotef;

    @Column(name = "ORIGINE")
    private BigDecimal origine;

    @Column(name = "HTMT", precision = 15, scale = 2)
    private BigDecimal htmt;

    @Column(name = "TTCMT", precision = 15, scale = 2)
    private BigDecimal ttcmt;

    @Column(name = "HTPDTMT", precision = 15, scale = 2)
    private BigDecimal htpdtmt;

    @Column(name = "ESCP", precision = 7, scale = 2)
    private BigDecimal escp;

    @Column(name = "ACMT", precision = 15, scale = 2)
    private BigDecimal acmt;

    @Column(name = "SOACMT", precision = 15, scale = 2)
    private BigDecimal soacmt;

    @Column(name = "REMMT", precision = 15, scale = 2)
    private BigDecimal remmt;

    @Column(name = "REM1", precision = 7, scale = 2)
    private BigDecimal rem1;

    @Column(name = "REMTYP1")
    private BigDecimal remtyp1;

    @Column(name = "FOUHTMT", precision = 15, scale = 2)
    private BigDecimal fouhtmt;

    @Column(name = "FOUESCMT", precision = 15, scale = 2)
    private BigDecimal fouescmt;

    @Column(name = "FOUTVAMT", precision = 15, scale = 2)
    private BigDecimal foutvamt;

    @Column(name = "DEVP", precision = 11, scale = 8)
    private BigDecimal devp;

    @Column(name = "PIEDNO_0001")
    private BigDecimal piedno0001;

    @Column(name = "PIEDNO_0002")
    private BigDecimal piedno0002;

    @Column(name = "PIEDNO_0003")
    private BigDecimal piedno0003;

    @Column(name = "PIEDMT_0001", precision = 15, scale = 2)
    private BigDecimal piedmt0001;

    @Column(name = "PIEDMT_0002", precision = 15, scale = 2)
    private BigDecimal piedmt0002;

    @Column(name = "PIEDMT_0003", precision = 15, scale = 2)
    private BigDecimal piedmt0003;

    @Column(name = "NBEX")
    private BigDecimal nbex;

    @Column(name = "PIRELCOD")
    private BigDecimal pirelcod;

    @Column(name = "RELCOD")
    private BigDecimal relcod;

    @Column(name = "EDITCOD")
    private BigDecimal editcod;

    @Column(name = "TRCOD")
    private BigDecimal trcod;

    @Column(name = "BOREDICOD")
    private BigDecimal boredicod;

    @Column(name = "ASCOD")
    private BigDecimal ascod;

    @Column(name = "ECHVCOD")
    private BigDecimal echvcod;

    @Column(name = "ENCASSCOD")
    private BigDecimal encasscod;

    @Column(name = "ADRTYP_0001")
    private BigDecimal adrtyp0001;

    @Column(name = "ADRTYP_0002")
    private BigDecimal adrtyp0002;

    @Column(name = "ADRTYP_0003")
    private BigDecimal adrtyp0003;

    @Column(name = "ADRTYP_0004")
    private BigDecimal adrtyp0004;

    @Column(name = "ADRTYP_0005")
    private BigDecimal adrtyp0005;

    @Column(name = "PRIOCOD")
    private BigDecimal priocod;

    @Column(name = "HTCOD")
    private BigDecimal htcod;

    @Column(name = "STRES")
    private BigDecimal stres;

    @Column(name = "FAMOD")
    private BigDecimal famod;

    @Column(name = "PERIOD")
    private BigDecimal period;

    @Column(name = "PORCOD")
    private BigDecimal porcod;

    @Column(name = "POICOD")
    private BigDecimal poicod;

    @Column(name = "VOLCOD")
    private BigDecimal volcod;

    @Column(name = "PORFRFL")
    private BigDecimal porfrfl;

    @Column(name = "POITOT", precision = 15, scale = 3)
    private BigDecimal poitot;

    @Column(name = "VOLTOT", precision = 15, scale = 3)
    private BigDecimal voltot;

    @Column(name = "COLINB")
    private BigDecimal colinb;

    @Column(name = "REFNB")
    private BigDecimal refnb;

    @Column(name = "TOURRG")
    private BigDecimal tourrg;

    @Column(name = "REM_0001", precision = 7, scale = 2)
    private BigDecimal rem0001;

    @Column(name = "REM_0002", precision = 7, scale = 2)
    private BigDecimal rem0002;

    @Column(name = "REM_0003", precision = 7, scale = 2)
    private BigDecimal rem0003;

    @Column(name = "REMTYP_0001")
    private BigDecimal remtyp0001;

    @Column(name = "REMTYP_0002")
    private BigDecimal remtyp0002;

    @Column(name = "REMTYP_0003")
    private BigDecimal remtyp0003;

    @Column(name = "COMP_0001")
    private BigDecimal comp0001;

    @Column(name = "COMP_0002")
    private BigDecimal comp0002;

    @Column(name = "COMP_0003")
    private BigDecimal comp0003;

    @Column(name = "PORTHEOMT", precision = 15, scale = 2)
    private BigDecimal portheomt;

    @Column(name = "REMPIETOT", precision = 15, scale = 2)
    private BigDecimal rempietot;

    @Column(name = "TRANSJRNB")
    private BigDecimal transjrnb;

    @Column(name = "OFASCOD")
    private BigDecimal ofascod;

    @Column(name = "FINAL")
    private BigDecimal finalField;

    @Column(name = "QUACOD")
    private BigDecimal quacod;

    @Column(name = "CEJOINT")
    private BigDecimal cejoint;

    @Column(name = "JOINT")
    private BigDecimal joint;

    @Column(name = "DEEEMT", precision = 15, scale = 2)
    private BigDecimal deeemt;

    @Column(name = "FOUDEEEMT", precision = 15, scale = 2)
    private BigDecimal foudeeemt;

    @Column(name = "PRGCDEFLG")
    private BigDecimal prgcdeflg;

    @Column(name = "BQCPCE", length = 8)
    private String bqcpce;

    @Column(name = "POINCOD")
    private BigDecimal poincod;

    @Column(name = "POINTOT", precision = 15, scale = 3)
    private BigDecimal pointot;

    @Column(name = "PRIOREG")
    private BigDecimal prioreg;

    @Column(name = "TVATIE", length = 8)
    private String tvatie;

    @Column(name = "STLGTGAMCOD", length = 8)
    private String stlgtgamcod;

    @Column(name = "DTFLG")
    private BigDecimal dtflg;

    @Column(name = "SYNCHROFL")
    private BigDecimal synchrofl;

    @Column(name = "ICPFL")
    private BigDecimal icpfl;

    @Column(name = "LIEUINCT", length = 40)
    private String lieuinct;

    @Column(name = "PORFRCOD")
    private BigDecimal porfrcod;

    @Column(name = "PORFRVAL", precision = 15, scale = 3)
    private BigDecimal porfrval;

    @Column(name = "TRANSICOD", length = 8)
    private String transicod;

    @Column(name = "TVABLCD3", length = 3)
    private String tvablcd3;

    @Column(name = "CEATRAITEFL")
    private BigDecimal ceatraitefl;

    @Column(name = "SITECOD", length = 8)
    private String sitecod;

    @Column(name = "UP_DEMANDEUR", length = 20)
    private String upDemandeur;

    @Column(name = "UP_DATERECUPERATION")
    private LocalDate upDaterecuperation;

    @Column(name = "BEXNO")
    private BigDecimal bexno;

    @Column(name = "BLQFL")
    private BigDecimal blqfl;

    @Column(name = "CONFIRMATIONFL")
    private BigDecimal confirmationfl;

    @Column(name = "TAXCPLFFL")
    private BigDecimal taxcplffl;

    @Column(name = "TAXSFVFL")
    private BigDecimal taxsfvfl;

    @Column(name = "TVAAUTOLIQFL")
    private BigDecimal tvaautoliqfl;

    @Column(name = "UNLOGCOD")
    private BigDecimal unlogcod;

    @Column(name = "UNLOGTOT", precision = 15, scale = 3)
    private BigDecimal unlogtot;

    @Column(name = "UNTYP", length = 8)
    private String untyp;

    @Column(name = "VALFINDT")
    private LocalDate valfindt;

    @Column(name = "VERSIONDEVISNO")
    private BigDecimal versiondevisno;

    @Column(name = "VERSIONDEVISORIPINO")
    private BigDecimal versiondevisoripino;

    @Column(name = "VERSIONDEVISORIPREFPINO", length = 10)
    private String versiondevisoriprefpino;

    @Column(name = "BPRELCOD")
    private BigDecimal bprelcod;

    @Column(name = "CATPICOD", length = 8)
    private String catpicod;

    @Column(name = "CIRCUITVALIDATIONBLFL")
    private BigDecimal circuitvalidationblfl;

    @Column(name = "CIRCUITVALIDATIONFCTFL")
    private BigDecimal circuitvalidationfctfl;

    @Column(name = "CONDEXP", length = 8)
    private String condexp;

    @Column(name = "ETABLNO", length = 1)
    private String etablno;

    @Column(name = "FRAISAPPCOD", length = 10)
    private String fraisappcod;

    @Column(name = "GOUVFACBLQFL")
    private BigDecimal gouvfacblqfl;

    @Column(name = "INDICENO")
    private BigDecimal indiceno;

    @Column(name = "MODEEXP", length = 8)
    private String modeexp;

    @Column(name = "MOTIF", length = 8)
    private String motif;

    @Column(name = "PIECEDT")
    private LocalDate piecedt;

    @Column(name = "PREFSITNO", length = 10)
    private String prefsitno;

    @Column(name = "REMSEUILFL")
    private BigDecimal remseuilfl;

    @Column(name = "SITNO")
    private BigDecimal sitno;

    @Column(name = "TRANSITFL")
    private BigDecimal transitfl;

    @Column(name = "ACOMPTETYP")
    private BigDecimal acomptetyp;

    @Column(name = "BIDON")
    private BigDecimal bidon;

    @Column(name = "BPJRNB")
    private BigDecimal bpjrnb;

    @Column(name = "PAIEMENTTYP")
    private BigDecimal paiementtyp;

    @Column(name = "RESJRNB")
    private BigDecimal resjrnb;

    @Column(name = "STNFL")
    private BigDecimal stnfl;

    @Column(name = "FANO")
    private BigDecimal fano;

    @Column(name = "PREFFANO", length = 10)
    private String preffano;

    @Column(name = "REGLIMMFL")
    private BigDecimal reglimmfl;

    @Column(name = "TIERSFACT", length = 20)
    private String tiersfact;

    @Column(name = "BTFULLPINO", length = 20)
    private String btfullpino;

    @Column(name = "BTPINO")
    private BigDecimal btpino;

    @Column(name = "BTPREFPINO", length = 10)
    private String btprefpino;

    @Column(name = "BTRETOURFL")
    private BigDecimal btretourfl;

    @Column(name = "BTSTATUS")
    private BigDecimal btstatus;
}