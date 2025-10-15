package com.agileo.AGILEO.controller;

import com.agileo.AGILEO.Dtos.response.AccessResponseDTO;
import com.agileo.AGILEO.Dtos.response.RoleResponseDTO;
import com.agileo.AGILEO.Dtos.response.UserResponseDTO;
import com.agileo.AGILEO.exception.ResourceNotFoundException;
import com.agileo.AGILEO.message.ResponseMessage;
import com.agileo.AGILEO.payload.response.ApiResponse;
import com.agileo.AGILEO.service.UserService;
import com.agileo.AGILEO.service.KdnsAccessorService;
import com.agileo.AGILEO.service.Impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private KdnsAccessorService kdnsAccessorService;

    // ============ GESTION DES UTILISATEURS (AUTH, STATUT) ============

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.findAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findUserById(id));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long id, Authentication authentication) {
        try {
            ResponseMessage response = userService.activateUser(id, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success(response.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(ApiResponse.error("Error activating user: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id, Authentication authentication) {
        try {
            ResponseMessage response = userService.deactivateUser(id, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success(response.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(ApiResponse.error("Error deactivating user: " + e.getMessage()));
        }
    }

    // ============ GESTION DES RÔLES (USERS SEULEMENT) ============

    @PostMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseMessage> addRoleToUser(
            @PathVariable Long userId,
            @PathVariable Long roleId,
            Authentication authentication) {
        return ResponseEntity.ok(userService.addRoleToUser(
                userId,
                roleId,
                authentication.getName()
        ));
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseMessage> removeRoleFromUser(
            @PathVariable Long userId,
            @PathVariable Long roleId,
            Authentication authentication) {
        return ResponseEntity.ok(userService.removeRoleFromUser(
                userId,
                roleId,
                authentication.getName()
        ));
    }

    @GetMapping("/{userId}/roles")
    public ResponseEntity<Set<RoleResponseDTO>> getUserRoles(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserRoles(userId));
    }

    // ============ GESTION DES ACCÈS (USERS SEULEMENT) ============

    @PostMapping("/{userId}/acces/{accesId}")
    public ResponseEntity<ResponseMessage> addAccesToUser(
            @PathVariable Long userId,
            @PathVariable Long accesId,
            Authentication authentication) {
        return ResponseEntity.ok(userService.addAccesToUser(
                userId,
                accesId,
                authentication.getName()
        ));
    }

    @DeleteMapping("/{userId}/acces/{accesId}")
    public ResponseEntity<ResponseMessage> removeAccesFromUser(
            @PathVariable Long userId,
            @PathVariable Long accesId,
            Authentication authentication) {
        return ResponseEntity.ok(userService.removeAccesFromUser(
                userId,
                accesId,
                authentication.getName()
        ));
    }

    @GetMapping("/{userId}/acces")
    public ResponseEntity<List<AccessResponseDTO>> getUserAcces(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserAcces(userId));
    }


    // ============ GESTION DES LIAISONS USER-ACCESSOR ============

    /**
     * Récupérer l'ID de l'accessor lié à cet utilisateur
     * GET /api/admin/users/{userId}/accessor-id
     */
    @GetMapping("/{userId}/accessor-id")
    public ResponseEntity<?> getAccessorIdByUserId(@PathVariable Long userId) {
        try {
            userService.findUserById(userId); // Verify user exists first
            Integer accessorId = userService.getAccessorIdByUserId(userId);

            if (accessorId != null) {
                return ResponseEntity.ok(accessorId);
            } else {
                // Return 204 No Content instead of 404 when no accessor is linked
                return ResponseEntity.noContent().build();
            }
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error getting accessor ID for user " + userId + ": " + e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Lier un utilisateur à un accessor
     * POST /api/admin/users/{userId}/link-accessor/{accessorId}
     */
    @PostMapping("/{userId}/link-accessor/{accessorId}")
    public ResponseEntity<?> linkUserToAccessor(@PathVariable Long userId,
                                                @PathVariable Integer accessorId,
                                                Authentication authentication) {
        try {
            // Vérifier que l'utilisateur et l'accessor existent
            userService.findUserById(userId);
            kdnsAccessorService.findAccessorById(accessorId);

            ResponseMessage response = userService.linkUserToAccessor(userId, accessorId, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success(response.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Erreur lors de la création de la liaison: " + e.getMessage()));
        }
    }

    /**
     * Délier un utilisateur d'un accessor
     * DELETE /api/admin/users/{userId}/unlink-accessor
     */
    @DeleteMapping("/{userId}/unlink-accessor")
    public ResponseEntity<?> unlinkUserFromAccessor(@PathVariable Long userId,
                                                    Authentication authentication) {
        try {
            // Vérifier que l'utilisateur existe
            userService.findUserById(userId);

            ResponseMessage response = userService.unlinkUserFromAccessor(userId, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success(response.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Erreur lors de la suppression de la liaison: " + e.getMessage()));
        }
    }

    /**
     * Synchroniser un utilisateur avec son accessor
     * POST /api/admin/users/{userId}/sync-accessor
     */
    @PostMapping("/{userId}/sync-accessor")
    public ResponseEntity<?> syncUserWithAccessor(@PathVariable Long userId,
                                                  Authentication authentication) {
        try {
            ResponseMessage response = userService.syncUserWithAccessor(userId, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success(response.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Erreur lors de la synchronisation: " + e.getMessage()));
        }
    }

    // ============ ENDPOINTS DE SYNCHRONISATION KEYCLOAK ============

    /**
     * Synchronise tous les utilisateurs avec Keycloak
     */
    @PostMapping("/sync-keycloak")
    public ResponseEntity<?> syncAllUsersWithKeycloak(Authentication authentication) {
        try {
            ResponseMessage response = ((UserServiceImpl) userService).syncAllUsersWithKeycloak();
            return ResponseEntity.ok(ApiResponse.success(response.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Error synchronizing users with Keycloak: " + e.getMessage()));
        }
    }

    /**
     * Synchronise un utilisateur spécifique avec Keycloak
     */
    @PostMapping("/{userId}/sync-keycloak")
    public ResponseEntity<?> syncUserWithKeycloak(@PathVariable Long userId, Authentication authentication) {
        try {
            ResponseMessage response = ((UserServiceImpl) userService).syncUserWithKeycloak(userId);
            return ResponseEntity.ok(ApiResponse.success(response.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Error synchronizing user with Keycloak: " + e.getMessage()));
        }
    }

    // ============ MÉTHODES UTILITAIRES ============

    /**
     * Rechercher des utilisateurs par terme de recherche
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserResponseDTO>> searchUsers(@RequestParam String search) {
        return ResponseEntity.ok(userService.searchUsers(search));
    }

    /**
     * Récupérer les utilisateurs actifs seulement
     */
    @GetMapping("/active")
    public ResponseEntity<List<UserResponseDTO>> getActiveUsers() {
        return ResponseEntity.ok(userService.getActiveUserss());
    }

    /**
     * Récupérer les utilisateurs inactifs seulement
     */
    @GetMapping("/inactive")
    public ResponseEntity<List<UserResponseDTO>> getInactiveUsers() {
        return ResponseEntity.ok(userService.getInactiveUsers());
    }

    // ============ ENDPOINTS DÉPRÉCIÉS POUR RÉTROCOMPATIBILITÉ ============
    // ⚠️ IMPORTANT: Ces endpoints redirigent vers AccessorAdminController

    /**
     * @deprecated Utilisez AccessorAdminController à la place
     * Les affaires sont maintenant gérées via KdnsAccessor, pas User
     */
    @Deprecated
    @PostMapping("/{userId}/affaires/{affaireCode}")
    public ResponseEntity<?> assignAffaire(@PathVariable Integer userId,
                                           @PathVariable String affaireCode,
                                           Authentication authentication) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error("DEPRECATED: Utilisez /api/admin/accessors/{accessorId}/affaires/{affaireCode} à la place. " +
                        "Les affaires sont maintenant gérées via KdnsAccessor."));
    }

    /**
     * @deprecated Utilisez AccessorAdminController à la place
     */
    @Deprecated
    @DeleteMapping("/{userId}/affaires/{affaireCode}")
    public ResponseEntity<?> removeAffaire(@PathVariable Integer userId,
                                           @PathVariable String affaireCode,
                                           Authentication authentication) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error("DEPRECATED: Utilisez /api/admin/accessors/{accessorId}/affaires/{affaireCode} à la place. " +
                        "Les affaires sont maintenant gérées via KdnsAccessor."));
    }

    /**
     * @deprecated Utilisez AccessorAdminController à la place
     */
    @Deprecated
    @GetMapping("/{userId}/affaires")
    public ResponseEntity<?> getUserAffaires(@PathVariable Integer userId) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error("DEPRECATED: Utilisez /api/admin/accessors/{accessorId}/affaires à la place. " +
                        "Les affaires sont maintenant gérées via KdnsAccessor."));
    }
}