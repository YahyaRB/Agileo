package com.agileo.AGILEO.service;

import com.agileo.AGILEO.Dtos.request.UserRequestDTO;
import com.agileo.AGILEO.Dtos.response.AccessResponseDTO;
import com.agileo.AGILEO.Dtos.response.RoleResponseDTO;
import com.agileo.AGILEO.Dtos.response.UserResponseDTO;
import com.agileo.AGILEO.entity.secondary.User;
import com.agileo.AGILEO.message.ResponseMessage;

import java.util.List;
import java.util.Set;

public interface UserService {

    // ============ MÉTHODES DE BASE CRUD ============
    UserResponseDTO createUser(UserRequestDTO userDto, String currentUsername);
    ResponseMessage updateUser(Long id, UserRequestDTO userDto, String currentUsername);
    ResponseMessage deleteUser(Long id);
    List<UserResponseDTO> findAllUsers();
    UserResponseDTO findUserById(Long id);
    UserResponseDTO findUserByLogin(String login);

    // ============ MÉTHODES POUR KEYCLOAK AUTHENTICATION ============
    User getUserByLogin(String login);
    User updateUser(User user);
    List<User> getActiveUsers();
    User createUserFromKeycloak(User user);

    // ============ GESTION STATUT UTILISATEUR ============
    ResponseMessage activateUser(Long userId, String currentUsername);
    ResponseMessage deactivateUser(Long userId, String currentUsername);
    boolean isActive(String username);

    // ============ MÉTHODES DE VALIDATION ============
    boolean existsByUsername(String username);
    boolean existsByUsernameExcludingId(String username, Long excludeId);
    boolean existsByEmail(String email);
    boolean existsByMatricule(String matricule);

    // ============ GESTION DES RÔLES ============
    ResponseMessage addRoleToUser(Long userId, Long roleId, String currentUsername);
    ResponseMessage removeRoleFromUser(Long userId, Long roleId, String currentUsername);
    Set<RoleResponseDTO> getUserRoles(Long userId);

    // ============ GESTION DES ACCÈS ============
    ResponseMessage addAccesToUser(Long userId, Long accesId, String currentUsername);
    ResponseMessage removeAccesFromUser(Long userId, Long accesId, String currentUsername);
    List<AccessResponseDTO> getUserAcces(Long userId);

    // ============ GESTION DES LIAISONS USER-ACCESSOR ============
    Integer getAccessorIdByUserId(Long userId);
    ResponseMessage linkUserToAccessor(Long userId, Integer accessorId, String currentUsername);
    ResponseMessage unlinkUserFromAccessor(Long userId, String currentUsername);
    boolean isUserLinkedToAccessor(Long userId);

    // ============ SYNCHRONISATION AVEC ACCESSOR ============
    ResponseMessage syncUserWithAccessor(Long userId, String currentUsername);

    // ============ MÉTHODES UTILITAIRES ============
    List<UserResponseDTO> searchUsers(String searchTerm);
    List<UserResponseDTO> getActiveUserss(); // Cette méthode retourne List<UserResponseDTO>
    List<UserResponseDTO> getInactiveUsers();
}