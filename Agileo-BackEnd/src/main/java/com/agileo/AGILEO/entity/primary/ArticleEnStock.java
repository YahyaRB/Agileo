package com.agileo.AGILEO.entity.primary;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "Article_en_stock")
public class ArticleEnStock {

    @Id
    @Column(name = "REF", nullable = false, length = 25)
    private String ref;

    @Column(name = "SumStQte", precision = 38, scale = 3)
    private BigDecimal sumStQte;

    @Column(name = "DES", length = 80)
    private String description;

    @Column(name = "ACHUN", nullable = false, length = 4)
    private String achun;

    @Column(name = "DEPO", nullable = false, length = 3)
    private String depo;

    // Getters & Setters
}