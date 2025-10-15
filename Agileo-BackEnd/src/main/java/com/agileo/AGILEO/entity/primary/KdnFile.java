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
@Table(name = "KDN_FILE")
public class KdnFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FILEID")
    private Integer fileId;

    @Column(name = "GROUPID", nullable = false)
    private Integer groupId;

    @Column(name = "NAME", length = 256, nullable = false)
    private String name;

    @Column(name = "EXTENSION", length = 10)
    private String extension;

    @Column(name = "ALT", length = 512)
    private String alt;

    @Column(name = "SIZE")
    private Integer size;

    @Column(name = "NBOPEN")
    private Integer nbOpen;

    @Column(name = "DOCUMENTTYPE")
    private Integer documentType;

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

    @Column(name = "STORAGENAME", length = 200)
    private String storageName;

    @Column(name = "HASH", length = 128)
    private String hash;

    @Column(name = "HASHTYPE", length = 10)
    private String hashType;

    @Column(name = "CHECKOUTPATH", length = 2000)
    private String checkoutPath;

    @Column(name = "TEMPPATH", length = 512)
    private String tempPath;

    // CORRECTION CRITIQUE: Ajouter le champ FTPPATH manquant
    @Column(name = "FTPPATH", length = 2000, nullable = false)
    private String ftpPath;

    // Constructeur simplifié mis à jour
    public KdnFile(Integer groupId, String name, String extension, Integer size,
                   String storageName, String hash, String hashType, String tempPath, Integer creatorId) {
        this.groupId = groupId;
        this.name = name;
        this.extension = extension;
        this.size = size;
        this.storageName = storageName;
        this.hash = hash;
        this.hashType = hashType;
        this.tempPath = tempPath;
        this.ftpPath = ""; // CORRECTION: Initialiser avec une valeur par défaut
        this.nbOpen = 0;
        this.documentType = 1;
        this.sysCreatorId = creatorId;
        this.sysUserId = creatorId;
        this.sysState = 1;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (sysCreationDate == null) sysCreationDate = now;
        if (sysModificationDate == null) sysModificationDate = now;
        if (sysState == null) sysState = 1;
        if (nbOpen == null) nbOpen = 0;
        if (documentType == null) documentType = 1;

        // CORRECTION: S'assurer que ftpPath n'est jamais null
        if (ftpPath == null) ftpPath = "";
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

    public String getFullFileName() {
        if (extension != null && !extension.isEmpty()) {
            return name + "." + extension;
        }
        return name;
    }

    public String getSizeFormatted() {
        if (size == null) return "0 B";

        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double fileSize = size;

        while (fileSize >= 1024 && unitIndex < units.length - 1) {
            fileSize /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", fileSize, units[unitIndex]);
    }
}