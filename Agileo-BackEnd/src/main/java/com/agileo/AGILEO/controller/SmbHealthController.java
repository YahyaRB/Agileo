// SmbHealthController.java - VERSION PUBLIQUE (sans authentification requise)

package com.agileo.AGILEO.controller;

import com.agileo.AGILEO.service.SmbStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller pour vérifier la santé de la connexion SMB
 * ENDPOINTS PUBLICS - Pas d'authentification requise
 */
@RestController
@RequestMapping("/api/health")
public class SmbHealthController {

    private static final Logger log = LoggerFactory.getLogger(SmbHealthController.class);

    @Autowired
    private SmbStorageService smbService;

    /**
     * Endpoint de santé pour la connexion SMB (PUBLIC)
     * Accessible sans authentification pour les tests
     */
    @GetMapping("/smb")
    @PreAuthorize("permitAll()")  // Autorise l'accès sans authentification
    public ResponseEntity<Map<String, Object>> checkSmbHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            log.info("=== DÉBUT TEST SANTÉ SMB ===");

            // Informations de configuration (masquer le mot de passe)
            health.put("host", smbService.getHost());
            health.put("share", smbService.getShare());
            health.put("basePath", smbService.getBasePath());

            // Test 1: Connexion
            log.info("Test 1: Connexion au serveur SMB...");
            boolean connected = smbService.testConnection();
            health.put("status", connected ? "UP" : "DOWN");

            if (!connected) {
                health.put("message", "Impossible de se connecter au serveur SMB");
                log.error("❌ Test santé SMB: Connexion échouée");
                return ResponseEntity.status(503).body(health);
            }

            log.info("✅ Connexion OK");

            // Test 2: Écriture
            log.info("Test 2: Test d'écriture...");
            try {
                byte[] testData = ("Health check: " + System.currentTimeMillis()).getBytes();
                String testPath = ".health_check_" + System.currentTimeMillis() + ".txt";

                smbService.upload(new ByteArrayInputStream(testData), testData.length, testPath);
                health.put("writable", true);
                log.info("✅ Écriture OK");

                // Test 3: Suppression
                log.info("Test 3: Test de suppression...");
                try {
                    boolean deleted = smbService.delete(testPath);
                    health.put("deletable", deleted);
                    log.info("✅ Suppression OK");
                } catch (Exception e) {
                    log.warn("⚠️ Suppression échouée (non critique): {}", e.getMessage());
                    health.put("deletable", false);
                    health.put("deleteWarning", e.getMessage());
                }

            } catch (Exception e) {
                log.error("❌ Test d'écriture échoué: {}", e.getMessage());
                health.put("writable", false);
                health.put("writeError", e.getMessage());
            }

            health.put("message", "Connexion SMB fonctionnelle");
            health.put("timestamp", System.currentTimeMillis());
            log.info("✅ Test santé SMB terminé avec succès");

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            log.error("❌ Erreur lors du test santé SMB", e);

            health.put("status", "ERROR");
            health.put("error", e.getMessage());
            health.put("errorType", e.getClass().getSimpleName());
            health.put("timestamp", System.currentTimeMillis());

            // Ajouter des conseils de dépannage
            if (e.getMessage() != null) {
                if (e.getMessage().contains("Connection refused") ||
                        e.getMessage().contains("timeout")) {
                    health.put("suggestion", "Vérifiez que le serveur SMB (192.168.77.4) est accessible et que le port 445 est ouvert");
                } else if (e.getMessage().contains("Authentication failed") ||
                        e.getMessage().contains("Access denied")) {
                    health.put("suggestion", "Vérifiez les credentials (username, password, domain) dans application-dev.properties");
                }
            }

            return ResponseEntity.status(500).body(health);
        }
    }

    /**
     * Endpoint simplifié pour un quick check (PUBLIC)
     */
    @GetMapping("/smb/quick")
    @PreAuthorize("permitAll()")  // Autorise l'accès sans authentification
    public ResponseEntity<String> quickCheck() {
        try {
            boolean connected = smbService.testConnection();
            return connected ?
                    ResponseEntity.ok("✅ SMB OK - Connexion fonctionnelle") :
                    ResponseEntity.status(503).body("❌ SMB DOWN - Connection failed");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ SMB ERROR: " + e.getMessage());
        }
    }

    /**
     * Endpoint pour obtenir juste les infos de config (PUBLIC)
     */
    @GetMapping("/smb/config")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, String>> getConfig() {
        Map<String, String> config = new HashMap<>();
        try {
            config.put("host", smbService.getHost());
            config.put("share", smbService.getShare());
            config.put("basePath", smbService.getBasePath());
            config.put("status", "configured");
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            config.put("status", "error");
            config.put("error", e.getMessage());
            return ResponseEntity.status(500).body(config);
        }
    }
}