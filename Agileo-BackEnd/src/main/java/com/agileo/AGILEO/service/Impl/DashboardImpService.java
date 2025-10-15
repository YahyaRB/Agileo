package com.agileo.AGILEO.service.Impl;
import com.agileo.AGILEO.Dtos.response.AffaireResponseDTO;
import com.agileo.AGILEO.Dtos.response.ConsommationStatsDTO;
import com.agileo.AGILEO.Dtos.response.DaStatsDTO;
import com.agileo.AGILEO.Dtos.response.DashboardDTO;
import com.agileo.AGILEO.entity.primary.*;
import com.agileo.AGILEO.repository.primary.*;
import com.agileo.AGILEO.service.AffaireService;
import com.agileo.AGILEO.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardImpService implements DashboardService {
    private final DemandeAchatRepository demandeAchatRepository;
    private final ConsommationRepository consommationRepository;
    private final CommandeRepository commandeRepository;
    private final ArticleEnStockRepository articleEnStockRepository;
    private final LigneReceptionRepository ligneReceptionRepository;
    private final LigneDemandeAchatRepository ligneDemandeAchatRepository;
    private final AffaireRepository affaireRepository;
    private final KdnsAccessorRepository kdnsAccessorRepository;
    private final ReceptionRepository receptionRepository;
    private final LigneConsommationRepository ligneConsommationRepository;
    private final AffaireLieUserRepository affaireLieUserRepository;
    private final AffaireService affaireService;

    /**
     * Récupère les statistiques pour l'admin (toutes les données)
     */
    @Override
    public DashboardDTO getStatistiquesAdmin() {
        DashboardDTO dashboard = new DashboardDTO();

        dashboard.setStatistiquesGenerales(getStatistiquesGenerales());
        dashboard.setDemandesAchat(getStatistiquesDemandesAchat(null));
        dashboard.setConsommations(getStatistiquesConsommations(null));
        dashboard.setCommandes(getStatistiquesCommandes(null));
        dashboard.setStock(getStatistiquesStock());
        dashboard.setReceptions(getStatistiquesReceptions(null));
        dashboard.setArticlesPopulaires(getArticlesPopulaires(null, 10));
        dashboard.setEvolutionDemandes(getEvolutionDemandes(null, 30));

        return dashboard;
    }

    /**
     * Récupère les statistiques pour un chef de projet / magasinier
     */
    @Override
    public DashboardDTO getStatistiquesUtilisateur(Integer accessorId) {
        DashboardDTO dashboard = new DashboardDTO();

        // Récupérer les affaires de l'utilisateur
        List<String> affaires = getAffairesUtilisateur(accessorId);

        dashboard.setStatistiquesGenerales(getStatistiquesGenerales());
        dashboard.setDemandesAchat(getStatistiquesDemandesAchat(affaires));
        dashboard.setConsommations(getStatistiquesConsommations(affaires));
        dashboard.setCommandes(getStatistiquesCommandes(affaires));
        dashboard.setStock(getStatistiquesStock());
        dashboard.setReceptions(getStatistiquesReceptions(affaires));
        dashboard.setArticlesPopulaires(getArticlesPopulaires(affaires, 10));
        dashboard.setEvolutionDemandes(getEvolutionDemandes(affaires, 30));
        dashboard.setAffaires(getStatistiquesAffaires(affaires));

        return dashboard;
    }
    @Override
    public DashboardDTO.StatistiquesGenerales getStatistiquesGenerales() {
        return new DashboardDTO.StatistiquesGenerales(
                affaireRepository.count(),
                kdnsAccessorRepository.count(),
                articleEnStockRepository.count()
        );
    }
    @Override
    public DashboardDTO.StatistiquesDemandesAchat getStatistiquesDemandesAchat(List<String> affaires) {
        List<DemandeAchat> demandes;
        if (affaires != null && !affaires.isEmpty()) {
            demandes = demandeAchatRepository.findByChantierIn(affaires);
        } else {
            demandes = demandeAchatRepository.findAll();
        }

        long total = demandes.size();
        long brouillon = demandes.stream().filter(DemandeAchat::isBrouillon).count();
        long envoye = demandes.stream().filter(DemandeAchat::isEnvoye).count();
        long recu = demandes.stream().filter(DemandeAchat::isRecu).count();
        long rejete = demandes.stream().filter(DemandeAchat::isRejete).count();

        LocalDateTime debutMois = LocalDateTime.now().minusMonths(1);
        long dernierMois = demandes.stream()
                .filter(d -> d.getDateDa() != null && d.getDateDa().isAfter(debutMois))
                .count();

        LocalDateTime debutMoisPrecedent = LocalDateTime.now().minusMonths(2);
        long moisPrecedent = demandes.stream()
                .filter(d -> d.getDateDa() != null &&
                        d.getDateDa().isAfter(debutMoisPrecedent) &&
                        d.getDateDa().isBefore(debutMois))
                .count();

        double evolution = 0.0;
        if (moisPrecedent > 0) {
            evolution = ((double)(dernierMois - moisPrecedent) / moisPrecedent) * 100;
        }

        double tauxValidation = total > 0 ? ((double)recu / total) * 100 : 0.0;

        return new DashboardDTO.StatistiquesDemandesAchat(
                total, brouillon, envoye, recu, rejete,
                Math.round(tauxValidation * 100.0) / 100.0,
                dernierMois,
                Math.round(evolution * 100.0) / 100.0
        );
    }
    @Override
    public DashboardDTO.StatistiquesConsommations getStatistiquesConsommations(List<String> affaires) {
        List<Consommation> consommations;
        if (affaires != null && !affaires.isEmpty()) {
            consommations = consommationRepository.findByChantierIn(affaires);
        } else {
            consommations = consommationRepository.findAll();
        }

        long total = consommations.size();

        // Vérifier directement le statut au lieu d'appeler isEnvoye()
        long envoye = consommations.stream()
                .filter(c -> c.getStatut() != null && c.getStatut() == 1)
                .count();

        // Vérifier directement le statut au lieu d'appeler isBrouillon()
        long brouillon = consommations.stream()
                .filter(c -> c.getStatut() == null || c.getStatut() == 0)
                .count();

        LocalDateTime debutMois = LocalDateTime.now().minusMonths(1);
        long dernierMois = consommations.stream()
                .filter(c -> c.getDateC() != null && c.getDateC().isAfter(debutMois))
                .count();

        // Compter les lignes de consommation avec vérification null
        List<Integer> consommationIds = consommations.stream()
                .map(Consommation::getIdBc)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        long lignesTotal = consommationIds.isEmpty() ? 0 :
                ligneConsommationRepository.countByNumConsIn(consommationIds);

        return new DashboardDTO.StatistiquesConsommations(
                total, envoye, brouillon, dernierMois, lignesTotal
        );
    }
    @Override
    public DashboardDTO.StatistiquesCommandes getStatistiquesCommandes(List<String> affaires) {
        List<Commande> commandes;
        if (affaires != null && !affaires.isEmpty()) {
            commandes = commandeRepository.findByAffaireCodeIn(affaires);
        } else {
            commandes = commandeRepository.findAll();
        }

        long total = commandes.size();

        LocalDateTime debutMois = LocalDateTime.now().minusMonths(1);
        long dernierMois = commandes.stream()
                .filter(c -> c.getDateCommande() != null &&
                        c.getDateCommande().isAfter(debutMois.toLocalDate()))
                .count();

        // Commandes livrées vs en cours (basé sur les réceptions)
        long livrees = 0;
        long enCours = total;

        return new DashboardDTO.StatistiquesCommandes(
                total, enCours, livrees, dernierMois, BigDecimal.ZERO
        );
    }
    @Override
    public DashboardDTO.StatistiquesStock getStatistiquesStock() {
        List<ArticleEnStock> articles = articleEnStockRepository.findAll();

        long articlesEnStock = articles.stream()
                .filter(a -> a.getSumStQte() != null && a.getSumStQte().compareTo(BigDecimal.ZERO) > 0)
                .count();

        long articlesEpuises = articles.stream()
                .filter(a -> a.getSumStQte() == null || a.getSumStQte().compareTo(BigDecimal.ZERO) <= 0)
                .count();

        long articlesStockFaible = articles.stream()
                .filter(a -> a.getSumStQte() != null &&
                        a.getSumStQte().compareTo(BigDecimal.TEN) > 0 &&
                        a.getSumStQte().compareTo(new BigDecimal(50)) < 0)
                .count();

        return new DashboardDTO.StatistiquesStock(
                articlesEnStock, articlesEpuises, articlesStockFaible,
                BigDecimal.ZERO, 0.0
        );
    }
    @Override
    public DashboardDTO.StatistiquesReceptions getStatistiquesReceptions(List<String> affaires) {
        List<LigneReception> lignes;
        if (affaires != null && !affaires.isEmpty()) {
            lignes = ligneReceptionRepository.findByAffaireIn(affaires);
        } else {
            lignes = ligneReceptionRepository.findAll();
        }

        long total = lignes.size();

        // Vérifier directement le champ integre au lieu d'appeler getIntege()
        long integrees = lignes.stream()
                .filter(l -> l.getIntegre() != null && l.getIntegre() == 1)
                .count();

        long enAttente = lignes.stream()
                .filter(l -> l.getIntegre() == null || l.getIntegre() == 0)
                .count();

        LocalDateTime debutMois = LocalDateTime.now().minusMonths(1);
        long dernierMois = lignes.stream()
                .filter(l -> l.getSysCreationDate() != null &&
                        l.getSysCreationDate().isAfter(debutMois))
                .count();

        return new DashboardDTO.StatistiquesReceptions(
                total, enAttente, integrees, dernierMois, enAttente
        );
    }
    @Override
    public List<DashboardDTO.ArticlePopulaire> getArticlesPopulaires(List<String> affaires, int limit) {
        List<LigneDemandeAchat> lignes;
        if (affaires != null && !affaires.isEmpty()) {
            lignes = ligneDemandeAchatRepository.findByDemandeAchat_ChantierIn(affaires);
        } else {
            lignes = ligneDemandeAchatRepository.findAll();
        }

        Map<String, List<LigneDemandeAchat>> groupedByRef = lignes.stream()
                .filter(l -> l.getRef() != null)
                .collect(Collectors.groupingBy(LigneDemandeAchat::getRef));

        return groupedByRef.entrySet().stream()
                .map(entry -> {
                    List<LigneDemandeAchat> lignesArticle = entry.getValue();
                    LigneDemandeAchat first = lignesArticle.get(0);

                    long nombreDemandes = lignesArticle.size();
                    BigDecimal quantiteTotale = lignesArticle.stream()
                            .map(LigneDemandeAchat::getQte)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new DashboardDTO.ArticlePopulaire(
                            first.getRef(),
                            first.getDesignation(),
                            nombreDemandes,
                            quantiteTotale,
                            first.getUnite()
                    );
                })
                .sorted(Comparator.comparing(DashboardDTO.ArticlePopulaire::getNombreDemandes).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    @Override
    public List<DashboardDTO.EvolutionJournaliere> getEvolutionDemandes(List<String> affaires, int jours) {
        LocalDateTime debut = LocalDateTime.now().minusDays(jours);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        List<DemandeAchat> demandes;
        if (affaires != null && !affaires.isEmpty()) {
            demandes = demandeAchatRepository.findByChantierInAndDateDaAfter(affaires, debut);
        } else {
            demandes = demandeAchatRepository.findByDateDaAfter(debut);
        }

        List<Consommation> consommations;
        if (affaires != null && !affaires.isEmpty()) {
            consommations = consommationRepository.findByChantierInAndDateCAfter(affaires, debut);
        } else {
            consommations = consommationRepository.findByDateCAfter(debut);
        }

        List<LigneReception> receptions;
        if (affaires != null && !affaires.isEmpty()) {
            receptions = ligneReceptionRepository.findByAffaireInAndSysCreationDateAfter(affaires, debut);
        } else {
            receptions = ligneReceptionRepository.findBySysCreationDateAfter(debut);
        }

        Map<String, Long> demandesParJour = demandes.stream()
                .filter(d -> d.getDateDa() != null)
                .collect(Collectors.groupingBy(
                        d -> d.getDateDa().toLocalDate().format(formatter),
                        Collectors.counting()
                ));

        Map<String, Long> consommationsParJour = consommations.stream()
                .filter(c -> c.getDateC() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getDateC().toLocalDate().format(formatter),
                        Collectors.counting()
                ));

        Map<String, Long> receptionsParJour = receptions.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getSysCreationDate().toLocalDate().format(formatter),
                        Collectors.counting()
                ));

        List<DashboardDTO.EvolutionJournaliere> evolution = new ArrayList<>();
        for (int i = jours - 1; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            String dateStr = date.toLocalDate().format(formatter);

            evolution.add(new DashboardDTO.EvolutionJournaliere(
                    dateStr,
                    demandesParJour.getOrDefault(dateStr, 0L),
                    consommationsParJour.getOrDefault(dateStr, 0L),
                    receptionsParJour.getOrDefault(dateStr, 0L)
            ));
        }

        return evolution;
    }
    @Override
    public List<DashboardDTO.AffaireStats> getStatistiquesAffaires(List<String> affaires) {
        if (affaires == null || affaires.isEmpty()) {
            return new ArrayList<>();
        }

        return affaires.stream().map(affaireCode -> {
                    Affaire affaire = affaireRepository.findById(affaireCode).orElse(null);
                    if (affaire == null) {
                        return null;
                    }

                    long demandesEnCours = demandeAchatRepository.countByChantierAndStatutNot(affaireCode, 2);
                    long commandesEnCours = commandeRepository.countByAffaireCode(affaireCode);

                    return new DashboardDTO.AffaireStats(
                            affaire.getAffaire(),
                            affaire.getLibelle(),
                            demandesEnCours,
                            commandesEnCours,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            0.0
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    @Override
    public List<String> getAffairesUtilisateur(Integer accessorId) {
        List<AffaireLieUser> affairesLiees = affaireLieUserRepository.findByAccessoirId(accessorId);
        return affairesLiees.stream()
                .map(AffaireLieUser::getAffaire)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    @Override
    public DaStatsDTO getDaStatsGlobal() {
        return calculateDaStats(null);
    }

    @Override
    public DaStatsDTO getDaStatsByLogin(Integer login) {
        return calculateDaStats(login);
    }

    private DaStatsDTO calculateDaStats(Integer login) {
        DaStatsDTO dto = new DaStatsDTO();

        if (login == null) {
            // Logique Globale (pour Magasinier/Admin)
            dto.setTotalDemandes(demandeAchatRepository.count());
            dto.setEnAttente(demandeAchatRepository.countByStatut(0));
            dto.setApprouvees(demandeAchatRepository.countByStatut(1));
            dto.setRejetee(demandeAchatRepository.countByStatut(-1));
        } else {
            // Logique par Utilisateur (Chef de Projet/Consommateur)
            // On compte les DA de cet utilisateur
            dto.setTotalDemandes(demandeAchatRepository.countByLogin(login));
            dto.setEnAttente(demandeAchatRepository.countByLoginAndStatut(login, 0));
            dto.setApprouvees(demandeAchatRepository.countByLoginAndStatut(login, 1));
            dto.setRejetee(demandeAchatRepository.countByLoginAndStatut(login, -1));
        }
        return dto;
    }

    // ==================== STATS CONSOMMATION ====================

    @Override
    public ConsommationStatsDTO getConsommationStatsGlobal() {
        ConsommationStatsDTO dto = new ConsommationStatsDTO();
        dto.setTotalConsommations(consommationRepository.count());
        dto.setTotalLignes(ligneConsommationRepository.count());
        return dto;
    }

    @Override
    public ConsommationStatsDTO getConsommationStatsByLogin(String login) {
        ConsommationStatsDTO dto = new ConsommationStatsDTO();

        // 1. Récupérer les codes affaires liés à cet utilisateur
        // UTILISATION CORRECTE DE affaireService
        List<String> affaires = affaireService.findAffairesByAccessorLogin(login)
                .stream()
                .map(AffaireResponseDTO::getAffaire) // Récupère le code affaire
                .collect(Collectors.toList());

        if (affaires.isEmpty()) {
            dto.setTotalConsommations(0L);
            dto.setTotalLignes(0L);
            return dto;
        }

        // 2. Compter les consommations liées à ces affaires
        dto.setTotalConsommations(consommationRepository.countByChantierIn(affaires));

        // 3. Compter les lignes de consommation liées à ces affaires
        dto.setTotalLignes(ligneConsommationRepository.countByAffaireIn(affaires));

        return dto;
    }

    @Override
    public Long countTotalDAByLogin(Integer login) {
        return demandeAchatRepository.countTotalDAByLogin(login);
    }

    @Override
    public Long countTotalConsommationByLogin(Integer login) {
        return consommationRepository.countTotalConsommationByLogin(login);
    }

    @Override
    public Long countTotalReceptionByLogin(Integer login) {
        return receptionRepository.countTotalReceptionByLogin(login);
    }
}