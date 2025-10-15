package com.agileo.AGILEO.service;

import com.agileo.AGILEO.Dtos.response.ConsommationStatsDTO;
import com.agileo.AGILEO.Dtos.response.DaStatsDTO;
import com.agileo.AGILEO.Dtos.response.DashboardDTO;
import com.agileo.AGILEO.entity.primary.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public interface DashboardService {


     DashboardDTO.StatistiquesGenerales getStatistiquesGenerales() ;
     DashboardDTO.StatistiquesDemandesAchat getStatistiquesDemandesAchat(List<String> affaires) ;
     DashboardDTO.StatistiquesConsommations getStatistiquesConsommations(List<String> affaires);
     DashboardDTO.StatistiquesCommandes getStatistiquesCommandes(List<String> affaires);
     DashboardDTO.StatistiquesStock getStatistiquesStock() ;
     DashboardDTO.StatistiquesReceptions getStatistiquesReceptions(List<String> affaires) ;
     List<DashboardDTO.ArticlePopulaire> getArticlesPopulaires(List<String> affaires, int limit) ;
     List<DashboardDTO.EvolutionJournaliere> getEvolutionDemandes(List<String> affaires, int jours) ;
     List<DashboardDTO.AffaireStats> getStatistiquesAffaires(List<String> affaires) ;
     List<String> getAffairesUtilisateur(Integer accessorId) ;
     DashboardDTO getStatistiquesUtilisateur(Integer accessorId);
     DashboardDTO getStatistiquesAdmin();
     DaStatsDTO getDaStatsGlobal();
     DaStatsDTO getDaStatsByLogin(Integer login);

     // Stats Consommation
     ConsommationStatsDTO getConsommationStatsGlobal();
     ConsommationStatsDTO getConsommationStatsByLogin(String login);
     Long countTotalDAByLogin(Integer login);
     Long countTotalConsommationByLogin(Integer login);
     Long countTotalReceptionByLogin(Integer login);
}
