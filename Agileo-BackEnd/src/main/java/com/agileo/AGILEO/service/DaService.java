package com.agileo.AGILEO.service;

import com.agileo.AGILEO.Dtos.response.AffaireReceptionResponseDTO;
import com.agileo.AGILEO.Dtos.response.ArticleDTO;
import com.agileo.AGILEO.Dtos.response.CommandeResponseDTO;
import com.agileo.AGILEO.Dtos.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DaService {

    // ==================== NOUVELLE MÉTHODE UNIFIÉE ====================

    /**
     * ✅ Recherche d'articles avec filtres optionnels
     * @param search Terme de recherche (optionnel)
     * @param familleCode Code de famille statistique (optionnel)
     * @param pageable Paramètres de pagination et tri
     * @return Page d'articles filtrés
     */
    PagedResponse<ArticleDTO> searchArticlesWithFilters(
            String search,
            String familleCode,
            Pageable pageable
    );

    // ==================== MÉTHODES EXISTANTES ====================

    PagedResponse<ArticleDTO> getAllArticles(Pageable pageable);

    PagedResponse<ArticleDTO> searchArticles(String searchTerm, Pageable pageable);

    PagedResponse<ArticleDTO> getArticlesByFamille(String codeFam, int type, Pageable pageable);

    CommandeResponseDTO findByCommandeId(Long commandeId);

    List<CommandeResponseDTO> getAllBonCommandes();

    List<CommandeResponseDTO> getBonCommandesByAffaire(String affaireCode);

    List<String> getAllFournisseurs();

    List<String> getFournisseursByAffaire(String affaireCode);

    List<AffaireReceptionResponseDTO> getArticleReceptionByCommandeID(Long commande);
}