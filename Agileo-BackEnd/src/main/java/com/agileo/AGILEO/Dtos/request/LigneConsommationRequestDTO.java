package com.agileo.AGILEO.Dtos.request;

import lombok.Data;

@Data
public class LigneConsommationRequestDTO {
    private String referenceArticle;
    private String designationArticle;
    private Double quantite;
    private String unite;
    private String familleStatistique1;
    private String familleStatistique2;
}