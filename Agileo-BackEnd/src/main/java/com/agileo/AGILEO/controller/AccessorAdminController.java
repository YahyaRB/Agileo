package com.agileo.AGILEO.controller;

import com.agileo.AGILEO.Dtos.response.AffaireResponseDTO;
import com.agileo.AGILEO.entity.primary.KdnsAccessor;
import com.agileo.AGILEO.message.ResponseMessage;
import com.agileo.AGILEO.payload.response.ApiResponse;
import com.agileo.AGILEO.service.AffaireService;
import com.agileo.AGILEO.service.KdnsAccessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/accessors")
@PreAuthorize("hasRole('ADMIN')")
public class AccessorAdminController {

    @Autowired
    private KdnsAccessorService kdnsAccessorService;

    @Autowired
    private AffaireService affaireService;

    // ============ GESTION DES ACCESSORS ============

    @GetMapping
    public ResponseEntity<List<KdnsAccessor>> getAllAccessors() {
        return ResponseEntity.ok(kdnsAccessorService.findAllAccessors());
    }

    @GetMapping("/{accessorId}")
    public ResponseEntity<KdnsAccessor> getAccessorById(@PathVariable Integer accessorId) {
        try {
            KdnsAccessor accessor = kdnsAccessorService.findAccessorById(accessorId);
            return ResponseEntity.ok(accessor);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/by-login/{login}")
    public ResponseEntity<KdnsAccessor> getAccessorByLogin(@PathVariable String login) {
        try {
            KdnsAccessor accessor = kdnsAccessorService.findAccessorByLogin(login);
            return ResponseEntity.ok(accessor);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<KdnsAccessor>> searchAccessors(@RequestParam String search) {
        return ResponseEntity.ok(kdnsAccessorService.searchAccessorsByName(search));
    }

    // ============ GESTION DES AFFAIRES AVEC KDNSACCESSOR ============

    /**
     * Assigner une affaire à un accessor (KdnsAccessor)
     * POST /api/admin/accessors/{accessorId}/affaires/{affaireCode}
     */
    @PostMapping("/{accessorId}/affaires/{affaireCode}")
    public ResponseEntity<?> assignAffaireToAccessor(@PathVariable Integer accessorId,
                                                     @PathVariable String affaireCode,
                                                     Authentication authentication) {
        try {
            // Vérifier que l'accessor existe
            kdnsAccessorService.findAccessorById(accessorId);

            ResponseMessage response = affaireService.addAccessorToAffaire(
                    affaireCode,
                    accessorId,
                    authentication.getName()
            );
            return ResponseEntity.ok(ApiResponse.success(response.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Erreur lors de l'assignation de l'affaire: " + e.getMessage()));
        }
    }

    /**
     * Retirer une affaire d'un accessor
     * DELETE /api/admin/accessors/{accessorId}/affaires/{affaireCode}
     */
    @DeleteMapping("/{accessorId}/affaires/{affaireCode}")
    public ResponseEntity<?> removeAffaireFromAccessor(@PathVariable Integer accessorId,
                                                       @PathVariable String affaireCode,
                                                       Authentication authentication) {
        try {
            // Vérifier que l'accessor existe
            kdnsAccessorService.findAccessorById(accessorId);

            ResponseMessage response = affaireService.removeAccessorFromAffaire(
                    affaireCode,
                    accessorId,
                    authentication.getName()
            );
            return ResponseEntity.ok(ApiResponse.success(response.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Erreur lors de la suppression de l'affaire: " + e.getMessage()));
        }
    }

    /**
     * Récupérer toutes les affaires d'un accessor
     * GET /api/admin/accessors/{accessorId}/affaires
     */
    @GetMapping("/{accessorId}/affaires")
    public ResponseEntity<List<AffaireResponseDTO>> getAccessorAffaires(@PathVariable Integer accessorId) {
        try {
            // Vérifier que l'accessor existe
            kdnsAccessorService.findAccessorById(accessorId);

            List<AffaireResponseDTO> affaires = affaireService.findAffairesByAccessorId(accessorId);
            return ResponseEntity.ok(affaires);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(null);
        }
    }

    /**
     * Récupérer les affaires actives d'un accessor
     * GET /api/admin/accessors/{accessorId}/affaires/active
     */
    @GetMapping("/{accessorId}/affaires/active")
    public ResponseEntity<List<AffaireResponseDTO>> getActiveAccessorAffaires(@PathVariable Integer accessorId) {
        try {
            // Vérifier que l'accessor existe
            kdnsAccessorService.findAccessorById(accessorId);

            List<AffaireResponseDTO> affaires = affaireService.findActiveAffairesByAccessorId(accessorId);
            return ResponseEntity.ok(affaires);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(null);
        }
    }

    /**
     * Compter les affaires d'un accessor
     * GET /api/admin/accessors/{accessorId}/affaires/count
     */
    @GetMapping("/{accessorId}/affaires/count")
    public ResponseEntity<Long> getAccessorAffairesCount(@PathVariable Integer accessorId) {
        try {
            // Vérifier que l'accessor existe
            kdnsAccessorService.findAccessorById(accessorId);

            Long count = affaireService.countAffairesByAccessorId(accessorId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(0L);
        }
    }

    /**
     * Vérifier si un accessor peut être assigné à une affaire
     * GET /api/admin/accessors/{accessorId}/affaires/{affaireCode}/can-assign
     */
    @GetMapping("/{accessorId}/affaires/{affaireCode}/can-assign")
    public ResponseEntity<Boolean> canAssignAccessorToAffaire(@PathVariable Integer accessorId,
                                                              @PathVariable String affaireCode) {
        try {
            // Vérifier que l'accessor existe
            kdnsAccessorService.findAccessorById(accessorId);

            Boolean canAssign = affaireService.canAssignAccessorToAffaire(affaireCode, accessorId);
            return ResponseEntity.ok(canAssign);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(false);
        }
    }

    /**
     * Récupérer les assignations d'un accessor spécifique
     * GET /api/admin/accessors/{accessorId}/assignments
     */
    @GetMapping("/{accessorId}/assignments")
    public ResponseEntity<?> getAccessorAssignments(@PathVariable Integer accessorId) {
        try {
            // Vérifier que l'accessor existe
            kdnsAccessorService.findAccessorById(accessorId);

            return ResponseEntity.ok(affaireService.getAccessorAssignments(accessorId));
        } catch (Exception e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Accessor non trouvé: " + e.getMessage()));
        }
    }

    // ============ MÉTHODES POUR LIEN AVEC USER ============

    /**
     * Récupérer l'ID de l'utilisateur lié à cet accessor
     * GET /api/admin/accessors/{accessorId}/user-id
     */
    @GetMapping("/{accessorId}/user-id")
    public ResponseEntity<Long> getUserIdByAccessorId(@PathVariable Integer accessorId) {
        try {
            Long userId = kdnsAccessorService.getUserIdByAccessorId(accessorId);
            return ResponseEntity.ok(userId);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(null);
        }
    }
}