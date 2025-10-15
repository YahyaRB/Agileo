package com.agileo.AGILEO.Dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {

    // Statistiques générales
    private StatistiquesGenerales statistiquesGenerales;

    // Demandes d'achat
    private StatistiquesDemandesAchat demandesAchat;

    // Consommations
    private StatistiquesConsommations consommations;

    // Commandes
    private StatistiquesCommandes commandes;

    // Stock
    private StatistiquesStock stock;

    // Réceptions
    private StatistiquesReceptions receptions;

    // Articles les plus demandés
    private List<ArticlePopulaire> articlesPopulaires;

    // Évolution des demandes (30 derniers jours)
    private List<EvolutionJournaliere> evolutionDemandes;

    // Affaires actives (pour chef de projet)
    private List<AffaireStats> affaires;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatistiquesGenerales {
        private Long totalAffaires;
        private Long totalUtilisateurs;
        private Long totalArticles;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatistiquesDemandesAchat {
        private Long total;
        private Long brouillon;
        private Long envoye;
        private Long recu;
        private Long rejete;
        private Double tauxValidation; // Pourcentage de demandes validées
        private Long dernierMois;
        private Double evolutionPourcentage; // Évolution par rapport au mois précédent
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatistiquesConsommations {
        private Long total;
        private Long envoye;
        private Long brouillon;
        private Long dernierMois;
        private Long lignesTotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatistiquesCommandes {
        private Long total;
        private Long enCours;
        private Long livrees;
        private Long dernierMois;
        private BigDecimal valeurTotale;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatistiquesStock {
        private Long articlesEnStock;
        private Long articlesEpuises;
        private Long articlesStockFaible; // Stock < seuil
        private BigDecimal valeurStock;
        private Double tauxRotation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatistiquesReceptions {
        private Long total;
        private Long enAttente;
        private Long integrees;
        private Long dernierMois;
        private Long lignesEnAttente;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticlePopulaire {
        private String ref;
        private String designation;
        private Long nombreDemandes;
        private BigDecimal quantiteTotale;
        private String unite;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvolutionJournaliere {
        private String date;
        private Long demandes;
        private Long consommations;
        private Long receptions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AffaireStats {
        private String code;
        private String libelle;
        private Long demandesEnCours;
        private Long commandesEnCours;
        private BigDecimal budget;
        private BigDecimal depense;
        private Double pourcentageUtilise;
    }
}