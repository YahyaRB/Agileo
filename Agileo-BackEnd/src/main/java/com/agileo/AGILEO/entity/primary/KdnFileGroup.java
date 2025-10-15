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
@Table(name = "KDN_FILEGROUP")
public class KdnFileGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GROUPID")
    private Integer groupId;

    @Column(name = "STORAGENAME", length = 255)
    private String storageName;

    @Column(name = "GROUPSTATE")
    private Integer groupState;

    @Column(name = "SYSCREATIONDATE")
    private LocalDateTime sysCreationDate;

    @Column(name = "SYSMODIFICATIONDATE")
    private LocalDateTime sysModificationDate;

    @Column(name = "SYSSTATE")
    private Integer sysState;

    @Column(name = "CHECKOUTUSER")
    private Integer checkoutUser;

    @Column(name = "CHECKOUTHOST", length = 200)
    private String checkoutHost;

    @Column(name = "CHECKOUTURL", length = 2000)
    private String checkoutUrl;

    @Column(name = "CHECKOUTDATE")
    private LocalDateTime checkoutDate;

    @Column(name = "CHECKOUTDESCRIPTION", length = 1000)
    private String checkoutDescription;

    // SUPPRIMÉ: Pas de relation @OneToMany car cela créerait des problèmes de performance
    // Les fichiers seront récupérés via les repositories selon les besoins

    // Constructeur simplifié
    public KdnFileGroup(String storageName) {
        this.storageName = storageName;
        this.groupState = 1;
        this.sysState = 1;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (sysCreationDate == null) sysCreationDate = now;
        if (sysModificationDate == null) sysModificationDate = now;
        if (sysState == null) sysState = 1;
        if (groupState == null) groupState = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        sysModificationDate = LocalDateTime.now();
    }

    // Méthodes utilitaires
    public boolean isActive() {
        return sysState != null && sysState == 1;
    }

    public void markAsDeleted() {
        this.sysState = 0;
        this.sysModificationDate = LocalDateTime.now();
    }
}

// ==================== MISE À JOUR DEMANDEACHAT POUR INTÉGRER LES FICHIERS ====================
// Ajoutez ce champ dans votre entité DemandeAchat existante :

// Dans DemandeAchat.java, ajoutez ce champ :
/*
@Column(name = "pj_da")
private Integer pjDa; // Référence vers le groupe de fichiers
*/

// Et cette méthode utilitaire :
/*
public boolean hasAttachments() {
    return pjDa != null && pjDa > 0;
}
*/