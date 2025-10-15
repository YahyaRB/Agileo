package com.agileo.AGILEO.controller;

import com.agileo.AGILEO.Dtos.request.AffaireRequestDTO;
import com.agileo.AGILEO.Dtos.response.AffaireDetailsDTO;
import com.agileo.AGILEO.Dtos.response.AffaireResponseDTO;
import com.agileo.AGILEO.Dtos.response.AffaireStatsDTO;
import com.agileo.AGILEO.Dtos.response.AffaireUserAssignmentDTO;
import com.agileo.AGILEO.message.ResponseMessage;
import com.agileo.AGILEO.service.AffaireService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/affaires")
public class AffaireController {

    private final AffaireService affaireService;

    public AffaireController(AffaireService affaireService) {
        this.affaireService = affaireService;
    }

    /**
     * Récupérer toutes les affaires (lecture depuis la vue AffaireLieUser)
     */
    @GetMapping
    public ResponseEntity<List<AffaireResponseDTO>> getAllAffaires(Authentication authentication) {
        try {
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            boolean isAdmin = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ADMIN"));
            boolean isConsulteur = authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("CONSULTEUR"));

            if (isAdmin || isConsulteur) {
                return ResponseEntity.ok(affaireService.findAllAffaires());
            } else {
                return ResponseEntity.ok(affaireService.findAffairesByAccessorLogin(authentication.getName()));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupérer une affaire par son ID (lecture depuis M6003AffaireUtilisateur pour les détails complets)
     */
    @GetMapping("/{id}")
    public ResponseEntity<AffaireResponseDTO> getAffaireById(@PathVariable Long id) {
        return ResponseEntity.ok(affaireService.findAffaireById(id));
    }

    /**
     * Récupérer les affaires par code (lecture depuis la vue AffaireLieUser)
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<List<AffaireResponseDTO>> getAffairesByCode(@PathVariable String code) {
        return ResponseEntity.ok(affaireService.findAffairesByCode(code));
    }

    /**
     * Récupérer les affaires par statut (lecture depuis M6003AffaireUtilisateur)
     */
    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<AffaireResponseDTO>> getAffairesByStatut(@PathVariable int statut) {
        return ResponseEntity.ok(affaireService.findAffairesByStatut(statut));
    }

    /**
     * Rechercher des affaires par mot-clé (lecture depuis la vue AffaireLieUser)
     */
    @GetMapping("/search")
    public ResponseEntity<List<AffaireResponseDTO>> searchAffaires(@RequestParam String keyword) {
        return ResponseEntity.ok(affaireService.searchAffaires(keyword));
    }

    /**
     * Récupérer les utilisateurs assignés à une affaire (lecture depuis la vue)
     */
    @GetMapping("/code/{affaireCode}/users")
    public ResponseEntity<Set<String>> getAffaireUsers(@PathVariable String affaireCode) {
        return ResponseEntity.ok(affaireService.getAffaireUsers(affaireCode));
    }

    /**
     * Ajouter un utilisateur à une affaire (écriture dans M6003AffaireUtilisateur)
     */
    @PostMapping("/code/{affaireCode}/accessors/{accessorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseMessage> addAccessorToAffaire(
            @PathVariable String affaireCode,
            @PathVariable Integer accessorId,
            Authentication authentication) {
        return ResponseEntity.ok(affaireService.addAccessorToAffaire(
                affaireCode, accessorId, authentication.getName()));
    }

    /**
     * Retirer un utilisateur d'une affaire (suppression dans M6003AffaireUtilisateur)
     */
    @DeleteMapping("/code/{affaireCode}/accessors/{accessorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseMessage> removeAccessorFromAffaire(
            @PathVariable String affaireCode,
            @PathVariable Integer accessorId,
            Authentication authentication) {
        return ResponseEntity.ok(affaireService.removeAccessorFromAffaire(
                affaireCode, accessorId, authentication.getName()));
    }

    /**
     * Changer le statut d'une affaire (modification dans M6003AffaireUtilisateur uniquement)
     */
    @PutMapping("/{id}/status/{newStatus}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseMessage> changeAffaireStatus(
            @PathVariable Long id,
            @PathVariable int newStatus,
            Authentication authentication) {
        return ResponseEntity.ok(affaireService.changeAffaireStatus(
                id, newStatus, authentication.getName()));
    }

    // ================ ENDPOINTS READ-ONLY POUR LA VUE ================

    /**
     * Récupérer les détails d'une affaire avec tous ses utilisateurs assignés
     */
    @GetMapping("/code/{affaireCode}/details")
    public ResponseEntity<AffaireDetailsDTO> getAffaireDetails(@PathVariable String affaireCode) {
        return ResponseEntity.ok(affaireService.getAffaireDetails(affaireCode));
    }

    /**
     * Récupérer toutes les assignations utilisateur-affaire (lecture seule depuis la vue)
     */
    @GetMapping("/assignments")
    public ResponseEntity<List<AffaireUserAssignmentDTO>> getAllAssignments() {
        return ResponseEntity.ok(affaireService.getAllUserAssignments());
    }

    /**
     * Récupérer les assignations d'un accessor spécifique
     */
    @GetMapping("/accessors/{accessorId}/assignments")
    public ResponseEntity<List<AffaireUserAssignmentDTO>> getAccessorAssignments(@PathVariable Integer accessorId) {
        return ResponseEntity.ok(affaireService.getAccessorAssignments(accessorId));
    }

    /**
     * Statistiques des affaires (lecture seule)
     */
    @GetMapping("/stats")
    public ResponseEntity<AffaireStatsDTO> getAffaireStats() {
        return ResponseEntity.ok(affaireService.getAffaireStats());
    }

    // ================ VALIDATION ENDPOINTS ================

    /**
     * Vérifier si une affaire existe (via vue Affaires)
     */
    @GetMapping("/validate/code/{code}")
    public ResponseEntity<Boolean> validateAffaireCode(@PathVariable String code) {
        return ResponseEntity.ok(affaireService.isValidAffaireCode(code));
    }

    /**
     * Vérifier si un accessor peut être assigné à une affaire
     */
    @GetMapping("/validate/assignment/{affaireCode}/{accessorId}")
    public ResponseEntity<Boolean> validateAccessorAssignment(
            @PathVariable String affaireCode,
            @PathVariable Integer accessorId) {
        return ResponseEntity.ok(affaireService.canAssignAccessorToAffaire(affaireCode, accessorId));
    }

    /**
     * Récupérer les affaires d'un accessor (KdnsAccessor) spécifique
     */
    @GetMapping("/accessor/{accessorId}")
    public ResponseEntity<List<AffaireResponseDTO>> getAccessorAffaires(@PathVariable Integer accessorId) {
        return ResponseEntity.ok(affaireService.findAffairesByAccessorId(accessorId));
    }

    /**
     * Récupérer les affaires de l'accessor connecté (basé sur l'authentification)
     */
    @GetMapping("/my-affaires")
    public ResponseEntity<List<AffaireResponseDTO>> getCurrentAccessorAffaires(Authentication authentication) {
        try {
            // Récupérer l'accessor connecté via son login depuis KdnsAccessor
            List<AffaireResponseDTO> affaires = affaireService.findAffairesByAccessorLogin(authentication.getName());
            return ResponseEntity.ok(affaires);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupérer le nombre d'affaires assignées à un accessor
     */
    @GetMapping("/accessor/{accessorId}/count")
    public ResponseEntity<Long> getAccessorAffairesCount(@PathVariable Integer accessorId) {
        return ResponseEntity.ok(affaireService.countAffairesByAccessorId(accessorId));
    }

    /**
     * Récupérer les affaires actives d'un accessor (statut = 1)
     */
    @GetMapping("/accessor/{accessorId}/active")
    public ResponseEntity<List<AffaireResponseDTO>> getActiveAccessorAffaires(@PathVariable Integer accessorId) {
        return ResponseEntity.ok(affaireService.findActiveAffairesByAccessorId(accessorId));
    }
}