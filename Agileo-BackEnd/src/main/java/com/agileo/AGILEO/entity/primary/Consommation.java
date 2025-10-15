package com.agileo.AGILEO.entity.primary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "M6000_CONSOMMATION")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Consommation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_BC") // Correspond à ID_BC (PK, int, not null)
    private Integer idBc;

    @Column(name = "chantier", length = 8) // varchar(8), null
    private String chantier;

    @Column(name = "comm") // text, null
    private String comm;

    @Column(name = "login") // int, null
    private Integer login;

    @Column(name = "date_c") // datetime, null
    private LocalDateTime dateC;

    @Column(name = "statut") // int, null
    private Integer statut;

    @Column(name = "ref_interne", length = 40) // varchar(40), null
    private String refInterne;

    @Column(name = "SYSCREATIONDATE") // datetime, not null
    private LocalDateTime sysCreationDate;

    @Column(name = "SYSCREATORID") // int, not null
    private Integer sysCreatorId;

    @Column(name = "SYSMODIFICATIONDATE") // datetime, not null
    private LocalDateTime sysModificationDate;

    @Column(name = "SYSUSERID") // int, not null
    private Integer sysUserId;

    @Column(name = "SYSSYNCHRONIZATIONDATE") // datetime, null
    private LocalDateTime sysSynchronizationDate;

    @Column(name = "SYSSTATE") // int, not null
    private Integer sysState;

    // Getters et Setters explicites

    public Integer getIdBc() {
        return idBc;
    }

    public void setIdBc(Integer idBc) {
        this.idBc = idBc;
    }

    public String getChantier() {
        return chantier;
    }

    public void setChantier(String chantier) {
        this.chantier = chantier;
    }

    public String getComm() {
        return comm;
    }

    public void setComm(String comm) {
        this.comm = comm;
    }

    public Integer getLogin() {
        return login;
    }

    public void setLogin(Integer login) {
        this.login = login;
    }

    public LocalDateTime getDateC() {
        return dateC;
    }

    public void setDateC(LocalDateTime dateC) {
        this.dateC = dateC;
    }

    public Integer getStatut() {
        return statut;
    }

    public void setStatut(Integer statut) {
        this.statut = statut;
    }

    public String getRefInterne() {
        return refInterne;
    }

    public void setRefInterne(String refInterne) {
        this.refInterne = refInterne;
    }

    // Méthodes métier adaptées
    public boolean isEnvoye() {
        return  statut == 1;
    }

    public boolean isBrouillon() {
        return statut == null || statut == 0; // Adapter selon votre logique
    }

    public void marquerCommeEnvoye() {
        this.statut = 1;
    }
}