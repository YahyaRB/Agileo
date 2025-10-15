package com.agileo.AGILEO.controller;

import com.agileo.AGILEO.entity.secondary.User;
import com.agileo.AGILEO.exception.UserSyncException;
import com.agileo.AGILEO.security.KeycloakSyncService;
import com.agileo.AGILEO.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:4200", "http://192.168.77.125:4200"}, allowCredentials = "true")
public class AuthController {

    @Autowired
    private KeycloakSyncService keycloakSyncService;

    @Autowired
    private UserService userService;

    /**
     * ✅ AMÉLIORÉ : Endpoint principal d'authentification avec meilleure gestion des erreurs
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticate() {
        try {
            System.out.println("🔄 === DÉBUT AUTHENTIFICATION ===");

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("🔍 Authentication: " + (authentication != null ? authentication.getClass().getSimpleName() : "null"));

            // Validation plus robuste
            if (!isValidJwtAuthentication(authentication)) {
                System.err.println("❌ Token JWT invalide ou manquant");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Token JWT invalide ou manquant", "INVALID_TOKEN"));
            }

            JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtToken.getToken();

            String login = jwt.getClaimAsString("preferred_username");
            System.out.println("🔄 Authentification pour utilisateur: " + login);

            // Vérification de l'expiration du token avec marge
            if (jwt.getExpiresAt().isBefore(Instant.now().plusSeconds(30))) {
                System.err.println("❌ Token expiré ou proche de l'expiration");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Token expiré", "TOKEN_EXPIRED"));
            }

            // Synchronisation avec gestion d'erreur améliorée
            User syncedUser = keycloakSyncService.syncUserFromKeycloak(jwt);

            System.out.println("✅ Authentification réussie pour: " + login);
            return ResponseEntity.ok(createSuccessResponse(syncedUser, "login"));

        } catch (UserSyncException e) {
            System.err.println("❌ Erreur de synchronisation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Erreur synchronisation: " + e.getMessage(), "SYNC_ERROR"));
        } catch (Exception e) {
            System.err.println("❌ Erreur interne authentification: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur", "INTERNAL_ERROR"));
        }
    }

    /**
     * ✅ NOUVEAU : Endpoint de vérification de santé pour Keycloak
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            Map<String, Object> health = new HashMap<>();
            health.put("timestamp", LocalDateTime.now());
            health.put("authenticated", authentication != null && authentication.isAuthenticated());
            health.put("authType", authentication != null ? authentication.getClass().getSimpleName() : "none");

            if (authentication instanceof JwtAuthenticationToken) {
                JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
                Jwt jwt = jwtToken.getToken();
                health.put("tokenValid", jwt.getExpiresAt().isAfter(Instant.now()));
                health.put("username", jwt.getClaimAsString("preferred_username"));
                health.put("expiresAt", jwt.getExpiresAt());
            }

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Health check failed: " + e.getMessage(), "HEALTH_ERROR"));
        }
    }

    /**
     * ✅ AMÉLIORÉ : Endpoint de synchronisation avec retry
     */
    @PostMapping("/sync")
    public ResponseEntity<?> syncCurrentUser(@RequestParam(defaultValue = "false") boolean forceSync) {
        try {
            System.out.println("🔄 === SYNCHRONISATION UTILISATEUR (force=" + forceSync + ") ===");

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (!isValidJwtAuthentication(authentication)) {
                return ResponseEntity.status(401)
                        .body(createErrorResponse("JWT Token requis", "TOKEN_REQUIRED"));
            }

            JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtToken.getToken();

            String login = jwt.getClaimAsString("preferred_username");
            System.out.println("🔄 Synchronisation pour: " + login);

            // Synchronisation avec retry en cas d'échec
            User syncedUser = null;
            int maxRetries = forceSync ? 3 : 1;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    syncedUser = keycloakSyncService.syncUserFromKeycloak(jwt);
                    break; // Succès, sortir de la boucle
                } catch (Exception e) {
                    System.err.println("❌ Tentative " + attempt + " échouée: " + e.getMessage());
                    if (attempt == maxRetries) {
                        throw e; // Dernière tentative, propager l'erreur
                    }
                    Thread.sleep(1000); // Attendre 1 seconde avant retry
                }
            }

            Map<String, Object> response = createSuccessResponse(syncedUser, "sync");
            response.put("forceSync", forceSync);
            response.put("attempts", maxRetries);

            System.out.println("✅ Synchronisation réussie pour: " + login);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur synchronisation: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Sync failed: " + e.getMessage(), "SYNC_FAILED"));
        }
    }



    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(@RequestParam(defaultValue = "false") boolean refresh) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null) {
                return ResponseEntity.status(401)
                        .body(createErrorResponse("Aucune authentification trouvée", "NO_AUTH"));
            }

            String login = authentication.getName();

            if (!userService.existsByUsername(login)) {
                System.err.println("❌ Utilisateur non trouvé en base: " + login);
                return ResponseEntity.status(404)
                        .body(createErrorResponse("Utilisateur non trouvé en base de données", "USER_NOT_FOUND"));
            }

            User user = userService.getUserByLogin(login);

            if (user.getIdAgelio() == null || user.getIdAgelio().trim().isEmpty()) {
                System.out.println("⚠️ idAgelio manquant pour l'utilisateur: " + login + " - Tentative de récupération...");
            }

            if (refresh && authentication instanceof JwtAuthenticationToken) {
                try {
                    JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
                    user = keycloakSyncService.syncExistingUserWithKeycloak(user, jwtToken.getToken());
                    System.out.println("✅ Données utilisateur rafraîchies");
                } catch (Exception e) {
                    System.err.println("⚠️ Erreur rafraîchissement (données cachées utilisées): " + e.getMessage());
                }
            }

            Map<String, Object> response = createSuccessResponse(user, "user-info");
            response.put("refreshed", refresh);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération info utilisateur: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Failed to get user info: " + e.getMessage(), "USER_INFO_ERROR"));
        }
    }
    /**
     * ✅ NOUVEAU : Endpoint de déconnexion propre
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null) {
                String login = authentication.getName();
                System.out.println("🔄 Déconnexion utilisateur: " + login);

                // Nettoyer le contexte de sécurité
                SecurityContextHolder.clearContext();

                System.out.println("✅ Déconnexion réussie pour: " + login);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logout successful");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Logout failed: " + e.getMessage(), "LOGOUT_ERROR"));
        }
    }

    /**
     * Endpoint de test (garder votre code existant mais amélioré)
     */
    @GetMapping("/check-user/{login}")
    public ResponseEntity<?> checkUser(@PathVariable String login) {
        try {
            boolean exists = userService.existsByUsername(login);

            Map<String, Object> response = new HashMap<>();
            response.put("login", login);
            response.put("exists", exists);
            response.put("timestamp", LocalDateTime.now());

            if (exists) {
                User user = userService.getUserByLogin(login);
                response.put("userId", user.getId());
                response.put("statut", user.getStatut());
                response.put("keycloakEnabled", user.getKeycloakEnabled());
                response.put("roles", user.getRoles().stream()
                        .map(role -> role.getName()).toArray());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Check failed: " + e.getMessage(), "CHECK_ERROR"));
        }
    }

    // ============ MÉTHODES HELPER AMÉLIORÉES ============

    private boolean isValidJwtAuthentication(Authentication authentication) {
        return authentication != null
                && authentication instanceof JwtAuthenticationToken
                && authentication.isAuthenticated();
    }

    /**
     * ✅ AMÉLIORÉ : Création de réponse de succès avec plus d'informations
     */
    private Map<String, Object> createSuccessResponse(User user, String action) {
        Map<String, Object> response = new HashMap<>();

        response.put("success", true);
        response.put("message", "Operation successful");
        response.put("action", action);
        response.put("timestamp", LocalDateTime.now());

        // Informations utilisateur
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("login", user.getLogin());
        userInfo.put("email", user.getEmail());
        userInfo.put("nom", user.getNom());
        userInfo.put("prenom", user.getPrenom());
        userInfo.put("matricule", user.getMatricule());
        userInfo.put("statut", user.getStatut());
        userInfo.put("dernierConnex", user.getDernierConnex());
        userInfo.put("keycloakEnabled", user.getKeycloakEnabled());
        userInfo.put("keycloakId", user.getKeycloakId());
        userInfo.put("idAgelio", user.getIdAgelio());

        // Ajouter les rôles
        userInfo.put("roles", user.getRoles().stream()
                .map(role -> role.getName()).toArray());

        response.put("user", userInfo);

        return response;
    }

    /**
     * ✅ AMÉLIORÉ : Création de réponse d'erreur avec code d'erreur
     */
    private Map<String, Object> createErrorResponse(String message, String errorCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("errorCode", errorCode);
        response.put("timestamp", LocalDateTime.now());
        return response;
    }
}