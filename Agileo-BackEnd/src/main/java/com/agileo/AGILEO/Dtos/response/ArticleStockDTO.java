package com.agileo.AGILEO.Dtos.response;

import lombok.Data;

@Data
public class ArticleStockDTO {
    private String referenceArticle;
    private String designationArticle;
    private String unite;
    private Double stockDisponible;
    private Double totalRecu;
    private Double totalConsomme;
    private String familleStatistique1;
    private String familleStatistique2;
    private String familleStatistique3;
    private String familleStatistique4;

    public ArticleStockDTO(String referenceArticle, String designationArticle, String unite,
                           Double stockDisponible, Double totalRecu, Double totalConsomme) {
        this.referenceArticle = referenceArticle;
        this.designationArticle = designationArticle;
        this.unite = unite;
        this.stockDisponible = stockDisponible;
        this.totalRecu = totalRecu;
        this.totalConsomme = totalConsomme;
    }
}
