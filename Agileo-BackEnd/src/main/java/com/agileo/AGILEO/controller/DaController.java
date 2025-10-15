package com.agileo.AGILEO.controller;

import com.agileo.AGILEO.Dtos.response.*;
import com.agileo.AGILEO.entity.primary.ArticleErp;
import com.agileo.AGILEO.entity.primary.ArticleReception;
import com.agileo.AGILEO.repository.primary.ArticleErpRepository;
import com.agileo.AGILEO.repository.primary.ArticleReceptionRepository;
import com.agileo.AGILEO.service.DaService;
import com.agileo.AGILEO.service.AffaireService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/temp")
@CrossOrigin(origins = "*", maxAge = 3600)
@AllArgsConstructor
public class DaController {

    private ArticleErpRepository articleErpRepository;
    private DaService daService;
    private AffaireService affaireService;
    public ArticleReceptionRepository articleReceptionRepository;

    @GetMapping("/articleRecpetion")
    public List<ArticleReception> articleRecpetion() {
        return articleReceptionRepository.findAll();
    }

    /**
     * Endpoint pour récupérer une commande par ID
     */
    @GetMapping("/boncommandes/{id}")
    public CommandeResponseDTO getCommandeById(@PathVariable Long id) {
        return daService.findByCommandeId(id);
    }

    /**
     * Endpoint pour récupérer tous les bons de commande (filtré selon les droits utilisateur)
     */
    @GetMapping("/boncommandes")
    public ResponseEntity<List<CommandeResponseDTO>> getAllBonCommandes(Authentication authentication) {
        try {
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            boolean isAdmin = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ADMIN"));
            boolean isConsulteur = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("CONSULTEUR"));

            List<CommandeResponseDTO> bonCommandes;

            if (isAdmin || isConsulteur) {
                // Admin et Consulteur voient tous les bons de commande
                bonCommandes = daService.getAllBonCommandes();
                System.out.println("ADMIN/CONSULTEUR - Tous les bons de commande: " + bonCommandes.size());
            } else {
                // Utilisateur normal : filtrer par ses affaires assignées
                bonCommandes = getBonCommandesForUser(authentication.getName());

            }

            return ResponseEntity.ok(bonCommandes);
        } catch (Exception e) {
            System.err.println("Erreur dans getAllBonCommandes: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(daService.getAllBonCommandes()); // Fallback
        }
    }

    /**
     * Méthode privée pour récupérer les bons de commande d'un utilisateur spécifique
     */
// Remplace TOUT le contenu de cette méthode :
    private List<CommandeResponseDTO> getBonCommandesForUser(String userLogin) {
        try {
            List<AffaireResponseDTO> userAffaires = affaireService.findAffairesByAccessorLogin(userLogin);

            if (userAffaires == null || userAffaires.isEmpty()) {
                System.out.println("Aucune affaire trouvée pour l'utilisateur: " + userLogin);
                return Collections.emptyList();
            }

            // adapte le getter si nécessaire (getAffaire() ou getCode() suivant ton DTO)
            List<String> affaireCodes = userAffaires.stream()
                    .map(AffaireResponseDTO::getAffaire)
                    .collect(Collectors.toList());

            List<CommandeResponseDTO> allBonCommandes = daService.getAllBonCommandes();

            List<CommandeResponseDTO> filteredCommandes = allBonCommandes.stream()
                    .filter(bc -> affaireCodes.contains(bc.getAffaireCode()))
                    .collect(Collectors.toList());




            return filteredCommandes;
        } catch (Exception e) {
            System.err.println("Erreur lors du filtrage des bons de commande pour " + userLogin + ": " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }


    /**
     * Endpoint pour récupérer les bons de commande par affaire
     */
    @GetMapping("/boncommandes/affaire/{affaireCode}")
    public ResponseEntity<List<CommandeResponseDTO>> getBonCommandesByAffaire(@PathVariable String affaireCode) {
        List<CommandeResponseDTO> bonCommandes = daService.getBonCommandesByAffaire(affaireCode);
        return ResponseEntity.ok(bonCommandes);
    }

    /**
     * Endpoint pour récupérer tous les fournisseurs (filtré selon les droits utilisateur)
     */
    @GetMapping("/fournisseurs")
    public ResponseEntity<List<String>> getAllFournisseurs(Authentication authentication) {
        try {
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            boolean isAdmin = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ADMIN"));
            boolean isConsulteur = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("CONSULTEUR"));

            if (isAdmin || isConsulteur) {
                // Admin et Consulteur voient tous les fournisseurs
                List<String> fournisseurs = daService.getAllFournisseurs();
                return ResponseEntity.ok(fournisseurs);
            } else {
                // Utilisateur normal : fournisseurs de ses affaires seulement
                List<String> fournisseurs = getFournisseursForUser(authentication.getName());
                return ResponseEntity.ok(fournisseurs);
            }
        } catch (Exception e) {
            System.err.println("Erreur dans getAllFournisseurs: " + e.getMessage());
            return ResponseEntity.ok(daService.getAllFournisseurs()); // Fallback
        }
    }

    /**
     * Méthode privée pour récupérer les fournisseurs d'un utilisateur spécifique
     */
// Remplace TOUT le contenu de cette méthode :
    private List<String> getFournisseursForUser(String userLogin) {
        try {
            List<AffaireResponseDTO> userAffaires = affaireService.findAffairesByAccessorLogin(userLogin);

            if (userAffaires == null || userAffaires.isEmpty()) {
                return Collections.emptyList();
            }

            // adapte le getter si nécessaire (getAffaire() ou getCode() suivant ton DTO)
            return userAffaires.stream()
                    .flatMap(affaire -> daService.getFournisseursByAffaire(affaire.getAffaire()).stream())
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Erreur lors du filtrage des fournisseurs pour " + userLogin + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }


    /**
     * Endpoint pour récupérer les fournisseurs par affaire
     */
    @GetMapping("/fournisseurs/affaire/{affaireCode}")
    public ResponseEntity<List<String>> getFournisseursByAffaire(@PathVariable String affaireCode) {
        List<String> fournisseurs = daService.getFournisseursByAffaire(affaireCode);
        return ResponseEntity.ok(fournisseurs);
    }
    @GetMapping("/articles/filtered")
    public ResponseEntity<PagedResponse<ArticleDTO>> getArticlesFiltered(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "familleCode", required = false) String familleCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "ref") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir) {

        System.out.println("=== ENDPOINT ARTICLES FILTRÉS ===");
        System.out.println("Search: " + search);
        System.out.println("Famille: " + familleCode);
        System.out.println("Page: " + page + ", Size: " + size);

        // Mapper le champ de tri
        String mappedSortBy = mapSortField(sortBy);

        // Créer le Pageable
        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(mappedSortBy).descending() :
                Sort.by(mappedSortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // Appeler le service unifié
        PagedResponse<ArticleDTO> response = daService.searchArticlesWithFilters(
                search,
                familleCode,
                pageable
        );

        System.out.println("Réponse: " + response.getTotalElements() + " articles");

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint pour récupérer tous les articles avec pagination
     */
    @GetMapping("/articles")
    public ResponseEntity<PagedResponse<ArticleDTO>> getAllArticles(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "ref") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir) {

        String mappedSortBy = mapSortField(sortBy);
        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(mappedSortBy).descending() :
                Sort.by(mappedSortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ArticleErp> articlePage = articleErpRepository.findAll(pageable);

        List<ArticleDTO> articleDTOs = articlePage.getContent().stream()
                .map(this::mapToArticleDTO)
                .collect(Collectors.toList());

        PagedResponse<ArticleDTO> response = new PagedResponse<>(
                articleDTOs,
                articlePage.getNumber(),
                articlePage.getSize(),
                articlePage.getTotalElements(),
                articlePage.getTotalPages(),
                articlePage.isFirst(),
                articlePage.isLast(),
                articlePage.isEmpty()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint pour rechercher des articles avec pagination
     */
    @GetMapping("/articles/search")
    public ResponseEntity<PagedResponse<ArticleDTO>> searchArticles(
            @RequestParam("term") String searchTerm,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "ref") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir) {

        String mappedSortBy = mapSortField(sortBy);
        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(mappedSortBy).descending() :
                Sort.by(mappedSortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ArticleErp> articlePage = articleErpRepository.searchArticles(searchTerm, pageable);

        List<ArticleDTO> articleDTOs = articlePage.getContent().stream()
                .map(this::mapToArticleDTO)
                .collect(Collectors.toList());

        PagedResponse<ArticleDTO> response = new PagedResponse<>(
                articleDTOs,
                articlePage.getNumber(),
                articlePage.getSize(),
                articlePage.getTotalElements(),
                articlePage.getTotalPages(),
                articlePage.isFirst(),
                articlePage.isLast(),
                articlePage.isEmpty()
        );

        return ResponseEntity.ok(response);
    }




    /**
     * Endpoint pour récupérer les articles par famille avec pagination
     */
    @GetMapping("/articles/famille/{codeFam}")
    public ResponseEntity<PagedResponse<ArticleDTO>> getArticlesByFamille(
            @PathVariable String codeFam,
            @RequestParam(value = "type", defaultValue = "1") int type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "ref") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir) {

        String mappedSortBy = mapSortField(sortBy);
        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(mappedSortBy).descending() :
                Sort.by(mappedSortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ArticleErp> articlePage = articleErpRepository.findByCodeFam1(codeFam, pageable);

        List<ArticleDTO> articleDTOs = articlePage.getContent().stream()
                .map(this::mapToArticleDTO)
                .collect(Collectors.toList());

        PagedResponse<ArticleDTO> response = new PagedResponse<>(
                articleDTOs,
                articlePage.getNumber(),
                articlePage.getSize(),
                articlePage.getTotalElements(),
                articlePage.getTotalPages(),
                articlePage.isFirst(),
                articlePage.isLast(),
                articlePage.isEmpty()
        );

        return ResponseEntity.ok(response);
    }
    private String mapSortField(String frontendField) {
        Map<String, String> fieldMapping = new HashMap<>();
        fieldMapping.put("reference", "ref");
        fieldMapping.put("designation", "description");
        fieldMapping.put("unite", "achun");
        fieldMapping.put("familleStatistique1", "fam0001");
        fieldMapping.put("familleStatistique2", "fam0002");
        fieldMapping.put("familleStatistique3", "fam0003");

        return fieldMapping.getOrDefault(frontendField, frontendField);
    }


    /**
     * Mapper ArticleErp vers ArticleDTO pour le frontend
     */
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

    // --- Placeholder pour compiler : remplace par ton vrai type ---
    // Supprime cette classe interne si tu as déjà la vraie classe dans ton code.
    private static class Affaire {
        private String affaire;
        public String getAffaire() { return affaire; }
    }
    @GetMapping("/articles/familles/1/values")
    public ResponseEntity<List<String>> getFamilleStatistique1Values() {
        List<String> familles = articleErpRepository.findDistinctCodeFam1();
        return ResponseEntity.ok(familles);
    }





    @GetMapping("/articles/familles/{familleType}/count")
    public ResponseEntity<List<Map<String, Object>>> getFamilleStatistiquesWithCount(
            @PathVariable int familleType) {

        List<Object[]> results;



                results = articleErpRepository.countArticlesByCodeFam1();



        List<Map<String, Object>> response = results.stream()
                .map(result -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("code", result[0]);
                    map.put("count", result[1]);
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
    @GetMapping("/articles/familles/1/with-designation")
    public ResponseEntity<List<FamilleStatistiqueDTO>> getFamilleStatistique1WithDesignation() {
        System.out.println("🔍 Récupération des familles 1");

        // ✅ Récupérer uniquement les codes
        List<String> codes = articleErpRepository.findDistinctCodeFam1();

        // ✅ Créer des DTOs avec code = désignation
        List<FamilleStatistiqueDTO> familles = codes.stream()
                .map(code -> new FamilleStatistiqueDTO(code, code)) // code comme désignation
                .collect(Collectors.toList());

        System.out.println("✅ " + familles.size() + " familles trouvées");

        return ResponseEntity.ok(familles);
    }
}
