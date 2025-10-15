package com.agileo.AGILEO.entity.primary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Fournisseurs")
public class Fournisseur {

    @Id
    @Column(name = "FOU_ID", nullable = false)
    private Integer fouId;

    @Column(name = "TIERS", nullable = false, length = 20)
    private String tiers;

    @Column(name = "NOM", nullable = false, length = 80)
    private String nom;

}