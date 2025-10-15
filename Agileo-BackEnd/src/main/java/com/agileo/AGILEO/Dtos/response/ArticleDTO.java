package com.agileo.AGILEO.Dtos.response;


import lombok.Data;

@Data
public class ArticleDTO {
    private Integer id;
    private String reference;
    private String designation;
    private String unite;
    private String familleStatistique1;
    private String familleStatistique2;
    private String familleStatistique3;
    private String familleStatistique4;

    // Getters et setters...
}