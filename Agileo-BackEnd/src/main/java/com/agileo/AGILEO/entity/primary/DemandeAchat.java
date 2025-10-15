package com.agileo.AGILEO.entity.primary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "M6000_DemandeAchat")
public class DemandeAchat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "chantier", length = 8, nullable = false)
    private String chantier;

    // SUPPRIMÉ : article et quantite
    // Ces informations sont maintenant dans les lignes de demande

    @Column(name = "delai_souhaite", nullable = false)
    private LocalDateTime delaiSouhaite;

    @Column(name = "comm", length = 8000)
    private String commentaire;

    @Column(name = "login", nullable = false)
    private Integer login;

    @Column(name = "date_da")
    private LocalDateTime dateDa;

    @Column(name = "statut")
    private Integer statut;

    @Column(name = "num_da", length = 100)
    private String numDa;

    @Column(name = "da_divalto", length = 40)
    private String dsDivalto;

    @Column(name = "pj_da")
    private Integer pjDa;

    // Champs système
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

    // Constructeur simplifié
    public DemandeAchat(String chantier, LocalDateTime delaiSouhaite, String commentaire, Integer login) {
        this.chantier = chantier;
        this.delaiSouhaite = delaiSouhaite;
        this.commentaire = commentaire;
        this.login = login;
        this.dateDa = LocalDateTime.now();
        this.statut = 0;
        this.sysCreationDate = LocalDateTime.now();
        this.sysCreatorId = login;
        this.sysState = 0;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (sysCreationDate == null) sysCreationDate = now;
        if (sysModificationDate == null) sysModificationDate = now;
        if (dateDa == null) dateDa = now;
        if (sysCreatorId == null) sysCreatorId = login;
        if (sysState == null) sysState = 0;
        if (statut == null) statut = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        sysModificationDate = LocalDateTime.now();
    }

    // Dans DemandeAchat.java - Remplacer la méthode getStatutLabel()

    public String getStatutLabel() {
        if (statut == null || statut == 0) return "Brouillon";
        if (statut == 1) return "Envoyé";
        if (statut == 2) return "Reçu";
        if (statut == -1) return "Rejeté";
        return "Inconnu";
    }

    public boolean isBrouillon() {
        return statut == null || statut == 0;
    }

    public boolean isEnvoye() {
        return statut != null && statut == 1;
    }

    public boolean isRecu() {
        return statut != null && statut == 2;
    }

    public boolean isRejete() {
        return statut != null && statut == -1;
    }

    public boolean isPending() {
        return isBrouillon();
    }
}