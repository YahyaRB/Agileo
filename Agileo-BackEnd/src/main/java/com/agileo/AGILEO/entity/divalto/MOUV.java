    package com.agileo.AGILEO.entity.divalto;

    import jakarta.persistence.*;
    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;
    import java.math.BigDecimal;
    import java.time.LocalDate;
    import java.time.LocalDateTime;

    @Entity
    @Table(name = "MOUV", schema = "dbo")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class MOUV{

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "MOUV_ID")
        private Integer mouvId;

        @Column(name = "ENRNO")
        private BigDecimal enrno;

        @Column(name = "TICOD", length = 1)
        private String ticod;

        @Column(name = "DOS", length = 8)
        private String dos;

        @Column(name = "ETB", length = 3)
        private String etb;

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

        @Column(name = "DEPO", length = 3)
        private String depo;

        @Column(name = "PREFDVNO", length = 3)
        private String prefdvno;

        @Column(name = "DVNO")
        private BigDecimal dvno;

        @Column(name = "DVDT")
        private LocalDate dvdt;

        @Column(name = "DVLG")
        private BigDecimal dvlg;

        @Column(name = "DVSLG")
        private BigDecimal dvslg;

        @Column(name = "PICOD")
        private BigDecimal picod;

        @Column(name = "REF", length = 25)
        private String ref;

        @Column(name = "DVCE4", length = 25)
        private String dvce4;

        @Column(name = "PREFCDNO", length = 25)
        private String prefcdno;

        @Column(name = "SREF1", length = 8)
        private String sref1;

        @Column(name = "SREF2", length = 8)
        private String sref2;

        @Column(name = "TIERS", length = 20)
        private String tiers;


        @Column(name = "BLNO")
        private BigDecimal blno;

        @Column(name = "BLDT")
        private LocalDate bldt;

        @Column(name = "CDLG")
        private BigDecimal cdlg;
        @Column(name = "CDSLG")
        private BigDecimal cdslg;

        @Column(name = "CDCE4", length = 1)
        private String  cdce4;

        @Column(name = "CDENRNO")
        private BigDecimal cdenrno;

    @Column(name = "IAGLIENTYP")
    private BigDecimal iaglientyp;

        @Column(name = "ARTIND", length = 4)
        private String artind;


        @Column(name = "CONFIGURATEURARTIND", length = 4)
        private String confurateurartind;

        @Column(name = "FANO")
        private BigDecimal fano;

        @Column(name = "FADT")
        private LocalDate fadt;

        @Column(name = "CDNO")
        private BigDecimal cdno;

        @Column(name = "CDDT")
        private LocalDate cddt;

        @Column(name = "OP", length = 3)
        private String op;

        @Column(name = "DEV", length = 4)
        private String dev;

        @Column(name = "USERCR", length = 20)
        private String usercr;

        @Column(name = "USERMO", length = 20)
        private String usermo;

        @Column(name = "PROJET", length = 8)
        private String projet;

        @Column(name = "DES", length = 80)
        private String des;


        @Column(name = "MARCHE", length = 8)
        private String marche;

        @Column(name = "USERCRDH")
        private LocalDateTime usercrdh;

        @Column(name = "USERMODH")
        private LocalDateTime usermodh;

        @Column(name = "STATUS")
        private BigDecimal status;



        @Column(name = "BLQTE", precision = 15, scale = 3)
        private BigDecimal blqte;



        @Column(name = "REFUN", length = 4)
        private String refun;



        @Column(name = "VENUN", length = 4)
        private String venun;

        @Column(name = "PUB", precision = 15, scale = 4)
        private BigDecimal pub;

        @Column(name = "PPAR", precision = 15, scale = 4)
        private BigDecimal ppar;





        @Column(name = "REMMT", precision = 15, scale = 2)
        private BigDecimal remmt;


        @Column(name = "CENOTE")
        private BigDecimal cenote;

        @Column(name = "CMPTOTMT")
        private BigDecimal cmptotmt;

        @Column(name = "COECOD", length = 4)
        private String coecod;

        @Column(name = "COFAMR", length = 4)
        private String cofamr;

        @Column(name = "COFAMV_0001", length = 4)
        private String cofamv0001;

        @Column(name = "COFAMV_0002", length = 4)
        private String cofamv0002;

        @Column(name = "COFAMV_0003", length = 4)
        private String cofamv0003;

        @Column(name = "COMMT_0001")
        private BigDecimal commt0001;

        @Column(name = "COMMT_0002")
        private BigDecimal commt0002;

        @Column(name = "COMMT_0003")
        private BigDecimal commt0003;

        @Column(name = "COMP_0001")
        private BigDecimal comp0001;

        @Column(name = "COMP_0002")
        private BigDecimal comp0002;

        @Column(name = "COMP_0003")
        private BigDecimal comp0003;

        @Column(name = "AFRINDICE", length = 4)
        private String afrindice;

        @Column(name = "APPREMMT")
        private BigDecimal appremmt;

        @Column(name = "APPREMMTUN")
        private BigDecimal appremmtun;

        @Column(name = "AVENANT", length = 8)
        private String avenant;

        @Column(name = "AXE_0001", length = 8)
        private String axe0001;

        @Column(name = "AXE_0002", length = 8)
        private String axe0002;

        @Column(name = "AXE_0003", length = 8)
        private String axe0003;

        @Column(name = "AXE_0004", length = 8)
        private String axe0004;

        @Column(name = "BESOINNO")
        private BigDecimal besoinno;

        @Column(name = "BLASENRNO")
        private BigDecimal blasenrno;

        @Column(name = "BLCE4", length = 1)
        private String blce4;

        @Column(name = "BLENRNO")
        private BigDecimal blenrno;

        @Column(name = "BLLG")
        private BigDecimal bllg;

        @Column(name = "BLSLG")
        private BigDecimal blslg;

        @Column(name = "BPDT")
        private LocalDate bpdt;

        @Column(name = "BPLIGCOMPFL")
        private BigDecimal bpligcompfl;

        @Column(name = "BPNO")
        private BigDecimal bpno;

        @Column(name = "CADEAUFL")
        private BigDecimal cadeaufl;

        @Column(name = "CDNOPERE")
        private BigDecimal cdnopere;

        @Column(name = "CDQTE")
        private BigDecimal cdqte;

        @Column(name = "CONFIGURATEURLINO")
        private BigDecimal configurateurlino;

        @Column(name = "CONFIGURATEURMONOSTATUS")
        private BigDecimal configurateurmonostatus;

        @Column(name = "CONFIGURATEURMULTISTATUS")
        private BigDecimal configurateurmultistatus;

        @Column(name = "CONFIGURATEURREF", length = 25)
        private String configurateurref;

        @Column(name = "CONFIGURATEURSREF1", length = 8)
        private String configurateursref1;

        @Column(name = "CONFIGURATEURSREF2", length = 8)
        private String configurateursref2;

        @Column(name = "CONTRATCOD", length = 8)
        private String contratcod;

        @Column(name = "CPTV", length = 20)
        private String cptv;

        @Column(name = "CRTOTMT")
        private BigDecimal crtotmt;

        @Column(name = "CTMFL")
        private BigDecimal ctmfl;

        @Column(name = "DECCOD")
        private BigDecimal deccod;

        @Column(name = "DEPOORIG", length = 3)
        private String depoorig;

        @Column(name = "DTRENRNO")
        private BigDecimal dtrenrno;

        @Column(name = "DTRGRP")
        private BigDecimal dtrgrp;

        @Column(name = "DTRTYPE")
        private BigDecimal dtrtype;

        @Column(name = "DVENRNO")
        private BigDecimal dvenrno;

        @Column(name = "DVQTE")
        private BigDecimal dvqte;

        @Column(name = "EDCOD", length = 5)
        private String edcod;

        @Column(name = "ELEMNO")
        private BigDecimal elemno;

        @Column(name = "EMBQTE")
        private BigDecimal embqte;

        @Column(name = "EMBUN", length = 4)
        private String embun;

        @Column(name = "ENRNOC_0001")
        private BigDecimal enrnoc0001;

        @Column(name = "ENRNOC_0002")
        private BigDecimal enrnoc0002;

        @Column(name = "ENRNOC_0003")
        private BigDecimal enrnoc0003;

        @Column(name = "ENRNOC_0004")
        private BigDecimal enrnoc0004;

        @Column(name = "ENRNOCAD")
        private BigDecimal enrnocad;

        @Column(name = "ENRNOP_0001")
        private BigDecimal enrnop0001;

        @Column(name = "ENRNOP_0002")
        private BigDecimal enrnop0002;

        @Column(name = "ENRNOP_0003")
        private BigDecimal enrnop0003;

        @Column(name = "ENRNOP_0004")
        private BigDecimal enrnop0004;

        @Column(name = "FACE4", length = 1)
        private String face4;

        @Column(name = "FALG")
        private BigDecimal falg;

        @Column(name = "FAMONTGIM")
        private BigDecimal famontgim;

        @Column(name = "FAPUBGIM")
        private BigDecimal fapubgim;

        @Column(name = "FAQTE")
        private BigDecimal faqte;

        @Column(name = "FASLG")
        private BigDecimal faslg;

        @Column(name = "FILLERSENS")
        private BigDecimal fillersens;

        @Column(name = "FOUFADTGIM")
        private LocalDate foufadtgim;

        @Column(name = "FOUFANOGIM", length = 20)
        private String foufanogim;

        @Column(name = "FOUFAQTEGIM")
        private BigDecimal foufaqtegim;

        @Column(name = "FRAISAPPCOD", length = 10)
        private String fraisappcod;

        @Column(name = "FRAISFL")
        private BigDecimal fraisfl;

        @Column(name = "FRAISIMPACTFLG")
        private BigDecimal fraisimpactflg;

        @Column(name = "FRAISMT")
        private BigDecimal fraismt;

        @Column(name = "FRAISMTGIM")
        private BigDecimal fraismtgim;

        @Column(name = "FRAISVALIDTYP")
        private BigDecimal fraisvalidtyp;

        @Column(name = "GADT")
        private LocalDate gadt;

        @Column(name = "GAMSEQ", length = 6)
        private String gamseq;

        @Column(name = "GIMCOD", length = 10)
        private String gimcod;

        @Column(name = "GPAFL")
        private BigDecimal gpafl;

        @Column(name = "GRATUITFL")
        private BigDecimal gratuitfl;

        @Column(name = "IAGCDENRNO")
        private BigDecimal iagcdenrno;

        @Column(name = "IAGFLUXLIEN", length = 20)
        private String iagfluxlien;

        @Column(name = "ICPFL")
        private BigDecimal icpfl;

        @Column(name = "LIGNE")
        private BigDecimal ligne;

        @Column(name = "LIVDIRECTFL")
        private BigDecimal livdirectfl;

        @Column(name = "MONT")
        private BigDecimal mont;

        @Column(name = "MOTIF", length = 8)
        private String motif;

        @Column(name = "MOTIFSOLDE", length = 8)
        private String motifsolde;

        @Column(name = "MVCOD")
        private BigDecimal mvcod;

        @Column(name = "MVSTAT")
        private BigDecimal mvstat;

        @Column(name = "NOTE")
        private BigDecimal note;

        @Column(name = "OFNO")
        private BigDecimal ofno;

        @Column(name = "OPTIONFL")
        private BigDecimal optionfl;

        @Column(name = "OPTIONVALIDEFL")
        private BigDecimal optionvalidefl;

        @Column(name = "PAFORF")
        private BigDecimal paforf;

        @Column(name = "PAGCOD", length = 2)
        private String pagcod;

        @Column(name = "PANACHEFL")
        private BigDecimal panachefl;

        @Column(name = "PATOTMT")
        private BigDecimal patotmt;

        @Column(name = "PCOD_0001")
        private BigDecimal pcod0001;

        @Column(name = "PCOD_0002")
        private BigDecimal pcod0002;

        @Column(name = "PCOD_0003")
        private BigDecimal pcod0003;

        @Column(name = "PCOD_0004")
        private BigDecimal pcod0004;

        @Column(name = "PCOD_0005")
        private BigDecimal pcod0005;

        @Column(name = "PCOD_0006")
        private BigDecimal pcod0006;

        @Column(name = "PERIODEDDT")
        private LocalDate periodeddt;

        @Column(name = "PERIODEFDT")
        private LocalDate periodefdt;

        @Column(name = "PFCNO")
        private BigDecimal pfcno;

        @Column(name = "POSITION", length = 8)
        private String position;

        @Column(name = "PREFBLNO", length = 10)
        private String prefblno;

        @Column(name = "PREFCDNOPERE", length = 10)
        private String prefcdnopere;

        @Column(name = "PREFFANO", length = 10)
        private String preffano;

        @Column(name = "PREFOFNO", length = 10)
        private String prefofno;

        @Column(name = "PRGQTE")
        private BigDecimal prgqte;

        @Column(name = "PRGREFQTE")
        private BigDecimal prgrefqte;

        @Column(name = "PRIOCOD")
        private BigDecimal priocod;

        @Column(name = "PRIXSPECIALFL")
        private BigDecimal prixspecialfl;

        @Column(name = "PROMOREMCOD", length = 8)
        private String promoremcod;

        @Column(name = "PROMOTACOD", length = 8)
        private String promotacod;

        @Column(name = "PROMOTYP")
        private BigDecimal promotyp;

        @Column(name = "PUBTYP")
        private BigDecimal pubtyp;

        @Column(name = "PUBUN", length = 4)
        private String pubun;

        @Column(name = "PUNETORI")
        private BigDecimal punetori;

        @Column(name = "PUSTAT")
        private BigDecimal pustat;

        @Column(name = "PVCOD")
        private BigDecimal pvcod;

        @Column(name = "QTE1")
        private BigDecimal qte1;

        @Column(name = "QTE2")
        private BigDecimal qte2;

        @Column(name = "QTE3")
        private BigDecimal qte3;

        @Column(name = "QTETYP")
        private BigDecimal qtetyp;

        @Column(name = "REBUCOD", length = 4)
        private String rebucod;

        @Column(name = "RECPTNO")
        private BigDecimal recptno;

        @Column(name = "REFAMR", length = 8)
        private String refamr;

        @Column(name = "REFAMRX", length = 8)
        private String refamrx;

        @Column(name = "REFFO", length = 40)
        private String reffo;

        @Column(name = "REFQTE")
        private BigDecimal refqte;

        @Column(name = "REGLECOD", length = 8)
        private String reglecod;

        @Column(name = "RELCOD_0001")
        private BigDecimal relcod0001;

        @Column(name = "RELCOD_0002")
        private BigDecimal relcod0002;

        @Column(name = "RELCOD_0003")
        private BigDecimal relcod0003;

        @Column(name = "REM_0001")
        private BigDecimal rem0001;

        @Column(name = "REM_0002")
        private BigDecimal rem0002;

        @Column(name = "REM_0003")
        private BigDecimal rem0003;

        @Column(name = "REMCOD", length = 8)
        private String remcod;

        @Column(name = "REMCODCAD", length = 8)
        private String remcodcad;

        @Column(name = "REMPIEMT_0001")
        private BigDecimal rempiemt0001;

        @Column(name = "REMPIEMT_0002")
        private BigDecimal rempiemt0002;

        @Column(name = "REMPIEMT_0003")
        private BigDecimal rempiemt0003;

        @Column(name = "REMPIEMT_0004")
        private BigDecimal rempiemt0004;

        @Column(name = "REMPIEPART_0001")
        private BigDecimal rempiepart0001;

        @Column(name = "REMPIEPART_0002")
        private BigDecimal rempiepart0002;

        @Column(name = "REMPIEPART_0003")
        private BigDecimal rempiepart0003;

        @Column(name = "REMPIEPART_0004")
        private BigDecimal rempiepart0004;

        @Column(name = "REMTYP_0001")
        private BigDecimal remtyp0001;

        @Column(name = "REMTYP_0002")
        private BigDecimal remtyp0002;

        @Column(name = "REMTYP_0003")
        private BigDecimal remtyp0003;

        @Column(name = "REPR_0001", length = 20)
        private String repr0001;

        @Column(name = "REPR_0002", length = 20)
        private String repr0002;

        @Column(name = "REPR_0003", length = 20)
        private String repr0003;

        @Column(name = "RGPENRNO")
        private BigDecimal rgpenrno;

        @Column(name = "SENS")
        private BigDecimal sens;

        @Column(name = "SOLDERELFL")
        private BigDecimal solderelfl;

        @Column(name = "STRES")
        private BigDecimal stres;

        @Column(name = "SYNCHROFL")
        private BigDecimal synchrofl;

        @Column(name = "TACOD", length = 8)
        private String tacod;

        @Column(name = "TAFAMR", length = 8)
        private String tafamr;

        @Column(name = "TAFAMRX", length = 8)
        private String tafamrx;

        @Column(name = "TICKET")
        private BigDecimal ticket;

        @Column(name = "TIERSEXTERNE", length = 20)
        private String tiersexterne;

        @Column(name = "TIERSFOU2", length = 20)
        private String tiersfou2;

        @Column(name = "TVAART", length = 8)
        private String tvaart;

        @Column(name = "TVANASSUJETTIEFL")
        private BigDecimal tvanassujettiefl;

        @Column(name = "TXTCOD")
        private BigDecimal txtcod;

        @Column(name = "TXTEDCOD", length = 5)
        private String txtedcod;

        @Column(name = "TXTNOTE")
        private BigDecimal txtnote;

        @Column(name = "UNTYP", length = 8)
        private String untyp;
    }




