package com.agileo.AGILEO.entity.primary;

import lombok.Data;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "M6001_Livraisons", schema = "dbo")
public class LigneReception {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Numero")
    private Integer numero;

    @Column(name = "PrefCmd", length = 10)
    private String prefCmd;

    @Column(name = "Commande")
    private Integer commande;

    @Column(name = "Article", length = 25)
    private String article;

    @Column(name = "Deseignation", length = 100)
    private String deseignation;

    @Column(name = "qte_cmd", precision = 18, scale = 3)
    private BigDecimal qteCmd;

    @Column(name = "qte_livre", precision = 18, scale = 3)
    private BigDecimal qteLivre;

    @Column(name = "Reste", precision = 18, scale = 3)
    private BigDecimal reste;

    @Column(name = "unite", length = 4)
    private String unite;

    @Column(name = "integre")
    private Integer integre;

    @Column(name = "qte", precision = 18, scale = 3)
    private BigDecimal qte;

    @Column(name = "affaire", length = 8)
    private String affaire;

    @Column(name = "Tiers", length = 20)
    private String tiers;

    @Column(name = "pj")
    private Integer pj;

    @Column(name = "commentaire", length = 250)
    private String commentaire;

    @Column(name = "ENRNO")
    private Integer enrno;

    @Column(name = "CDENRNO")
    private Integer cdenrno;

    @Column(name = "Serie", length = 50)
    private String serie;

    @Column(name = "SREF1", length = 8)
    private String sref1;

    @Column(name = "SREF2", length = 8)
    private String sref2;

    @Column(name = "VTLNO")
    private Integer vtlno;

    @Column(name = "Bl_diva")
    private Integer blDiva;

    @Column(name = "Ent_ID")
    private Integer entId;

    @Column(name = "SYSCREATIONDATE")
    private LocalDateTime sysCreationDate;

    @Column(name = "SYSCREATORID")
    private Integer sysCreatorId;

    @Column(name = "SYSMODIFICATIONDATE")
    private LocalDateTime sysModificationDate;

    @Column(name = "SYSUSERID")
    private Integer sysUserId;

    @Column(name = "SYSSYNCHRONIZATIONDATE")
    private LocalDateTime sysSynchronizationDate;

    @Column(name = "SYSSTATE")
    private Integer sysState;

    @Column(name = "Pidt")
    private LocalDateTime pldt;

    public int getIntege() {
        return integre;
    }

    public void setIntege(int i) {
        this.integre = i;
    }
}
