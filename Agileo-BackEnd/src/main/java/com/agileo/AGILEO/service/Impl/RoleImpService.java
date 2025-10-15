package com.agileo.AGILEO.service.Impl;

import com.agileo.AGILEO.Dtos.request.RoleRequestDTO;
import com.agileo.AGILEO.Dtos.response.RoleResponseDTO;
import com.agileo.AGILEO.entity.secondary.Role;
import com.agileo.AGILEO.entity.secondary.User;
import com.agileo.AGILEO.exception.*;
import com.agileo.AGILEO.message.ResponseMessage;
import com.agileo.AGILEO.repository.secondary.RoleRepository;
import com.agileo.AGILEO.repository.secondary.UserRepository;
import com.agileo.AGILEO.service.RoleService;
import com.agileo.AGILEO.service.KeycloakAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoleImpService implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    // ✅ AJOUT : Service pour synchronisation Keycloak
    @Autowired
    private KeycloakAdminService keycloakAdminService;

    public RoleImpService(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public RoleResponseDTO createRole(RoleRequestDTO roleDto, String currentUsername) {


        validateRoleCreation(roleDto);

        // 1. Créer en base locale d'abord
        Role role = new Role();
        role.setName(roleDto.getName().toUpperCase().trim()); // Normaliser le nom
        setAuditFields(role, currentUsername);

        Role savedRole = roleRepository.save(role);


        // 2. ✅ SYNCHRONISER AVEC KEYCLOAK
        try {
            boolean keycloakSuccess = keycloakAdminService.createRoleInKeycloak(savedRole);
            if (keycloakSuccess) {
                System.out.println("✅ Rôle synchronisé avec Keycloak: " + savedRole.getName());
            } else {
                System.err.println("⚠️ Échec synchronisation Keycloak (rôle créé quand même): " + savedRole.getName());
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur synchronisation Keycloak: " + e.getMessage());
            // On continue même si Keycloak échoue pour ne pas bloquer l'application
        }

        System.out.println("🔄 === FIN CRÉATION RÔLE AVEC SYNC ===");
        return mapEntityToDto(savedRole);
    }

    @Override
    public ResponseMessage deleteRole(Long id) {
        System.out.println("🔄 === SUPPRESSION RÔLE AVEC SYNC KEYCLOAK ===");

        Role role = getRoleById(id);
        String roleName = role.getName();
        System.out.println("🔄 Suppression du rôle: " + roleName);

        // Check if any users have this role before deleting
        if (!role.getUsers().isEmpty()) {
            throw new ConflictException("Cannot delete role that is assigned to users");
        }

        // ✅ SYNCHRONISER AVEC KEYCLOAK EN PREMIER
        try {
            boolean keycloakSuccess = keycloakAdminService.deleteRoleFromKeycloak(roleName);
            if (keycloakSuccess) {
                System.out.println("✅ Rôle supprimé de Keycloak: " + roleName);
            } else {
                System.err.println("⚠️ Échec suppression Keycloak: " + roleName);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur suppression Keycloak: " + e.getMessage());
            // On continue même si Keycloak échoue
        }

        // Supprimer de la base locale
        roleRepository.delete(role);
        System.out.println("✅ Rôle supprimé de la base locale: " + roleName);
        System.out.println("🔄 === FIN SUPPRESSION RÔLE AVEC SYNC ===");

        return new ResponseMessage("Role deleted successfully and synchronized with Keycloak");
    }

    @Override
    public List<RoleResponseDTO> findAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public RoleResponseDTO findRoleById(Long id) {
        return mapEntityToDto(getRoleById(id));
    }

    // ============ NOUVELLES MÉTHODES DE SYNCHRONISATION ============

    /**
     * ✅ NOUVELLE MÉTHODE : Synchronise tous les rôles existants avec Keycloak
     */
    public ResponseMessage syncAllRolesWithKeycloak() {
        try {
            System.out.println("🔄 === SYNCHRONISATION TOUS LES RÔLES VERS KEYCLOAK ===");

            List<Role> allRoles = roleRepository.findAll();
            int totalRoles = allRoles.size();
            int successfulSyncs = 0;

            for (Role role : allRoles) {
                try {
                    if (keycloakAdminService.createRoleInKeycloak(role)) {
                        successfulSyncs++;
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Échec sync rôle '" + role.getName() + "': " + e.getMessage());
                }
            }

            String message = String.format("Roles synchronized with Keycloak: %d/%d successful",
                    successfulSyncs, totalRoles);
            System.out.println("✅ " + message);

            return new ResponseMessage(message);

        } catch (Exception e) {
            System.err.println("❌ Erreur sync tous les rôles: " + e.getMessage());
            throw new RuntimeException("Failed to sync roles with Keycloak: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Teste la connexion avec Keycloak
     */
    public ResponseMessage testKeycloakConnection() {
        try {
            boolean connected = keycloakAdminService.testKeycloakConnection();

            if (connected) {
                return new ResponseMessage("Keycloak connection successful");
            } else {
                return new ResponseMessage("Keycloak connection failed");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur test connexion Keycloak: " + e.getMessage());
            return new ResponseMessage("Keycloak connection test failed: " + e.getMessage());
        }
    }

    // ============ MÉTHODES HELPER EXISTANTES ============

    private Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
    }

    private User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private void validateRoleCreation(RoleRequestDTO dto) {
        validateRoleName(dto.getName());
    }

    private void validateRoleName(String name) {
        String normalizedName = name.toUpperCase().trim();
        if (roleRepository.existsByName(normalizedName)) {
            throw new ConflictException("Role name already exists: " + normalizedName);
        }
    }

    private RoleResponseDTO mapEntityToDto(Role entity) {
        RoleResponseDTO dto = new RoleResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        return dto;
    }

    private void setAuditFields(Role role, String username) {
        role.setCreatedBy(username);
        role.setCreatedDate(LocalDateTime.now());
        role.setLastModifiedBy(username);
        role.setLastModifiedDate(LocalDateTime.now());
    }
}