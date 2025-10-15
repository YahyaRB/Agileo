package com.agileo.AGILEO.security;

import com.agileo.AGILEO.entity.secondary.User;
import com.agileo.AGILEO.entity.secondary.Role;
import com.agileo.AGILEO.service.UserService;
import com.agileo.AGILEO.repository.secondary.RoleRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserService userService;
    private final RoleRepository roleRepository;

    public CustomJwtAuthenticationConverter(UserService userService, RoleRepository roleRepository) {
        this.userService = userService;
        this.roleRepository = roleRepository;
    }


    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        try {
            String keycloakLogin = jwt.getClaimAsString("preferred_username");
            String keycloakId = jwt.getSubject();

            if (keycloakLogin == null || keycloakLogin.isEmpty()) {
                System.err.println("❌ preferred_username claim manquant dans le JWT");
                throw new IllegalArgumentException("preferred_username claim is missing in JWT");
            }

            // ✅ NOUVEAU : Récupérer ou créer l'utilisateur avec synchronisation complète
            User user = getOrCreateUserWithFullSync(jwt, keycloakLogin, keycloakId);

            if (user == null || user.getId() == null) {
                System.err.println("❌ Utilisateur non sauvegardé correctement");
                throw new RuntimeException("User not properly saved");
            }

            // Vérifier que l'utilisateur est actif
            if (user.getStatut() == null || !user.getStatut()) {
                throw new SecurityException("User account is disabled: " + keycloakLogin);
            }

            // Construire les authorities depuis les rôles locaux ET Keycloak
            Collection<GrantedAuthority> authorities = buildAuthorities(user, jwt);


            return new CustomJwtAuthenticationToken(jwt, authorities, user.getLogin(), user);

        } catch (Exception e) {
            System.err.println("❌ Erreur conversion JWT: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ MÉTHODE PRINCIPALE AMÉLIORÉE : Récupère/crée utilisateur avec synchronisation complète
     */
    private User getOrCreateUserWithFullSync(Jwt jwt, String keycloakLogin, String keycloakId) {
        try {


            if (userService.existsByUsername(keycloakLogin)) {
                // ✅ UTILISATEUR EXISTANT → SYNCHRONISATION COMPLÈTE
                User existingUser = userService.getUserByLogin(keycloakLogin);

                // ✅ NOUVEAU : Synchroniser avec les données Keycloak
                User syncedUser = syncExistingUserWithKeycloak(existingUser, jwt, keycloakId);


                return syncedUser;
            } else {
                // ✅ NOUVEL UTILISATEUR → CRÉATION
                return createUserFromKeycloak(jwt, keycloakLogin, keycloakId);
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur dans getOrCreateUserWithFullSync: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get or create user with sync: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Synchronise un utilisateur existant avec Keycloak
     */
    private User syncExistingUserWithKeycloak(User existingUser, Jwt jwt, String keycloakId) {
        try {

            boolean hasChanges = false;

            // 1. Synchroniser Keycloak ID
            if (existingUser.getKeycloakId() == null || !existingUser.getKeycloakId().equals(keycloakId)) {
                existingUser.setKeycloakId(keycloakId);
                existingUser.setKeycloakEnabled(true);
                hasChanges = true;
                System.out.println("🔄 Keycloak ID mis à jour: " + keycloakId);
            }

            // 2. ✅ NOUVEAU : Synchroniser les informations personnelles
            hasChanges |= syncUserPersonalInfo(existingUser, jwt);

            // 3. ✅ NOUVEAU : Synchroniser les rôles depuis Keycloak
            hasChanges |= syncUserRolesFromKeycloak(existingUser, jwt);

            // 4. Mettre à jour la dernière connexion
            existingUser.setDernierConnex(LocalDateTime.now());
            hasChanges = true;

            // 5. Sauvegarder si des changements ont eu lieu
            if (hasChanges) {
                existingUser.setLastModifiedBy("keycloak-sync");
                existingUser.setLastModifiedDate(LocalDateTime.now());
                User updatedUser = userService.updateUser(existingUser);
                return updatedUser;
            } else {
                System.out.println("ℹ️ Aucun changement détecté, pas de sauvegarde");
                return existingUser;
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur synchronisation utilisateur existant: " + e.getMessage());
            e.printStackTrace();
            // En cas d'erreur de sync, retourner l'utilisateur original
            return existingUser;
        }
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Synchronise les informations personnelles depuis JWT
     */
    private boolean syncUserPersonalInfo(User user, Jwt jwt) {
        boolean hasChanges = false;

        try {
            // Email
            String jwtEmail = jwt.getClaimAsString("email");
            if (jwtEmail != null && !jwtEmail.equals(user.getEmail())) {
                System.out.println("🔄 Email mis à jour: " + user.getEmail() + " → " + jwtEmail);
                user.setEmail(jwtEmail);
                hasChanges = true;
            }

            // Prénom
            String jwtFirstName = jwt.getClaimAsString("given_name");
            if (jwtFirstName != null && !jwtFirstName.equals(user.getPrenom())) {
                System.out.println("🔄 Prénom mis à jour: " + user.getPrenom() + " → " + jwtFirstName);
                user.setPrenom(jwtFirstName);
                hasChanges = true;
            }

            // Nom
            String jwtLastName = jwt.getClaimAsString("family_name");
            if (jwtLastName != null && !jwtLastName.equals(user.getNom())) {
                System.out.println("🔄 Nom mis à jour: " + user.getNom() + " → " + jwtLastName);
                user.setNom(jwtLastName);
                hasChanges = true;
            }

            if (hasChanges) {
                System.out.println("✅ Informations personnelles synchronisées");
            }

        } catch (Exception e) {
            System.err.println("⚠️ Erreur synchronisation infos personnelles: " + e.getMessage());
        }

        return hasChanges;
    }

    /**
     * ✅ MÉTHODE PRINCIPALE : Synchronise les rôles depuis Keycloak
     */
    private boolean syncUserRolesFromKeycloak(User user, Jwt jwt) {
        try {


            // Récupérer les rôles actuels en base
            Set<Role> currentRoles = user.getRoles() != null ? user.getRoles() : new HashSet<>();
            Set<String> currentRoleNames = currentRoles.stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet());



            // Récupérer les rôles depuis Keycloak JWT
            Set<String> keycloakRoleNames = extractRoleNamesFromJwt(jwt);


            // Comparer les rôles
            if (rolesAreEqual(currentRoleNames, keycloakRoleNames)) {
                return false;
            }




            // Convertir les noms de rôles Keycloak en entités Role
            Set<Role> newRoles = convertRoleNamesToEntities(keycloakRoleNames);

            // Assigner les nouveaux rôles
            user.setRoles(newRoles);

            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur synchronisation rôles: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Extrait les noms de rôles depuis le JWT
     */
    private Set<String> extractRoleNamesFromJwt(Jwt jwt) {
        Set<String> roleNames = new HashSet<>();

        try {
            // 1. Rôles depuis realm_access
            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> realmMap = (java.util.Map<String, Object>) realmAccess;
                Object roles = realmMap.get("roles");
                if (roles instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<String> rolesList = (java.util.List<String>) roles;
                    for (String role : rolesList) {
                        String cleanRole = cleanRoleName(role);
                        if (isBusinessRole(cleanRole)) {
                            roleNames.add(cleanRole);

                        }
                    }
                }
            }

            // 2. Si aucun rôle métier trouvé, assigner USER par défaut
            if (roleNames.isEmpty()) {
                roleNames.add("USER");
                System.out.println("🔍 Aucun rôle métier trouvé, ajout du rôle USER par défaut");
            }

        } catch (Exception e) {
            System.err.println("⚠️ Erreur extraction rôles JWT: " + e.getMessage());
            roleNames.add("USER"); // Fallback sécurisé
        }

        return roleNames;
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Vérifie si c'est un rôle métier (pas système)
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
     * ✅ NOUVELLE MÉTHODE : Convertit les noms de rôles en entités Role
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
     * ✅ NOUVELLE MÉTHODE : Compare deux ensembles de rôles
     */
    private boolean rolesAreEqual(Set<String> currentRoles, Set<String> keycloakRoles) {
        if (currentRoles.size() != keycloakRoles.size()) {
            return false;
        }
        return currentRoles.containsAll(keycloakRoles) && keycloakRoles.containsAll(currentRoles);
    }

    /**
     * Crée un nouvel utilisateur à partir des informations Keycloak
     */
    private User createUserFromKeycloak(Jwt jwt, String keycloakLogin, String keycloakId) {
        try {
            System.out.println("🔄 === DÉBUT CRÉATION NOUVEL UTILISATEUR ===");
            System.out.println("🔄 Login: " + keycloakLogin);
            System.out.println("🔄 Keycloak ID: " + keycloakId);

            User newUser = new User();
            newUser.setKeycloakId(keycloakId);
            newUser.setLogin(keycloakLogin);
            newUser.setEmail(jwt.getClaimAsString("email"));
            newUser.setPrenom(jwt.getClaimAsString("given_name"));
            newUser.setNom(jwt.getClaimAsString("family_name"));
            newUser.setStatut(true);
            newUser.setKeycloakEnabled(true);
            newUser.setDernierConnex(LocalDateTime.now());

            System.out.println("📝 Données utilisateur préparées:");
            System.out.println("   - Login: " + newUser.getLogin());
            System.out.println("   - Email: " + newUser.getEmail());
            System.out.println("   - Nom: " + newUser.getNom() + " " + newUser.getPrenom());

            // Appel de la méthode de création (elle gère déjà les rôles)
            User savedUser = userService.createUserFromKeycloak(newUser);

            if (savedUser == null || savedUser.getId() == null) {
                throw new RuntimeException("User creation failed");
            }

            System.out.println("✅ Nouvel utilisateur créé avec succès - ID: " + savedUser.getId());
            System.out.println("🔄 === FIN CRÉATION NOUVEL UTILISATEUR ===");
            return savedUser;

        } catch (Exception e) {
            System.err.println("❌ Erreur création nouvel utilisateur: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create user from Keycloak: " + e.getMessage(), e);
        }
    }

    /**
     * Construit les authorities à partir des rôles utilisateur ET des rôles Keycloak
     */

    private Collection<GrantedAuthority> buildAuthorities(User user, Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1. Rôles depuis la base de données (prioritaires)
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            user.getRoles().forEach(role -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()));

            });
        }

        // 2. Rôles depuis Keycloak (complémentaires)
        try {
            Set<String> keycloakRoles = extractRoleNamesFromJwt(jwt);
            for (String role : keycloakRoles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur extraction rôles Keycloak pour authorities: " + e.getMessage());
        }

        // 3. Rôle par défaut si aucun rôle trouvé
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            System.out.println("🔑 Rôle par défaut ajouté: ROLE_USER");
        }

        return authorities;
    }

    /**
     * Nettoie le nom du rôle (supprime ROLE_ si présent)
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
     * Crée un rôle s'il n'existe pas (version améliorée)
     */
    private Role createRoleIfNotExists(String roleName) {
        try {
            // Double vérification pour éviter les conditions de course
            return roleRepository.findByName(roleName)
                    .orElseGet(() -> {
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
                    });
        } catch (Exception e) {
            System.err.println("❌ Erreur recherche/création rôle '" + roleName + "': " + e.getMessage());
            return null;
        }
    }
}