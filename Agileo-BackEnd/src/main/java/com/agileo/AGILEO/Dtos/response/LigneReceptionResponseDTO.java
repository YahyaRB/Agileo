package com.agileo.AGILEO.Dtos.response;

import lombok.Data;

import jakarta.persistence.Column;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LigneReceptionResponseDTO {
    private Integer id;
    private String referenceArticle;
    private String designationArticle;
    private BigDecimal quantite;
    private BigDecimal qteCmd;
    private BigDecimal qteLivre;
    private BigDecimal reste;
    private String unite;
    private String familleStatistique1;
    private String familleStatistique2;
    private String familleStatistique3;
    private String familleStatistique4;
    private LocalDateTime sysCreationDate;
    private LocalDateTime sysModificationDate;
    private String statut;


}
