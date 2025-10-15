package com.agileo.AGILEO.service.Impl;

import com.agileo.AGILEO.Dtos.response.AffaireReceptionResponseDTO;
import com.agileo.AGILEO.Dtos.response.ArticleDTO;
import com.agileo.AGILEO.Dtos.response.CommandeResponseDTO;
import com.agileo.AGILEO.Dtos.response.PagedResponse;
import com.agileo.AGILEO.entity.primary.ArticleErp;
import com.agileo.AGILEO.entity.primary.ArticleReception;
import com.agileo.AGILEO.entity.primary.Commande;
import com.agileo.AGILEO.repository.primary.ArticleErpRepository;
import com.agileo.AGILEO.repository.primary.ArticleReceptionRepository;
import com.agileo.AGILEO.repository.primary.CommandeRepository;
import com.agileo.AGILEO.service.DaService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DaImpService implements DaService {

    private ArticleErpRepository articleErpRepository;
    private final CommandeRepository commandeRepository;
    public final ArticleReceptionRepository articleReceptionRepository;

    @Override
    public PagedResponse<ArticleDTO> getAllArticles(Pageable pageable) {
        Page<ArticleErp> articlePage = articleErpRepository.findAll(pageable);
        return convertToPagedResponse(articlePage);
    }



    @Override
    public PagedResponse<ArticleDTO> searchArticles(String searchTerm, Pageable pageable) {
        Page<ArticleErp> articlePage = articleErpRepository.searchArticles(searchTerm, pageable);
        return convertToPagedResponse(articlePage);
    }

    @Override
    public PagedResponse<ArticleDTO> searchArticlesWithFilters(
            String search,
            String familleCode,
            Pageable pageable) {

        Page<ArticleErp> articlePage = articleErpRepository.searchArticlesWithFilters(
                search,
                familleCode,
                pageable
        );

        return convertToPagedResponse(articlePage);
    }
    @Override
    public PagedResponse<ArticleDTO> getArticlesByFamille(String codeFam, int type, Pageable pageable) {
        Page<ArticleErp> articlePage = articleErpRepository.findByCodeFam1(codeFam, pageable);
        return convertToPagedResponse(articlePage);
    }

    @Override
    public CommandeResponseDTO findByCommandeId(Long commandeId) {
        return commandeRepository.findById(commandeId)
                .map(this::toCommandeResponseDTO)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée avec id : " + commandeId));
    }

    @Override
    public List<CommandeResponseDTO> getAllBonCommandes() {
        List<Commande> commandes = commandeRepository.findByCe("1");
        return commandes.stream()
                .map(this::toCommandeResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommandeResponseDTO> getBonCommandesByAffaire(String affaireCode) {
        List<Commande> commandes = commandeRepository.findByAffaireCode(affaireCode);
        return commandes.stream()
                .map(this::toCommandeResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllFournisseurs() {
        return commandeRepository.findDistinctFournisseurs();
    }

    @Override
    public List<String> getFournisseursByAffaire(String affaireCode) {
        return commandeRepository.findDistinctFournisseursByAffaire(affaireCode);
    }

    @Override
    public List<AffaireReceptionResponseDTO> getArticleReceptionByCommandeID(Long commande) {
        List<ArticleReception> articleByCommande = articleReceptionRepository.findArticleReceptionsByCommande(commande);
        return Collections.emptyList();
    }

    private PagedResponse<ArticleDTO> convertToPagedResponse(Page<ArticleErp> articlePage) {
        List<ArticleDTO> articleDTOs = articlePage.getContent().stream()
                .map(this::mapToArticleDTO)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                articleDTOs,
                articlePage.getNumber(),
                articlePage.getSize(),
                articlePage.getTotalElements(),
                articlePage.getTotalPages(),
                articlePage.isFirst(),
                articlePage.isLast(),
                articlePage.isEmpty()
        );
    }


    private ArticleDTO mapToArticleDTO(ArticleErp article) {
        ArticleDTO dto = new ArticleDTO();
        dto.setId(article.getRef() != null ? Math.abs(article.getRef().hashCode()) : 0);
        dto.setReference(article.getRef() != null ? article.getRef() : "N/A");
        dto.setDesignation(article.getDescription() != null ? article.getDescription() : "Description non disponible");
        dto.setUnite(article.getAchun() != null ? article.getAchun() : "PCS");
        dto.setFamilleStatistique1(article.getFam0001());
        dto.setFamilleStatistique2(article.getFam0002());
        dto.setFamilleStatistique3(article.getFam0003());
        return dto;
    }

    private CommandeResponseDTO toCommandeResponseDTO(Commande commande) {
        CommandeResponseDTO dto = new CommandeResponseDTO();
        dto.setCe(commande.getCe());
        dto.setFournisseur(commande.getFournisseur());
        dto.setFournisseurId(commande.getFournisseurId());
        dto.setAffaireCode(commande.getAffaireCode());
        dto.setAffaireName(commande.getAffaireName());
        dto.setCommande(commande.getCommande());
        dto.setVotreReference(commande.getVotreReference());
        dto.setPiece(commande.getPiece());
        dto.setDateCommande(commande.getDateCommande());

        // Initialiser referenceBC pour l'affichage
        dto.setReferenceBC();

        return dto;
    }
}