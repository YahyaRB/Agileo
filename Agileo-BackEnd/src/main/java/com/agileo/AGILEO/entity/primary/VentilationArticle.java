package com.agileo.AGILEO.entity.primary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "Ventilation")
public class VentilationArticle {
    @Id
    @Column(name = "REF", nullable = false, length = 25)
    private String ref;

    @Column(name = "DEPO", length = 80)
    private String depot;

    @Column(name = "ENRNO")
    private Integer enrno;

    @Column(name = "VTLNO")
    private Integer vtlno;
}