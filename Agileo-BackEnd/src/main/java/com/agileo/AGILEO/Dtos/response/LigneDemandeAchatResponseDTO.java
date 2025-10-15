package com.agileo.AGILEO.Dtos.response;

import lombok.Data;

import jakarta.persistence.Column;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LigneDemandeAchatResponseDTO {
    private Integer idArt;
    private String ref;
    private String designation;
    private BigDecimal qte;
    private String unite;
    private String fam0001;
    private String fam0002;
    private String fam0003;
    private String sref1;
    private String sref2;
    private Integer da;
    private LocalDateTime sysCreationDate;
    private Integer sysCreatorId;
    private LocalDateTime sysModificationDate;
    private Integer sysUserId;

}
