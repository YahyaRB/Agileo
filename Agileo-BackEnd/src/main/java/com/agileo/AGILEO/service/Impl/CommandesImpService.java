package com.agileo.AGILEO.service.Impl;

import com.agileo.AGILEO.Dtos.response.ArticleDisponibleDTO;
import com.agileo.AGILEO.Dtos.response.CommandeResponseDTO;
import com.agileo.AGILEO.entity.primary.ArticleReception;
import com.agileo.AGILEO.entity.primary.Commande;
import com.agileo.AGILEO.entity.primary.LigneReception;
import com.agileo.AGILEO.exception.BadRequestException;
import com.agileo.AGILEO.repository.primary.ArticleReceptionRepository;
import com.agileo.AGILEO.repository.primary.CommandeRepository;
import com.agileo.AGILEO.repository.primary.LigneReceptionRepository;
import com.agileo.AGILEO.service.CommandesService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class CommandesImpService implements CommandesService {
    private CommandeRepository commandeRepository;
    private ArticleReceptionRepository articleReceptionRepository;
    private LigneReceptionRepository ligneReceptionRepository;
    @Override
    public List<ArticleDisponibleDTO> getArticlesDisponibles(Long commandeId) {
        try {



            // Récupérer les articles depuis ArticleReception (Livraison_ERP)
            List<ArticleReception> articlesReception = articleReceptionRepository
                    .findArticleReceptionsByCommande(commandeId.longValue());


            // Transformer en DTOs avec calcul des quantités disponibles
            List<ArticleDisponibleDTO> articlesDisponibles = new ArrayList<>();

            for (ArticleReception articleReception : articlesReception) {

                ArticleDisponibleDTO dto = new ArticleDisponibleDTO();

                // Informations de base
                dto.setReference(articleReception.getArticleId());
                dto.setDesignation(articleReception.getDesignation());
                dto.setUnite(articleReception.getUnite());

                // Quantités
                BigDecimal qteCommandee = articleReception.getQteCommandee() != null ?
                        articleReception.getQteCommandee() : BigDecimal.ZERO;
                BigDecimal qteLivree = articleReception.getQteLivree() != null ?
                        articleReception.getQteLivree() : BigDecimal.ZERO;
                BigDecimal qteRest = articleReception.getQteRest() != null ?
                        articleReception.getQteRest() : BigDecimal.ZERO;

                dto.setQuantiteCommandee(qteCommandee);
                dto.setQuantiteDejaRecue(qteLivree);
                dto.setQuantiteDisponible(qteRest);
                articlesDisponibles.add(dto);
            }
            return articlesDisponibles;
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des articles disponibles: " + e.getMessage());
            e.printStackTrace();
            throw new BadRequestException("Erreur lors de la récupération des articles: " + e.getMessage());
        }
    }
}
