package com.agileo.AGILEO.entity.primary;


import lombok.Data;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "M6003_Affaire_Utilisateur")
public class M6003AffaireUtilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Numero", nullable = false)
    private Integer numero;

    @Column(name = "ACCESSOIRID", nullable = true)
    private Integer accessoirId;

    @Column(name = "Affaire", length = 50, nullable = true)
    private String affaire;

    @Column(name = "Commentaire", length = 200, nullable = true)
    private String commentaire;

    @Column(name = "SYSCREATIONDATE", nullable = false)
    private LocalDateTime sysCreationDate;

    @Column(name = "SYSCREATORID", nullable = false)
    private Integer sysCreatorId;

    @Column(name = "SYSMODIFICATIONDATE", nullable = false)
    private LocalDateTime sysModificationDate;

    @Column(name = "SYSUSERID", nullable = false)
    private Integer sysUserId;

    @Column(name = "SYSSYNCHRONIZATIONDATE", nullable = true)
    private LocalDateTime sysSynchronizationDate;

    @Column(name = "SYSSTATE", nullable = true)
    private Integer sysState;

    // Constructeurs
    public M6003AffaireUtilisateur() {
    }

    public M6003AffaireUtilisateur(String affaire, String commentaire, Integer sysCreatorId, Integer sysUserId) {
        this.affaire = affaire;
        this.commentaire = commentaire;
        this.sysCreatorId = sysCreatorId;
        this.sysUserId = sysUserId;
        this.sysCreationDate = LocalDateTime.now();
        this.sysModificationDate = LocalDateTime.now();
    }

    // Getters et Setters
    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public Integer getAccessoirId() {
        return accessoirId;
    }

    public void setAccessoirId(Integer accessoirId) {
        this.accessoirId = accessoirId;
    }

    public String getAffaire() {
        return affaire;
    }

    public void setAffaire(String affaire) {
        this.affaire = affaire;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public LocalDateTime getSysCreationDate() {
        return sysCreationDate;
    }

    public void setSysCreationDate(LocalDateTime sysCreationDate) {
        this.sysCreationDate = sysCreationDate;
    }

    public Integer getSysCreatorId() {
        return sysCreatorId;
    }

    public void setSysCreatorId(Integer sysCreatorId) {
        this.sysCreatorId = sysCreatorId;
    }

    public LocalDateTime getSysModificationDate() {
        return sysModificationDate;
    }

    public void setSysModificationDate(LocalDateTime sysModificationDate) {
        this.sysModificationDate = sysModificationDate;
    }

    public Integer getSysUserId() {
        return sysUserId;
    }

    public void setSysUserId(Integer sysUserId) {
        this.sysUserId = sysUserId;
    }

    public LocalDateTime getSysSynchronizationDate() {
        return sysSynchronizationDate;
    }

    public void setSysSynchronizationDate(LocalDateTime sysSynchronizationDate) {
        this.sysSynchronizationDate = sysSynchronizationDate;
    }

    public Integer getSysState() {
        return sysState;
    }

    public void setSysState(Integer sysState) {
        this.sysState = sysState;
    }

    // Méthodes utilitaires
    @PrePersist
    protected void onCreate() {
        if (sysCreationDate == null) {
            sysCreationDate = LocalDateTime.now();
        }
        if (sysModificationDate == null) {
            sysModificationDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        sysModificationDate = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "M6003AffaireUtilisateur{" +
                "numero=" + numero +
                ", accessoirId=" + accessoirId +
                ", affaire='" + affaire + '\'' +
                ", commentaire='" + commentaire + '\'' +
                ", sysCreationDate=" + sysCreationDate +
                ", sysCreatorId=" + sysCreatorId +
                ", sysModificationDate=" + sysModificationDate +
                ", sysUserId=" + sysUserId +
                ", sysSynchronizationDate=" + sysSynchronizationDate +
                ", sysState=" + sysState +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof M6003AffaireUtilisateur)) return false;
        M6003AffaireUtilisateur that = (M6003AffaireUtilisateur) o;
        return numero != null && numero.equals(that.numero);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
