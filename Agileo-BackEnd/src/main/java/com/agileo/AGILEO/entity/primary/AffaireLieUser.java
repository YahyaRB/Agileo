package com.agileo.AGILEO.entity.primary;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Data
@Entity
@Table(name = "Affaires_lies_users")
public class AffaireLieUser {

    @Id
    @Column(name = "ACCESSOIRID", nullable = false)
    private Integer accessoirId;

    @Column(name = "Affaire", length = 50)
    private String affaire;

    @Column(name = "LIB", length = 80)
    private String libelle;

}