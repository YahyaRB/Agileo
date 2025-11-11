package com.agileo.AGILEO.entity.primary;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Data
@Table(name = "AffairesDisplay") // Vue sans filtre SAISAUTTYP_0001
public class AffaireDisplay {

    @Id
    @Column(name = "AFFAIRE", nullable = false, length = 8)
    private String affaire;

    @Column(name = "LIB80", nullable = false, length = 80)
    private String libelle;

}