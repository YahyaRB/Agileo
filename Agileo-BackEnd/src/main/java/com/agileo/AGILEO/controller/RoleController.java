package com.agileo.AGILEO.controller;

import com.agileo.AGILEO.Dtos.request.RoleRequestDTO;
import com.agileo.AGILEO.Dtos.response.RoleResponseDTO;
import com.agileo.AGILEO.message.ResponseMessage;
import com.agileo.AGILEO.service.RoleService;
import com.agileo.AGILEO.service.Impl.RoleImpService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<RoleResponseDTO> createRole(
            @Valid @RequestBody RoleRequestDTO roleDto,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roleService.createRole(roleDto, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<RoleResponseDTO>> getAllRoles() {
        return ResponseEntity.ok(roleService.findAllRoles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponseDTO> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.findRoleById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseMessage> deleteRole(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.deleteRole(id));
    }



    // ============ NOUVEAUX ENDPOINTS DE SYNCHRONISATION KEYCLOAK ============

    /**
     * ✅ NOUVEAU : Synchronise tous les rôles avec Keycloak
     */
    @PostMapping("/sync-keycloak")
    public ResponseEntity<ResponseMessage> syncAllRolesWithKeycloak(Authentication authentication) {
        try {
            System.out.println("🔄 API: Synchronisation tous les rôles avec Keycloak");
            System.out.println("🔄 Demandé par: " + authentication.getName());

            ResponseMessage response = ((RoleImpService) roleService).syncAllRolesWithKeycloak();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Erreur API sync rôles: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(new ResponseMessage("Error synchronizing roles with Keycloak: " + e.getMessage()));
        }
    }

    /**
     * must delete  : Teste la connexion Keycloak
     */
    @GetMapping("/test-keycloak")
    public ResponseEntity<ResponseMessage> testKeycloakConnection() {
        try {
            System.out.println("🔍 API: Test connexion Keycloak");

            ResponseMessage response = ((RoleImpService) roleService).testKeycloakConnection();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Erreur API test Keycloak: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(new ResponseMessage("Error testing Keycloak connection: " + e.getMessage()));
        }
    }

    @GetMapping("/sync-status")
    public ResponseEntity<ResponseMessage> getSyncStatus() {
        try {
            System.out.println("🔍 API: Vérification statut synchronisation");

            // Tester la connexion Keycloak
            boolean keycloakConnected = ((RoleImpService) roleService).testKeycloakConnection()
                    .getMessage().contains("successful");

            // Compter les rôles
            List<RoleResponseDTO> allRoles = roleService.findAllRoles();
            int totalRoles = allRoles.size();

            String status = String.format(
                    "Sync Status - Keycloak: %s, Total roles: %d",
                    keycloakConnected ? "Connected" : "Disconnected",
                    totalRoles
            );

            return ResponseEntity.ok(new ResponseMessage(status));

        } catch (Exception e) {
            System.err.println("❌ Erreur API statut sync: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(new ResponseMessage("Error checking sync status: " + e.getMessage()));
        }
    }
}