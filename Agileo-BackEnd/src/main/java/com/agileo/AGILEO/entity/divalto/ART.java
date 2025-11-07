package com.agileo.AGILEO.entity.divalto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ART", schema = "dbo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ART {

    @Id
    @Column(name = "ART_ID")
    private Integer artId;

    @Column(name = "CE1", length = 1) private String ce1;
    @Column(name = "CE2", length = 1) private String ce2;
    @Column(name = "CE3", length = 1) private String ce3;
    @Column(name = "CE4", length = 1) private String ce4;
    @Column(name = "CE5", length = 1) private String ce5;
    @Column(name = "CE6", length = 1) private String ce6;
    @Column(name = "CE7", length = 1) private String ce7;
    @Column(name = "CE8", length = 1) private String ce8;
    @Column(name = "CE9", length = 1) private String ce9;
    @Column(name = "CEA", length = 1) private String cea;

    @Column(name = "DOS", length = 8) private String dos;
    @Column(name = "REF", length = 25) private String ref;
    @Column(name = "USERCR", length = 20) private String usercr;
    @Column(name = "USERMO", length = 20) private String usermo;
    @Column(name = "CONF", length = 4) private String conf;
    @Column(name = "DES", length = 80) private String des;
    @Column(name = "DESABR", length = 25) private String desabr;
    @Column(name = "EAN", length = 13) private String ean;
    @Column(name = "TIERS", length = 20) private String tiers;
    @Column(name = "TAREF", length = 25) private String taref;
    @Column(name = "REFRPL", length = 25) private String refrpl;
    @Column(name = "TAFAMRX", length = 8) private String tafamrx;
    @Column(name = "TAFAMR", length = 8) private String tafamr;
    @Column(name = "REFAMRX", length = 8) private String refamrx;
    @Column(name = "REFAMR", length = 8) private String refamr;
    @Column(name = "COFAMR", length = 4) private String cofamr;
    @Column(name = "FAM_0001", length = 8) private String fam0001;
    @Column(name = "FAM_0002", length = 8) private String fam0002;
    @Column(name = "FAM_0003", length = 8) private String fam0003;
    @Column(name = "PRODNAT", length = 4) private String prodnat;
    @Column(name = "REFUN", length = 4) private String refun;
    @Column(name = "ACHUN", length = 4) private String achun;
    @Column(name = "STUN", length = 4) private String stun;
    @Column(name = "VENUN", length = 4) private String venun;
    @Column(name = "POIUN", length = 4) private String poiun;
    @Column(name = "VOLUN", length = 4) private String volun;
    @Column(name = "DIMUN", length = 4) private String dimun;
    @Column(name = "CPTV", length = 20) private String cptv;
    @Column(name = "CPTA", length = 20) private String cpta;
    @Column(name = "CPTS", length = 20) private String cpts;

    @Column(name = "TPFR_0001", precision = 18, scale = 2) private BigDecimal tpfr0001;
    @Column(name = "TPFR_0002", precision = 18, scale = 2) private BigDecimal tpfr0002;
    @Column(name = "TPFR_0003", precision = 18, scale = 2) private BigDecimal tpfr0003;

    @Column(name = "TVANOM", length = 13) private String tvanom;
    @Column(name = "TVAUN", length = 4) private String tvaun;
    @Column(name = "TVARGCOD", precision = 18, scale = 2) private BigDecimal tvargcod;
    @Column(name = "EDCOD", length = 5) private String edcod;
    @Column(name = "GRICOD", length = 4) private String gricod;
    @Column(name = "ZONA", length = 40) private String zona;
    @Column(name = "MEDIA", length = 40) private String media;
    @Column(name = "HTML", length = 255) private String html;
    @Column(name = "AXEMSK", length = 8) private String axemsk;
    @Column(name = "AXENO", precision = 18, scale = 2) private BigDecimal axeno;
    @Column(name = "ABCCOD", length = 1) private String abccod;
    @Column(name = "QUESTION", length = 32) private String question;
    @Column(name = "CBNGESCOD", length = 4) private String cbngescod;
    @Column(name = "COMPETCOD", length = 8) private String competcod;
    @Column(name = "CPTACES", length = 20) private String cptaces;
    @Column(name = "CPTVCES", length = 20) private String cptvces;
    @Column(name = "ACHTAFAMRX", length = 8) private String achtafamrx;
    @Column(name = "ACHTAFAMR", length = 8) private String achtafamr;
    @Column(name = "ACHREFAMRX", length = 8) private String achrefamrx;
    @Column(name = "ACHREFAMR", length = 8) private String achrefamr;
    @Column(name = "SURFUN", length = 4) private String surfun;

    @Column(name = "COEFPTS", precision = 18, scale = 2) private BigDecimal coefpts;
    @Column(name = "CONFIGURATEURFORMULAIRE", length = 20) private String configurateurrformulaire;
    @Column(name = "CONFIGURATEURCHEMINCOD", length = 20) private String configurateurchemincod;
    @Column(name = "SMCFL", precision = 18, scale = 2) private BigDecimal smcfl;
    @Column(name = "SMCVISUFL", precision = 18, scale = 2) private BigDecimal smcvisufl;

    @Column(name = "USERCRDH") private LocalDateTime usercrdh;
    @Column(name = "USERMODH") private LocalDateTime usermodh;
    @Column(name = "HSDT") private LocalDate hsdt;
    @Column(name = "DOPDH") private LocalDateTime dopdh;

    @Column(name = "CENOTE", precision = 18, scale = 2) private BigDecimal cenote;
    @Column(name = "NOTE", precision = 18, scale = 2) private BigDecimal note;
    @Column(name = "POIN", precision = 18, scale = 2) private BigDecimal poin;
    @Column(name = "POIB", precision = 18, scale = 2) private BigDecimal poib;
    @Column(name = "GRISAIS", precision = 18, scale = 2) private BigDecimal grisas;
    @Column(name = "RESTOTQTE", precision = 18, scale = 2) private BigDecimal restotqte;
    @Column(name = "CDECLQTE", precision = 18, scale = 2) private BigDecimal cdeclqte;
    @Column(name = "CDEFOQTE", precision = 18, scale = 2) private BigDecimal cdefoqte;
    @Column(name = "STTOTQTE", precision = 18, scale = 2) private BigDecimal sttotqte;

    @Column(name = "INVDT") private LocalDate invdt;

    @Column(name = "GARJRNB", precision = 18, scale = 2) private BigDecimal garjrnb;
    @Column(name = "STCOD", precision = 18, scale = 2) private BigDecimal stcod;
    @Column(name = "GICOD", precision = 18, scale = 2) private BigDecimal gicod;
    @Column(name = "VOL", precision = 18, scale = 2) private BigDecimal vol;

    @Column(name = "DIM_0001", precision = 18, scale = 2) private BigDecimal dim0001;
    @Column(name = "DIM_0002", precision = 18, scale = 2) private BigDecimal dim0002;
    @Column(name = "DIM_0003", precision = 18, scale = 2) private BigDecimal dim0003;

    @Column(name = "SREFCOD", precision = 18, scale = 2) private BigDecimal srefcod;

    @Column(name = "OPSAIS_0001", precision = 18, scale = 2) private BigDecimal opsais0001;
    @Column(name = "OPSAIS_0002", precision = 18, scale = 2) private BigDecimal opsais0002;
    @Column(name = "OPSAIS_0003", precision = 18, scale = 2) private BigDecimal opsais0003;

    @Column(name = "ZONN", precision = 18, scale = 2) private BigDecimal zonn;
    @Column(name = "MGTX", precision = 18, scale = 2) private BigDecimal mgtx;
    @Column(name = "PERJRNB", precision = 18, scale = 2) private BigDecimal perjrnb;
    @Column(name = "STVALCOD", precision = 18, scale = 2) private BigDecimal stvalcod;
    @Column(name = "STSORCOD", precision = 18, scale = 2) private BigDecimal stsorcod;
    @Column(name = "WEBCDECOD", precision = 18, scale = 2) private BigDecimal webcdecod;
    @Column(name = "PVCOD", precision = 18, scale = 2) private BigDecimal pvcod;
    @Column(name = "LGTYP", precision = 18, scale = 2) private BigDecimal lgtyp;
    @Column(name = "STRES", precision = 18, scale = 2) private BigDecimal stres;
    @Column(name = "CDEINQTE", precision = 18, scale = 2) private BigDecimal cdeinqte;
    @Column(name = "CEJOINT", precision = 18, scale = 2) private BigDecimal cejoint;
    @Column(name = "JOINT", precision = 18, scale = 2) private BigDecimal joint;
    @Column(name = "REJALOF", precision = 18, scale = 2) private BigDecimal rejalof;
    @Column(name = "REJALCDE", precision = 18, scale = 2) private BigDecimal rejalcde;
    @Column(name = "REJALOFJRNB", precision = 18, scale = 2) private BigDecimal rejalofjrnb;
    @Column(name = "REJALCDEJRNB", precision = 18, scale = 2) private BigDecimal rejalcderjnb;
    @Column(name = "TOLERANCETX", precision = 18, scale = 2) private BigDecimal tolerancetx;
    @Column(name = "COMSAIS", precision = 18, scale = 2) private BigDecimal comsais;
    @Column(name = "SURF", precision = 18, scale = 2) private BigDecimal surf;
    @Column(name = "COEFOIVOL", precision = 18, scale = 2) private BigDecimal coefoivol;

    @Column(name = "MANUN", length = 4) private String manun;
    @Column(name = "STLGTABCCOD", length = 8) private String stlgtabccod;
    @Column(name = "STLGTHGCOD", precision = 18, scale = 2) private BigDecimal stlgthgcod;
    @Column(name = "STLGTHGCOLINB", precision = 18, scale = 2) private BigDecimal stlgthgcolinb;

    @Column(name = "RANGABC", precision = 18, scale = 2) private BigDecimal rangabc;
    @Column(name = "PDP", precision = 18, scale = 2) private BigDecimal pdp;
    @Column(name = "PDPPERCOD", precision = 18, scale = 2) private BigDecimal pdppercod;
    @Column(name = "PDPDELOBT", precision = 18, scale = 2) private BigDecimal pDPdElObt;



}
