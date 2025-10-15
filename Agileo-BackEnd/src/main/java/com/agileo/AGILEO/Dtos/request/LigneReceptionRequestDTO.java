package com.agileo.AGILEO.Dtos.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LigneReceptionRequestDTO {
    private String referenceArticle;
    private String designationArticle;
    private BigDecimal quantite;
    private String unite;
    private String familleStatistique1;
    private String familleStatistique2;
    private String familleStatistique3;
    private String familleStatistique4;
    private Integer ENRNO;
    private Integer VTLNO;
}