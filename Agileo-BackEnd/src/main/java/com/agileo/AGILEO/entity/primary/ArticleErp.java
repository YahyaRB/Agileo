package com.agileo.AGILEO.entity.primary;


import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Data
@Table(name = "Articles_erp") // nom de la vue
public class ArticleErp {

    @Id
    @Column(name = "REF", nullable = false, length = 25)
    private String ref;

    @Column(name = "DES", length = 80)
    private String description;

    @Column(name = "CodeFam1", length = 8, nullable = false)
    private String codeFam1;

    @Column(name = "CodeFam2", length = 8, nullable = false)
    private String codeFam2;

    @Column(name = "CodeFam3", length = 8, nullable = false)
    private String codeFam3;

    @Column(name = "ACHUN", length = 4, nullable = false)
    private String achun;

    @Column(name = "FAM_0001", length = 40)
    private String fam0001;

    @Column(name = "FAM_0002", length = 40)
    private String fam0002;

    @Column(name = "FAM_0003", length = 40)
    private String fam0003;

}
