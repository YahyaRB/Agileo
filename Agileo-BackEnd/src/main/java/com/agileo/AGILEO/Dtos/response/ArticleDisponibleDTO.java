package com.agileo.AGILEO.Dtos.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ArticleDisponibleDTO {
    private String reference;
    private String designation;
    private String unite;
    private BigDecimal quantiteCommandee;
    private BigDecimal quantiteDejaRecue;
    private BigDecimal quantiteDisponible;
    private String referenceBonCommande;
    private String nomFournisseur;
    private String familleStatistique1;
    private String familleStatistique2;
    private String familleStatistique3;
    private String familleStatistique4;


}