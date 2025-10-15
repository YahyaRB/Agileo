package com.agileo.AGILEO.security;

import com.agileo.AGILEO.entity.secondary.User;
import com.agileo.AGILEO.entity.secondary.Role;
import com.agileo.AGILEO.service.UserService;
import com.agileo.AGILEO.service.KeycloakAdminService;
import com.agileo.AGILEO.repository.secondary.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class KeycloakSyncService {

    @Autowired
    private UserService userService;

    @Autowired
    private KeycloakAdminService keycloakAdminService;

    @Autowired
    private RoleRepository roleRepository;

    // ============ MÉTHODES PRINCIPALES ============

    /**
     * Synchronise un utilisateur Keycloak avec la base de données locale
     */
    public User syncUserFromKeycloak(Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            String login = jwt.getClaimAsString("preferred_username");
            String email = jwt.getClaimAsString("email");
            String firstName = jwt.getClaimAsString("given_name");
            String lastName = jwt.getClaimAsString("family_name");



            if (login == null || login.trim().isEmpty()) {
                throw new IllegalArgumentException("Login cannot be null or empty");
            }

            // Vérifier si l'utilisateur existe déjà
            if (userService.existsByUsername(login)) {
                return updateExistingUser(jwt, keycloakId, login, email, firstName, lastName);
            } else {
                return createNewUser(jwt, keycloakId, login, email, firstName, lastName);
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur synchronisation utilisateur Keycloak: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to sync user from Keycloak: " + e.getMessage(), e);
        }
    }

    /**
     * Synchronise un utilisateur existant avec les données Keycloak
     */
    public User syncExistingUserWithKeycloak(User user, Jwt jwt) {
        try {
            System.out.println("🔄 Synchronisation utilisateur existant avec Keycloak: " + user.getLogin());

            boolean hasChanges = false;

            // 1. Mettre à jour les informations personnelles
            hasChanges |= updatePersonalInfo(user, jwt);

            // 2. Synchroniser les rôles
            hasChanges |= syncUserRoles(user, jwt);

            // 3. Mettre à jour les informations Keycloak
            String keycloakId = jwt.getSubject();
            if (user.getKeycloakId() == null || !user.getKeycloakId().equals(keycloakId)) {
                user.setKeycloakId(keycloakId);
                user.setKeycloakEnabled(true);
                hasChanges = true;
            }

            // 4. Mettre à jour la dernière connexion
            user.setDernierConnex(LocalDateTime.now());
            hasChanges = true;

            // 5. Sauvegarder si des changements ont eu lieu
            if (hasChanges) {
                user.setLastModifiedBy("keycloak-sync");
                user.setLastModifiedDate(LocalDateTime.now());
                User updatedUser = userService.updateUser(user);
                System.out.println("✅ Utilisateur synchronisé: " + user.getLogin());
                return updatedUser;
            } else {
                System.out.println("ℹ️ Aucun changement détecté pour: " + user.getLogin());
                return user;
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur synchronisation utilisateur existant: " + e.getMessage());
            return user; // Retourner l'utilisateur original en cas d'erreur
        }
    }

    /**
     * Synchronise tous les utilisateurs avec Keycloak
     */
    public void syncAllUsersWithKeycloak() {
        try {
            System.out.println("🔄 === SYNCHRONISATION GLOBALE KEYCLOAK ===");

            List<User> allUsers = userService.getActiveUsers();
            int totalUsers = allUsers.size();
            int syncedUsers = 0;

            for (User user : allUsers) {
                try {
                    if (keycloakAdminService.syncUserRolesWithKeycloak(user)) {
                        syncedUsers++;
                        System.out.println("✅ Utilisateur synchronisé: " + user.getLogin());
                    } else {
                        System.err.println("⚠️ Échec synchronisation: " + user.getLogin());
                    }
                } catch (Exception e) {
                    System.err.println("❌ Erreur sync utilisateur " + user.getLogin() + ": " + e.getMessage());
                }
            }

            System.out.println("🎉 Synchronisation terminée: " + syncedUsers + "/" + totalUsers + " utilisateurs");

        } catch (Exception e) {
            System.err.println("❌ Erreur synchronisation globale: " + e.getMessage());
        }
    }

    // ============ MÉTHODES PRIVÉES ============

    /**
     * Met à jour un utilisateur existant
     */
    private User updateExistingUser(Jwt jwt, String keycloakId, String login, String email, String firstName, String lastName) {
        try {
            System.out.println("🔄 Mise à jour utilisateur existant: " + login);

            User existingUser = userService.getUserByLogin(login);
            boolean hasChanges = false;

            // Mettre à jour les informations Keycloak
            if (existingUser.getKeycloakId() == null || !existingUser.getKeycloakId().equals(keycloakId)) {
                existingUser.setKeycloakId(keycloakId);
                existingUser.setKeycloakEnabled(true);
                hasChanges = true;
            }

            // Mettre à jour les informations personnelles
            if (email != null && !email.equals(existingUser.getEmail())) {
                existingUser.setEmail(email);
                hasChanges = true;
            }

            if (firstName != null && !firstName.equals(existingUser.getPrenom())) {
                existingUser.setPrenom(firstName);
                hasChanges = true;
            }

            if (lastName != null && !lastName.equals(existingUser.getNom())) {
                existingUser.setNom(lastName);
                hasChanges = true;
            }

            // Synchroniser les rôles
            hasChanges |= syncUserRoles(existingUser, jwt);

            // Mettre à jour la dernière connexion
            existingUser.setDernierConnex(LocalDateTime.now());
            hasChanges = true;

            if (hasChanges) {
                existingUser.setLastModifiedBy("keycloak-sync");
                existingUser.setLastModifiedDate(LocalDateTime.now());
                User updatedUser = userService.updateUser(existingUser);
                System.out.println("✅ Utilisateur existant mis à jour: " + login);
                return updatedUser;
            } else {
                System.out.println("ℹ️ Aucun changement pour l'utilisateur: " + login);
                return existingUser;
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur mise à jour utilisateur existant: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Crée un nouvel utilisateur
     */
    private User createNewUser(Jwt jwt, String keycloakId, String login, String email, String firstName, String lastName) {
        try {
            System.out.println("🔄 Création nouvel utilisateur: " + login);

            User newUser = new User();
            newUser.setKeycloakId(keycloakId);
            newUser.setLogin(login);
            newUser.setEmail(email);
            newUser.setPrenom(firstName);
            newUser.setNom(lastName);
            newUser.setStatut(true);
            newUser.setKeycloakEnabled(true);
            newUser.setDernierConnex(LocalDateTime.now());
            newUser.setCreatedBy("keycloak-sync");
            newUser.setCreatedDate(LocalDateTime.now());

            // Assigner les rôles depuis Keycloak
            assignRolesFromKeycloak(newUser, jwt);

            User savedUser = userService.createUserFromKeycloak(newUser);
            System.out.println("✅ Nouvel utilisateur créé: " + login + " (ID: " + savedUser.getId() + ")");

            return savedUser;

        } catch (Exception e) {
            System.err.println("❌ Erreur création nouvel utilisateur: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Met à jour les informations personnelles depuis le JWT
     */
    private boolean updatePersonalInfo(User user, Jwt jwt) {
        boolean hasChanges = false;

        try {
            String email = jwt.getClaimAsString("email");
            String firstName = jwt.getClaimAsString("given_name");
            String lastName = jwt.getClaimAsString("family_name");

            if (email != null && !email.equals(user.getEmail())) {
                user.setEmail(email);
                hasChanges = true;
                System.out.println("🔄 Email mis à jour: " + email);
            }

            if (firstName != null && !firstName.equals(user.getPrenom())) {
                user.setPrenom(firstName);
                hasChanges = true;
                System.out.println("🔄 Prénom mis à jour: " + firstName);
            }

            if (lastName != null && !lastName.equals(user.getNom())) {
                user.setNom(lastName);
                hasChanges = true;
                System.out.println("🔄 Nom mis à jour: " + lastName);
            }

        } catch (Exception e) {
            System.err.println("⚠️ Erreur mise à jour infos personnelles: " + e.getMessage());
        }

        return hasChanges;
    }

    /**
     * Synchronise les rôles de l'utilisateur avec Keycloak
     */
    private boolean syncUserRoles(User user, Jwt jwt) {
        try {
            System.out.println("🔑 Synchronisation rôles pour: " + user.getLogin());

            // Récupérer les rôles depuis le JWT
            Set<String> keycloakRoleNames = extractRoleNamesFromJwt(jwt);
            System.out.println("🔑 Rôles Keycloak: " + keycloakRoleNames);

            // Récupérer les rôles actuels
            Set<String> currentRoleNames = user.getRoles() != null ?
                    user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()) :
                    new HashSet<>();
            System.out.println("🔑 Rôles actuels: " + currentRoleNames);

            // Comparer les rôles
            if (rolesAreEqual(currentRoleNames, keycloakRoleNames)) {
                System.out.println("ℹ️ Rôles identiques, pas de changement");
                return false;
            }

            // Convertir les noms de rôles en entités Role
            Set<Role> newRoles = convertRoleNamesToEntities(keycloakRoleNames);

            // Mettre à jour les rôles
            user.setRoles(newRoles);
            System.out.println("✅ Rôles mis à jour: " + keycloakRoleNames);

            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur synchronisation rôles: " + e.getMessage());
            return false;
        }
    }

    /**
     * Assigne les rôles depuis Keycloak lors de la création
     */
    private void assignRolesFromKeycloak(User user, Jwt jwt) {
        try {
            Set<String> roleNames = extractRoleNamesFromJwt(jwt);
            Set<Role> roles = convertRoleNamesToEntities(roleNames);
            user.setRoles(roles);
            System.out.println("🔑 Rôles assignés au nouvel utilisateur: " + roleNames);
        } catch (Exception e) {
            System.err.println("⚠️ Erreur assignation rôles: " + e.getMessage());
            // Assigner un rôle par défaut
            assignDefaultRole(user);
        }
    }

    /**
     * Extrait les noms de rôles depuis le JWT
     */
    private Set<String> extractRoleNamesFromJwt(Jwt jwt) {
        Set<String> roleNames = new HashSet<>();

        try {
            // Extraire depuis realm_access.roles
            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> realmMap = (Map<String, Object>) realmAccess;
                Object roles = realmMap.get("roles");
                if (roles instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> rolesList = (List<String>) roles;
                    for (String role : rolesList) {
                        String cleanRole = cleanRoleName(role);
                        if (isBusinessRole(cleanRole)) {
                            roleNames.add(cleanRole);
                        }
                    }
                }
            }

            // Si aucun rôle métier trouvé, assigner USER par défaut
            if (roleNames.isEmpty()) {
                roleNames.add("USER");
            }

        } catch (Exception e) {
            System.err.println("⚠️ Erreur extraction rôles JWT: " + e.getMessage());
            roleNames.add("USER");
        }

        return roleNames;
    }

    /**
     * Convertit les noms de rôles en entités Role
     */
    private Set<Role> convertRoleNamesToEntities(Set<String> roleNames) {
        Set<Role> roles = new HashSet<>();

        for (String roleName : roleNames) {
            try {
                Role role = roleRepository.findByName(roleName)
                        .orElseGet(() -> createRoleIfNotExists(roleName));

                if (role != null) {
                    roles.add(role);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Erreur conversion rôle '" + roleName + "': " + e.getMessage());
            }
        }

        return roles;
    }

    /**
     * Crée un rôle s'il n'existe pas
     */
    private Role createRoleIfNotExists(String roleName) {
        try {
            Role newRole = new Role();
            newRole.setName(roleName);
            newRole.setCreatedBy("keycloak-sync");
            newRole.setCreatedDate(LocalDateTime.now());
            newRole.setLastModifiedBy("keycloak-sync");
            newRole.setLastModifiedDate(LocalDateTime.now());

            Role savedRole = roleRepository.save(newRole);
            System.out.println("✅ Rôle créé automatiquement: " + roleName);
            return savedRole;
        } catch (Exception e) {
            System.err.println("❌ Erreur création rôle '" + roleName + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Assigne le rôle USER par défaut
     */
    private void assignDefaultRole(User user) {
        try {
            Role defaultRole = roleRepository.findByName("USER")
                    .orElseGet(() -> createRoleIfNotExists("USER"));

            if (defaultRole != null) {
                Set<Role> roles = new HashSet<>();
                roles.add(defaultRole);
                user.setRoles(roles);
                System.out.println("✅ Rôle USER par défaut assigné");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur assignation rôle par défaut: " + e.getMessage());
        }
    }

    /**
     * Nettoie le nom du rôle
     */
    private String cleanRoleName(String rawRoleName) {
        if (rawRoleName == null) return "USER";

        String cleaned = rawRoleName.trim().toUpperCase();
        if (cleaned.startsWith("ROLE_")) {
            cleaned = cleaned.substring(5);
        }
        return cleaned.isEmpty() ? "USER" : cleaned;
    }

    /**
     * Vérifie si c'est un rôle métier
     */
    private boolean isBusinessRole(String roleName) {
        Set<String> systemRoles = new HashSet<>(Arrays.asList(
                "OFFLINE_ACCESS",
                "UMA_AUTHORIZATION",
                "DEFAULT-ROLES-AGILEO-REALM"
        ));
        return !systemRoles.contains(roleName.toUpperCase());
    }


    /**
     * Compare deux ensembles de rôles
     */
    private boolean rolesAreEqual(Set<String> currentRoles, Set<String> keycloakRoles) {
        if (currentRoles.size() != keycloakRoles.size()) {
            return false;
        }
        return currentRoles.containsAll(keycloakRoles) && keycloakRoles.containsAll(currentRoles);
    }

    // ============ MÉTHODES UTILITAIRES PUBLIQUES ============

    /**
     * Vérifie et corrige les rôles d'un utilisateur
     */
    public void validateAndFixUserRoles(User user) {
        try {
            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                System.out.println("⚠️ Utilisateur sans rôles détecté: " + user.getLogin());
                assignDefaultRole(user);
                userService.updateUser(user);
                System.out.println("✅ Rôle par défaut assigné à: " + user.getLogin());
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur validation rôles: " + e.getMessage());
        }
    }

    /**
     * Force la synchronisation d'un utilisateur spécifique
     */
    public void forceSyncUser(String login) {
        try {
            System.out.println("🔄 Force sync pour utilisateur: " + login);

            User user = userService.getUserByLogin(login);
            boolean success = keycloakAdminService.syncUserRolesWithKeycloak(user);

            if (success) {
                System.out.println("✅ Force sync réussie pour: " + login);
            } else {
                System.err.println("❌ Force sync échouée pour: " + login);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur force sync: " + e.getMessage());
        }
    }
}