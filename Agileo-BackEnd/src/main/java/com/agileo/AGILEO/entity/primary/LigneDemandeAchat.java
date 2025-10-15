package com.agileo.AGILEO.entity.primary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "M6000_ARTICLE") // Suppression de schema et catalog
public class LigneDemandeAchat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Pour auto-increment si nécessaire
    @Column(name = "ID_ART")
    private Integer idArt;

    @Column(name = "REF", length = 25)
    private String ref;

    @Column(name = "DES", length = 80)
    private String designation;

    @Column(name = "FAM_0001", length = 8)
    private String fam0001;

    @Column(name = "FAM_0002", length = 8)
    private String fam0002;

    @Column(name = "FAM_0003", length = 8)
    private String fam0003;

    @Column(name = "QTE", precision = 18, scale = 2)
    private BigDecimal qte;

    @Column(name = "DA")
    private Integer da;

    @Column(name = "unite", length = 4)
    private String unite;

    @Column(name = "SREF1", length = 8)
    private String sref1;

    @Column(name = "SREF2", length = 8)
    private String sref2;

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

    // Relation avec DemandeAchat - Ajout de insertable=false, updatable=false car DA est déjà mappé
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DA", referencedColumnName = "id", insertable = false, updatable = false)
    private DemandeAchat demandeAchat;

    // Constructeurs personnalisés
    public LigneDemandeAchat(String ref, String designation, BigDecimal qte, Integer da, String unite) {
        this.ref = ref;
        this.designation = designation;
        this.qte = qte;
        this.da = da;
        this.unite = unite;
        this.sysCreationDate = LocalDateTime.now();
        this.sysState = 0;
    }

    public LigneDemandeAchat(String ref, String designation, BigDecimal qte, Integer da, String unite,
                             String fam0001, String fam0002, String fam0003) {
        this(ref, designation, qte, da, unite);
        this.fam0001 = fam0001;
        this.fam0002 = fam0002;
        this.fam0003 = fam0003;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (sysCreationDate == null) {
            sysCreationDate = now;
        }
        if (sysModificationDate == null) {
            sysModificationDate = now;
        }
        if (sysCreatorId == null) {
            sysCreatorId = 1; // Valeur par défaut
        }
        if (sysState == null) {
            sysState = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        sysModificationDate = LocalDateTime.now();
    }

    // Méthodes utilitaires
    public boolean isActive() {
        return sysState == null || sysState == 0;
    }

    public void markAsDeleted() {
        this.sysState = -1;
        this.sysModificationDate = LocalDateTime.now();
    }

    public boolean hasValidQuantity() {
        return qte != null && qte.compareTo(BigDecimal.ZERO) > 0;
    }

    public String getFullDescription() {
        StringBuilder description = new StringBuilder();
        if (ref != null && !ref.isEmpty()) {
            description.append("[").append(ref).append("] ");
        }
        if (designation != null && !designation.isEmpty()) {
            description.append(designation);
        }
        return description.toString();
    }

    public String getCategoryPath() {
        StringBuilder path = new StringBuilder();
        if (fam0001 != null && !fam0001.isEmpty()) {
            path.append(fam0001);
        }
        if (fam0002 != null && !fam0002.isEmpty()) {
            if (path.length() > 0) path.append(" > ");
            path.append(fam0002);
        }
        if (fam0003 != null && !fam0003.isEmpty()) {
            if (path.length() > 0) path.append(" > ");
            path.append(fam0003);
        }
        return path.toString();
    }

    public void updateQuantity(BigDecimal newQuantity, Integer userId) {
        if (newQuantity != null && newQuantity.compareTo(BigDecimal.ZERO) >= 0) {
            this.qte = newQuantity;
            this.sysUserId = userId;
            this.sysModificationDate = LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return "LigneDemandeAchat{" +
                "idArt=" + idArt +
                ", ref='" + ref + '\'' +
                ", designation='" + designation + '\'' +
                ", qte=" + qte +
                ", unite='" + unite + '\'' +
                ", da=" + da +
                '}';
    }
}