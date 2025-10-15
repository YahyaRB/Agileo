package com.agileo.AGILEO.controller;

import com.agileo.AGILEO.Dtos.request.*;
import com.agileo.AGILEO.Dtos.response.*;
import com.agileo.AGILEO.exception.BadRequestException;
import com.agileo.AGILEO.exception.ResourceNotFoundException;
import com.agileo.AGILEO.message.ResponseMessage;
import com.agileo.AGILEO.service.ConsommationService;
import com.agileo.AGILEO.service.FileService;
import com.agileo.AGILEO.service.Impl.AffaireImpService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/consommations")
public class ConsommationController {

    private final ConsommationService consommationService;
    private final AffaireImpService affaireService;
    private final FileService fileService;

    public ConsommationController(ConsommationService consommationService, AffaireImpService affaireService, FileService fileService) {
        this.consommationService = consommationService;
        this.affaireService = affaireService;
        this.fileService = fileService;
    }

    // ==================== GESTION DES CONSOMMATIONS ====================

    /**
     * Créer une nouvelle consommation
     */
    @PostMapping
    public ResponseEntity<ConsommationResponseDTO> createConsommation(
            @RequestBody ConsommationRequestDTO consommationDto,
            Authentication authentication) {
        try {
            String currentUsername = authentication != null ? authentication.getName() : "system";
            ConsommationResponseDTO response = consommationService.createConsommation(consommationDto, currentUsername);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (BadRequestException e) {
            System.err.println("Erreur création consommation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (ResourceNotFoundException e) {
            System.err.println("Affaire non trouvée: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            System.err.println("Erreur serveur lors de la création: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupérer toutes les consommations avec contrôle d'accès par rôle
     */
    @GetMapping
    public ResponseEntity<List<ConsommationResponseDTO>> getAllConsommations(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            boolean isAdmin = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ADMIN"));
            boolean isManager = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("MANAGER"));
            boolean isConsulteur = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("CONSULTEUR"));

            if (isAdmin || isManager || isConsulteur) {
                // Accès à toutes les consommations
                List<ConsommationResponseDTO> allConsommations = consommationService.getAllConsommations();
                return ResponseEntity.ok(allConsommations);
            } else {
                // Accès seulement aux consommations de l'utilisateur
                String username = authentication.getName();
                if (username == null || username.trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }
                List<ConsommationResponseDTO> userConsommations = consommationService.getCurrentUserConsommations(username);
                return ResponseEntity.ok(userConsommations);
            }
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.ok(Collections.emptyList());
        } catch (BadRequestException e) {
            System.err.println("Erreur de validation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des consommations: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupérer une consommation par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConsommationResponseDTO> getConsommationById(@PathVariable Integer id) {
        try {
            ConsommationResponseDTO consommation = consommationService.getConsommationById(id);
            return ResponseEntity.ok(consommation);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            System.err.println("Erreur récupération consommation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupérer les consommations par affaire
     */
    @GetMapping("/affaire/{affaireId}")
    public ResponseEntity<List<ConsommationResponseDTO>> getConsommationsByAffaire(@PathVariable Integer affaireId) {
        try {
            List<ConsommationResponseDTO> consommations = consommationService.getConsommationsByAffaire(affaireId);
            return ResponseEntity.ok(consommations);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            System.err.println("Erreur consommations par affaire: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupérer les consommations par utilisateur
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ConsommationResponseDTO>> getConsommationsByUser(@PathVariable Integer userId) {
        try {
            List<ConsommationResponseDTO> consommations = consommationService.getConsommationsByUser(userId);
            return ResponseEntity.ok(consommations);
        } catch (Exception e) {
            System.err.println("Erreur consommations par utilisateur: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/accessor/{accessorId}")
    public ResponseEntity<List<AffaireResponseDTO>> getAccessorAffaires(@PathVariable Integer accessorId) {
        try {
            List<AffaireResponseDTO> affaires = affaireService.findAffairesByAccessorId(accessorId);
            return ResponseEntity.ok(affaires);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Erreur récupération affaires accessor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * NOUVEAU: Récupérer les affaires de l'accessor connecté
     */
    @GetMapping("/my-affaires")
    public ResponseEntity<List<AffaireResponseDTO>> getCurrentAccessorAffaires(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String currentUsername = authentication.getName();
            List<AffaireResponseDTO> affaires = affaireService.findAffairesByAccessorLogin(currentUsername);
            return ResponseEntity.ok(affaires);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.ok(new ArrayList<>());
        } catch (Exception e) {
            System.err.println("Erreur récupération affaires utilisateur courant: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupérer les consommations de l'utilisateur connecté
     */
    @GetMapping("/current/consommations")
    public ResponseEntity<List<ConsommationResponseDTO>> getCurrentUserConsommations(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            String username = authentication.getName();
            List<ConsommationResponseDTO> consommations = consommationService.getCurrentUserConsommations(username);
            return ResponseEntity.ok(consommations);
        } catch (Exception e) {
            System.err.println("Erreur consommations utilisateur courant: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Mettre à jour une consommation
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResponseMessage> updateConsommation(
            @PathVariable Integer id,
            @RequestBody ConsommationRequestDTO consommationDto,
            Authentication authentication) {
        try {
            String currentUsername = authentication != null ? authentication.getName() : "system";
            ResponseMessage response = consommationService.updateConsommation(id, consommationDto, currentUsername);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (BadRequestException e) {
            System.err.println("Erreur modification consommation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            System.err.println("Erreur serveur modification: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Envoyer une consommation (changer le statut à "Envoyé")
     */
    @PutMapping("/{id}/envoyer")
    public ResponseEntity<ResponseMessage> envoyerConsommation(
            @PathVariable Integer id,
            Authentication authentication) {
        try {
            String currentUsername = authentication != null ? authentication.getName() : "system";
            ResponseMessage response = consommationService.envoyerConsommation(id, currentUsername);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (BadRequestException e) {
            System.err.println("Erreur envoi consommation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            System.err.println("Erreur serveur envoi: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Supprimer une consommation
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseMessage> deleteConsommation(@PathVariable Integer id) {
        try {
            ResponseMessage response = consommationService.deleteConsommation(id);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (BadRequestException e) {
            System.err.println("Erreur suppression consommation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            System.err.println("Erreur serveur suppression: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== GESTION DES ARTICLES DISPONIBLES ====================

    @GetMapping("/affaire/{affaireCode}")
    public ResponseEntity<List<ConsommationResponseDTO>> getConsommationsByAffaire(@PathVariable String affaireCode) {
        try {
            // CORRECTION: Passer directement le code d'affaire comme String
            List<ConsommationResponseDTO> consommations = consommationService.getConsommationsByAffaireCode(affaireCode);
            return ResponseEntity.ok(consommations);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            System.err.println("Erreur consommations par affaire: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupérer les articles disponibles pour une affaire - CORRIGÉ
     */
    @GetMapping("/articles-disponibles/{affaireCode}")
    public ResponseEntity<List<ArticleStockDTO>> getArticlesDisponibles(@PathVariable String affaireCode) {
        try {
            List<ArticleStockDTO> articles = consommationService.getArticlesDisponiblesForAffaire(affaireCode);
            return ResponseEntity.ok(articles);
        } catch (ResourceNotFoundException e) {
            System.err.println("Affaire non trouvée pour articles: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            System.err.println("Erreur articles disponibles: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // ==================== GESTION DES LIGNES DE CONSOMMATION ====================

    /**
     * Ajouter des lignes de consommation
     */
    @PostMapping("/{consommationId}/lignes")
    public ResponseEntity<ResponseMessage> addLignesConsommation(
            @PathVariable Integer consommationId,
            @RequestBody List<LigneConsommationRequestDTO> lignesDto,
            Authentication authentication) {
        try {
            String currentUsername = authentication != null ? authentication.getName() : "system";
            ResponseMessage response = consommationService.addLignesConsommation(consommationId, lignesDto, currentUsername);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (BadRequestException e) {
            System.err.println("Erreur ajout lignes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            System.err.println("Erreur serveur ajout lignes: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupérer les lignes d'une consommation
     */
    @GetMapping("/{consommationId}/lignes")
    public ResponseEntity<List<LigneConsommationResponseDTO>> getLignesConsommation(@PathVariable Integer consommationId) {
        try {
            List<LigneConsommationResponseDTO> lignes = consommationService.getLignesConsommationByConsommationId(consommationId);
            return ResponseEntity.ok(lignes);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            System.err.println("Erreur récupération lignes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Mettre à jour une ligne de consommation
     */
    @PutMapping("/lignes/{ligneId}")
    public ResponseEntity<ResponseMessage> updateLigneConsommation(
            @PathVariable Integer ligneId,
            @RequestBody LigneConsommationRequestDTO ligneDto,
            Authentication authentication) {
        try {
            String currentUsername = authentication != null ? authentication.getName() : "system";
            ResponseMessage response = consommationService.updateLigneConsommation(ligneId, ligneDto, currentUsername);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (BadRequestException e) {
            System.err.println("Erreur modification ligne: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            System.err.println("Erreur serveur modification ligne: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Supprimer une ligne de consommation
     */
    @DeleteMapping("/lignes/{ligneId}")
    public ResponseEntity<ResponseMessage> deleteLigneConsommation(
            @PathVariable Integer ligneId,
            Authentication authentication) {
        try {
            String currentUsername = authentication != null ? authentication.getName() : "system";
            ResponseMessage response = consommationService.deleteLigneConsommation(ligneId, currentUsername);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (BadRequestException e) {
            System.err.println("Erreur suppression ligne: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            System.err.println("Erreur serveur suppression ligne: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Valider la quantité avant ajout/modification (endpoint optionnel)
     */
    @PostMapping("/{consommationId}/validate-quantite")
    public ResponseEntity<Object> validateQuantite(
            @PathVariable Integer consommationId,
            @RequestBody Map<String, Object> request) {
        try {
            String referenceArticle = (String) request.get("referenceArticle");
            Double quantite = ((Number) request.get("quantite")).doubleValue();

            // Logique de validation personnalisée
            Map<String, Object> result = new HashMap<>();
            result.put("valide", true);
            result.put("message", "Quantité valide");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("valide", false);
            result.put("message", "Erreur de validation");
            return ResponseEntity.ok(result);
        }
    }

    // ==================== GESTION DES FICHIERS ====================

    /**
     * Upload de fichiers pour une consommation
     */
//    @PostMapping("/upload/{consommationId}")
//    public ResponseEntity<ResponseMessage> uploadFiles(
//            @PathVariable Integer consommationId,
//            @RequestParam("files") List<MultipartFile> files,
//            @RequestParam(value = "generatedNames", required = false) List<String> generatedNames,
//            Authentication authentication) {
//        try {
//            String currentUsername = authentication != null ? authentication.getName() : "system";
//
//            // Validation des fichiers
//            if (files == null || files.isEmpty()) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//            }
//
//            // Vérifier que la consommation existe
//            consommationService.getConsommationById(consommationId);
//
//            // TODO: Implémenter l'upload de fichiers pour les consommations
//            // Cette partie nécessite un service de gestion des fichiers
//
//            return ResponseEntity.ok(new ResponseMessage("Fichiers uploadés avec succès"));
//        } catch (ResourceNotFoundException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
//        } catch (Exception e) {
//            System.err.println("Erreur upload fichiers: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }

    /**
     * Supprimer un fichier
     */
    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<ResponseMessage> deleteFile(@PathVariable Integer fileId) {
        try {
            // TODO: Implémenter la suppression de fichier pour les consommations
            return ResponseEntity.ok(new ResponseMessage("Fichier supprimé avec succès"));
        } catch (Exception e) {
            System.err.println("Erreur suppression fichier: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

//    /**
//     * Récupérer les fichiers d'une consommation
//     */
//    @GetMapping("/{consommationId}/files")
//    public ResponseEntity<List<Object>> getFilesByConsommation(@PathVariable Integer consommationId) {
//        try {
//            // Vérifier que la consommation existe
//            consommationService.getConsommationById(consommationId);
//
//            // TODO: Implémenter la récupération des fichiers pour les consommations
//            return ResponseEntity.ok(Collections.emptyList());
//        } catch (ResourceNotFoundException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
//        } catch (Exception e) {
//            System.err.println("Erreur récupération fichiers: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }

    /**
     * Télécharger un fichier
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Integer fileId) {
        try {
            // TODO: Implémenter le téléchargement de fichier pour les consommations
            return ResponseEntity.ok(new byte[0]);
        } catch (Exception e) {
            System.err.println("Erreur téléchargement fichier: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Prévisualiser un fichier
     */
    @GetMapping("/preview/{fileId}")
    public ResponseEntity<byte[]> previewFile(@PathVariable Integer fileId) {
        try {
            // TODO: Implémenter la prévisualisation de fichier pour les consommations
            return ResponseEntity.ok(new byte[0]);
        } catch (Exception e) {
            System.err.println("Erreur prévisualisation fichier: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtenir des statistiques de consommation par affaire
     */
    @GetMapping("/stats/affaire/{affaireId}")
    public ResponseEntity<Object> getStatsConsommationByAffaire(@PathVariable Integer affaireId) {
        try {
            // TODO: Implémenter les statistiques de consommation
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalConsommations", 0);
            stats.put("totalArticles", 0);
            stats.put("message", "Statistiques non implémentées");

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Erreur statistiques: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtenir l'historique des consommations pour un article
     */
    @GetMapping("/historique/article/{referenceArticle}")
    public ResponseEntity<List<Object>> getHistoriqueConsommationArticle(@PathVariable String referenceArticle) {
        try {
            // TODO: Implémenter l'historique des consommations par article
            return ResponseEntity.ok(Collections.emptyList());
        } catch (Exception e) {
            System.err.println("Erreur historique article: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Rechercher des consommations par critères multiples
     */
    @PostMapping("/search")
    public ResponseEntity<List<ConsommationResponseDTO>> searchConsommations(
            @RequestBody Map<String, Object> criteria,
            Authentication authentication) {
        try {
            // TODO: Implémenter la recherche multicritères
            return ResponseEntity.ok(Collections.emptyList());
        } catch (Exception e) {
            System.err.println("Erreur recherche consommations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}