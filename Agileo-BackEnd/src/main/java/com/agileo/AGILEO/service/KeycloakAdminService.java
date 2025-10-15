package com.agileo.AGILEO.service;

import com.agileo.AGILEO.entity.secondary.Role;
import com.agileo.AGILEO.entity.secondary.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
public class KeycloakAdminService {

    @Value("${keycloak.admin.server-url:http://192.168.77.125:8080}")
    private String keycloakServerUrl;

    @Value("${keycloak.admin.realm:RB-realm}")
    private String realm;

    @Value("${keycloak.admin.client-id:admin-cli}")
    private String adminClientId;

    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin.password:m@ster0312}")
    private String adminPassword;

    private final RestTemplate restTemplate = new RestTemplate();
    private String cachedAccessToken;
    private long tokenExpirationTime;

    @PostConstruct
    public void init() {
        System.out.println("🔧 KeycloakAdminService initialisé pour synchronisation bidirectionnelle");
        System.out.println("🔧 Server URL: " + keycloakServerUrl);
        System.out.println("🔧 Realm: " + realm);
    }

    // ============ GESTION DE L'AUTHENTIFICATION ADMIN ============
    private String getAdminAccessToken() {
        try {
            if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpirationTime) {
                return cachedAccessToken;
            }
            String tokenUrl = keycloakServerUrl + "/realms/master/protocol/openid-connect/token";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = String.format(
                    "grant_type=password&client_id=%s&username=%s&password=%s",
                    adminClientId, adminUsername, adminPassword
            );
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenData = response.getBody();
                cachedAccessToken = (String) tokenData.get("access_token");
                Integer expiresIn = (Integer) tokenData.get("expires_in");
                tokenExpirationTime = System.currentTimeMillis() + ((expiresIn - 30) * 1000L);
                return cachedAccessToken;
            }
            throw new RuntimeException("Échec récupération token admin");
        } catch (Exception e) {
            return null;
        }
    }
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String token = getAdminAccessToken();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    // ============ GESTION DES RÔLES ============

    public boolean createRoleInKeycloak(Role role) {
        try {
            String token = getAdminAccessToken();
            if (token == null) {
                return false;
            }
            String url = String.format("%s/admin/realms/%s/roles", keycloakServerUrl, realm);
            Map<String, Object> roleData = new HashMap<>();
            roleData.put("name", role.getName());
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(roleData, createAuthHeaders());
            ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return true;
            }
            return false;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteRoleFromKeycloak(String roleName) {
        try {
            String token = getAdminAccessToken();
            if (token == null) {
                return false;
            }
            String url = String.format("%s/admin/realms/%s/roles/%s", keycloakServerUrl, realm, roleName);
            HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders());
            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
            return true;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // ============ GESTION DES UTILISATEURS ============

    public Map<String, Object> findKeycloakUserByUsername(String username) {
        try {
            String token = getAdminAccessToken();
            if (token == null) return null;
            String url = String.format("%s/admin/realms/%s/users?username=%s&exact=true",
                    keycloakServerUrl, realm, username);
            HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, request, List.class);
            if (response.getBody() != null && !response.getBody().isEmpty()) {
                return (Map<String, Object>) response.getBody().get(0);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean assignRoleToUserInKeycloak(String username, String roleName) {
        try {
            String token = getAdminAccessToken();
            if (token == null) {
                return false;
            }
            Map<String, Object> keycloakUser = findKeycloakUserByUsername(username);
            if (keycloakUser == null) {
                return false;
            }
            String userId = (String) keycloakUser.get("id");
            Map<String, Object> roleInfo = getKeycloakRoleInfo(roleName);
            if (roleInfo == null) {
                return false;
            }
            String url = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm",
                    keycloakServerUrl, realm, userId);

            List<Map<String, Object>> roles = Arrays.asList(roleInfo);

            HttpEntity<List<Map<String, Object>>> request = new HttpEntity<>(roles, createAuthHeaders());
            ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return true;
            }

            return false;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean removeRoleFromUserInKeycloak(String username, String roleName) {
        try {
            String token = getAdminAccessToken();
            if (token == null) {
                return false;
            }
            Map<String, Object> keycloakUser = findKeycloakUserByUsername(username);
            if (keycloakUser == null) {
                return false;
            }
            String userId = (String) keycloakUser.get("id");
            Map<String, Object> roleInfo = getKeycloakRoleInfo(roleName);
            if (roleInfo == null) {
                return false;
            }
            String url = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm",
                    keycloakServerUrl, realm, userId);

            List<Map<String, Object>> roles = Arrays.asList(roleInfo);

            HttpEntity<List<Map<String, Object>>> request = new HttpEntity<>(roles, createAuthHeaders());
            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
            return true;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> getKeycloakRoleInfo(String roleName) {
        try {
            String url = String.format("%s/admin/realms/%s/roles/%s", keycloakServerUrl, realm, roleName);
            HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }
    // activate et Désactive

    public boolean enableUserInKeycloak(String username) {
        Map<String, Object> keycloakUser = findKeycloakUserByUsername(username);
        String userId = (String) keycloakUser.get("id");
        String url = String.format("%s/admin/realms/%s/users/%s", keycloakServerUrl, realm, userId);
        Map<String, Object> userData = new HashMap<>();
        userData.put("enabled", true);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(userData, createAuthHeaders());
        restTemplate.exchange(url, HttpMethod.PUT, request, Void.class);
        return true;

    }

    public boolean disableUserInKeycloak(String username) {
        Map<String, Object> keycloakUser = findKeycloakUserByUsername(username);
        String userId = (String) keycloakUser.get("id");
        String url = String.format("%s/admin/realms/%s/users/%s", keycloakServerUrl, realm, userId);
        Map<String, Object> userData = new HashMap<>();
        userData.put("enabled", false);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(userData, createAuthHeaders());
        restTemplate.exchange(url, HttpMethod.PUT, request, Void.class);
        return true;
    }

    /**
     * Vérifie si un utilisateur est activé dans Keycloak
     */
    public boolean isUserEnabledInKeycloak(String username) {
        try {
            Map<String, Object> keycloakUser = findKeycloakUserByUsername(username);
            if (keycloakUser == null) {
                return false;
            }
            Object enabled = keycloakUser.get("enabled");
            return enabled != null && (Boolean) enabled;
        } catch (Exception e) {
            return false;
        }
    }
    // ============ MÉTHODES UTILITAIRES ============

    /**
     * Teste la connectivité avec Keycloak
     */
    public boolean testKeycloakConnection() {
        try {
            String token = getAdminAccessToken();
            return token != null && !token.isEmpty();
        } catch (Exception e) {
            System.err.println("❌ Test connexion Keycloak échoué: " + e.getMessage());
            return false;
        }
    }

    /**
     * Synchronise tous les rôles d'un utilisateur avec Keycloak
     */
    public boolean syncUserRolesWithKeycloak(User user) {
        try {


            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                System.out.println("ℹ️ Aucun rôle à synchroniser pour: " + user.getLogin());
                return true;
            }
            boolean overallSuccess = true;
            for (Role role : user.getRoles()) {
                boolean roleCreated = createRoleInKeycloak(role);
                boolean assigned = assignRoleToUserInKeycloak(user.getLogin(), role.getName());
                if (!roleCreated || !assigned) {
                    overallSuccess = false;
                } else {
                }
            }
            return overallSuccess;
        } catch (Exception e) {
            return false;
        }
    }
}