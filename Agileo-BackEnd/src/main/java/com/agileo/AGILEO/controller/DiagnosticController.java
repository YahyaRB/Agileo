package com.agileo.AGILEO.controller;

import com.agileo.AGILEO.repository.secondary.UserRepository;
import com.agileo.AGILEO.repository.secondary.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CONTROLLER DE DIAGNOSTIC TEMPORAIRE - COMPATIBLE JAVA 8
 * À SUPPRIMER EN PRODUCTION !
 *
 * Permet de diagnostiquer les problèmes de déploiement en temps réel
 */
@RestController
@RequestMapping("/api/diagnostic")
@CrossOrigin(origins = "*")
public class DiagnosticController {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private DataSource primaryDataSource;

    @Autowired(required = false)
    private DataSource secondaryDataSource;

    @Autowired(required = false)
    private UserRepository userRepository;

    @Autowired(required = false)
    private RoleRepository roleRepository;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    /**
     * Test basique - Vérifie que le controller fonctionne
     * GET /api/diagnostic/ping
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Le serveur Spring Boot fonctionne correctement");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("serverPort", environment.getProperty("server.port"));
        response.put("contextPath", environment.getProperty("server.servlet.context-path"));
        return ResponseEntity.ok(response);
    }

    /**
     * Liste TOUS les endpoints disponibles
     * GET /api/diagnostic/endpoints
     */
    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, Object>> listAllEndpoints() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> endpoints = new ArrayList<>();

        Map<RequestMappingInfo, HandlerMethod> handlerMethods =
                requestMappingHandlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mapping = entry.getKey();
            HandlerMethod method = entry.getValue();

            Map<String, String> endpointInfo = new HashMap<>();

            // Patterns (URLs)
            if (mapping.getPatternsCondition() != null) {
                endpointInfo.put("url", mapping.getPatternsCondition().getPatterns().toString());
            }

            // Méthodes HTTP
            if (mapping.getMethodsCondition() != null) {
                endpointInfo.put("methods", mapping.getMethodsCondition().getMethods().toString());
            }

            // Classe et méthode
            endpointInfo.put("handler", method.getBeanType().getSimpleName() + "." + method.getMethod().getName());

            endpoints.add(endpointInfo);
        }

        // Grouper par controller
        Map<String, List<Map<String, String>>> groupedEndpoints = endpoints.stream()
                .collect(Collectors.groupingBy(e -> {
                    String handler = e.get("handler");
                    return handler.substring(0, handler.indexOf('.'));
                }));

        response.put("status", "SUCCESS");
        response.put("totalEndpoints", endpoints.size());
        response.put("controllersCount", groupedEndpoints.size());
        response.put("controllers", new ArrayList<>(groupedEndpoints.keySet()));
        response.put("endpoints", endpoints);
        response.put("groupedByController", groupedEndpoints);

        return ResponseEntity.ok(response);
    }

    /**
     * Vérifie les controllers importants
     * GET /api/diagnostic/controllers
     */
    @GetMapping("/controllers")
    public ResponseEntity<Map<String, Object>> checkControllers() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Boolean> controllers = new HashMap<>();

        String[] requiredControllers = {
                "UserController",
                "RoleController",
                "AuthController",
                "AffaireController",
                "DemandeAchatController",
                "ReceptionController",
                "ConsommationController"
        };

        for (String controllerName : requiredControllers) {
            try {
                String beanName = controllerName.substring(0, 1).toLowerCase() + controllerName.substring(1);
                boolean exists = applicationContext.containsBean(beanName);
                controllers.put(controllerName, exists);
            } catch (Exception e) {
                controllers.put(controllerName, false);
            }
        }

        boolean allPresent = true;
        for (Boolean value : controllers.values()) {
            if (!value) {
                allPresent = false;
                break;
            }
        }

        List<String> missingControllers = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : controllers.entrySet()) {
            if (!entry.getValue()) {
                missingControllers.add(entry.getKey());
            }
        }

        response.put("status", allPresent ? "SUCCESS" : "WARNING");
        response.put("message", allPresent ? "Tous les controllers sont chargés" : "Certains controllers sont manquants");
        response.put("controllers", controllers);
        response.put("missingControllers", missingControllers);

        return ResponseEntity.ok(response);
    }

    /**
     * Test de connexion aux bases de données
     * GET /api/diagnostic/database
     */
    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> checkDatabase() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> databases = new HashMap<>();

        // Test Primary DataSource
        if (primaryDataSource != null) {
            Connection conn = null;
            try {
                conn = primaryDataSource.getConnection();
                Map<String, Object> primaryInfo = new HashMap<>();
                primaryInfo.put("status", "CONNECTED");
                primaryInfo.put("url", conn.getMetaData().getURL());
                primaryInfo.put("driver", conn.getMetaData().getDriverName());
                databases.put("primary", primaryInfo);
            } catch (Exception e) {
                Map<String, Object> primaryError = new HashMap<>();
                primaryError.put("status", "ERROR");
                primaryError.put("error", e.getMessage());
                databases.put("primary", primaryError);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        } else {
            Map<String, Object> primaryNotConfig = new HashMap<>();
            primaryNotConfig.put("status", "NOT_CONFIGURED");
            databases.put("primary", primaryNotConfig);
        }

        // Test Secondary DataSource
        if (secondaryDataSource != null) {
            Connection conn = null;
            try {
                conn = secondaryDataSource.getConnection();
                Map<String, Object> secondaryInfo = new HashMap<>();
                secondaryInfo.put("status", "CONNECTED");
                secondaryInfo.put("url", conn.getMetaData().getURL());
                secondaryInfo.put("driver", conn.getMetaData().getDriverName());
                databases.put("secondary", secondaryInfo);
            } catch (Exception e) {
                Map<String, Object> secondaryError = new HashMap<>();
                secondaryError.put("status", "ERROR");
                secondaryError.put("error", e.getMessage());
                databases.put("secondary", secondaryError);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        } else {
            Map<String, Object> secondaryNotConfig = new HashMap<>();
            secondaryNotConfig.put("status", "NOT_CONFIGURED");
            databases.put("secondary", secondaryNotConfig);
        }

        response.put("status", "SUCCESS");
        response.put("databases", databases);
        return ResponseEntity.ok(response);
    }

    /**
     * Compte les utilisateurs et rôles
     * GET /api/diagnostic/data
     */
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> checkData() {
        Map<String, Object> response = new HashMap<>();

        try {
            long userCount = userRepository != null ? userRepository.count() : -1;
            long roleCount = roleRepository != null ? roleRepository.count() : -1;

            response.put("status", "SUCCESS");
            response.put("usersCount", userCount);
            response.put("rolesCount", roleCount);
            response.put("message", String.format("Base de données accessible - %d utilisateurs, %d rôles", userCount, roleCount));
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Informations sur l'environnement
     * GET /api/diagnostic/environment
     */
    @GetMapping("/environment")
    public ResponseEntity<Map<String, Object>> getEnvironment() {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> properties = new HashMap<>();

        String[] importantProps = {
                "spring.application.name",
                "server.port",
                "server.servlet.context-path",
                "spring.datasource.primary.jdbc-url",
                "spring.datasource.secondary.jdbc-url",
                "keycloak.admin.server-url",
                "keycloak.admin.realm",
                "cors.allowed-origins",
                "spring.profiles.active"
        };

        for (String prop : importantProps) {
            properties.put(prop, environment.getProperty(prop, "NOT_SET"));
        }

        response.put("status", "SUCCESS");
        response.put("properties", properties);
        response.put("activeProfiles", Arrays.asList(environment.getActiveProfiles()));
        response.put("javaVersion", System.getProperty("java.version"));

        try {
            response.put("springBootVersion", org.springframework.boot.SpringBootVersion.getVersion());
        } catch (Exception e) {
            response.put("springBootVersion", "UNKNOWN");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Rapport complet de diagnostic
     * GET /api/diagnostic/full
     */
    @GetMapping("/full")
    public ResponseEntity<Map<String, Object>> fullDiagnostic() {
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. Ping
            ResponseEntity<Map<String, Object>> pingResult = ping();
            response.put("ping", pingResult.getBody());

            // 2. Controllers
            ResponseEntity<Map<String, Object>> controllersResult = checkControllers();
            response.put("controllers", controllersResult.getBody());

            // 3. Database
            ResponseEntity<Map<String, Object>> databaseResult = checkDatabase();
            response.put("database", databaseResult.getBody());

            // 4. Data
            ResponseEntity<Map<String, Object>> dataResult = checkData();
            response.put("data", dataResult.getBody());

            // 5. Environment
            ResponseEntity<Map<String, Object>> envResult = getEnvironment();
            response.put("environment", envResult.getBody());

            // 6. Endpoints count
            ResponseEntity<Map<String, Object>> endpointsResult = listAllEndpoints();
            Map<String, Object> endpointsInfo = endpointsResult.getBody();
            response.put("endpointsCount", endpointsInfo != null ? endpointsInfo.get("totalEndpoints") : 0);

            response.put("status", "SUCCESS");
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("message", "Diagnostic complet terminé");

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            response.put("stackTrace", Arrays.toString(e.getStackTrace()));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Test si les fichiers statiques Angular sont accessibles
     * GET /api/diagnostic/static-files
     */
    @GetMapping("/static-files")
    public ResponseEntity<Map<String, Object>> checkStaticFiles() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Vérifier si on peut accéder à index.html via le ClassLoader
            boolean indexHtmlExists = getClass().getClassLoader().getResource("static/index.html") != null;

            response.put("status", indexHtmlExists ? "SUCCESS" : "WARNING");
            response.put("indexHtmlInClasspath", indexHtmlExists);
            response.put("message", indexHtmlExists ?
                    "Les fichiers statiques Angular sont présents dans le WAR" :
                    "ATTENTION: index.html manquant dans le WAR!");

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Vérifie la configuration de sécurité
     * GET /api/diagnostic/security
     */
    @GetMapping("/security")
    public ResponseEntity<Map<String, Object>> checkSecurity() {
        Map<String, Object> response = new HashMap<>();

        String keycloakUrl = environment.getProperty("keycloak.admin.server-url");
        String keycloakRealm = environment.getProperty("keycloak.admin.realm");

        response.put("status", "SUCCESS");
        response.put("keycloakConfigured", keycloakUrl != null && keycloakRealm != null);
        response.put("keycloakUrl", keycloakUrl);
        response.put("keycloakRealm", keycloakRealm);

        return ResponseEntity.ok(response);
    }
}