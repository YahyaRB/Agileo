package com.agileo.AGILEO.service.Impl;

import com.agileo.AGILEO.Dtos.request.UserRequestDTO;
import com.agileo.AGILEO.Dtos.response.AccessResponseDTO;
import com.agileo.AGILEO.Dtos.response.RoleResponseDTO;
import com.agileo.AGILEO.Dtos.response.UserResponseDTO;
import com.agileo.AGILEO.entity.primary.KdnsAccessor;
import com.agileo.AGILEO.entity.secondary.Acces;
import com.agileo.AGILEO.entity.secondary.Role;
import com.agileo.AGILEO.entity.secondary.User;
import com.agileo.AGILEO.exception.*;
import com.agileo.AGILEO.message.ResponseMessage;
import com.agileo.AGILEO.repository.primary.KdnsAccessorRepository;
import com.agileo.AGILEO.repository.secondary.AccesRepository;
import com.agileo.AGILEO.repository.secondary.RoleRepository;
import com.agileo.AGILEO.repository.secondary.UserRepository;
import com.agileo.AGILEO.service.KeycloakAdminService;
import com.agileo.AGILEO.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AccesRepository accesRepository;
    private final KdnsAccessorRepository kdnsAccessorRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakAdminService keycloakAdminService;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           AccesRepository accesRepository,
                           KdnsAccessorRepository kdnsAccessorRepository,
                           PasswordEncoder passwordEncoder,
                           KeycloakAdminService keycloakAdminService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.accesRepository = accesRepository;
        this.kdnsAccessorRepository = kdnsAccessorRepository;
        this.passwordEncoder = passwordEncoder;
        this.keycloakAdminService = keycloakAdminService;
    }

    // ============ MÉTHODES POUR KEYCLOAK AUTHENTICATION ============

    @Override
    public User getUserByLogin(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new UserNotFoundException("User not found with login: " + login));
    }

    @Override
    public User updateUser(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID cannot be null for update");
        }

        if (!userRepository.existsById(user.getId())) {
            throw new UserNotFoundException("User not found with id: " + user.getId());
        }

        return userRepository.save(user);
    }

    @Override
    public List<User> getActiveUsers() {
        return userRepository.findByStatutTrue();
    }

    // ============ MÉTHODES CRUD ============

    @Override
    @Transactional
    public UserResponseDTO createUser(UserRequestDTO userDto, String currentUsername) {
        validateUserCreation(userDto);

        User user = new User();
        mapDtoToEntity(userDto, user);
        setAuditFields(user, currentUsername);

        // Sauvegarder l'utilisateur
        User savedUser = userRepository.save(user);

        // Synchroniser avec KdnsAccessor
        syncUserWithAccessor(savedUser.getId(), currentUsername);

        return mapEntityToDto(savedUser);
    }

    @Override
    @Transactional
    public ResponseMessage updateUser(Long id, UserRequestDTO userDto, String currentUsername) {
        User user = getUserById(id);
        validateUserUpdate(user, userDto);

        // Sauvegarder les anciennes valeurs pour la synchronisation
        String oldLogin = user.getLogin();
        String oldFirstName = user.getPrenom();
        String oldLastName = user.getNom();
        String oldEmail = user.getEmail();

        mapDtoToEntity(userDto, user);
        user.setLastModifiedBy(currentUsername);
        user.setLastModifiedDate(LocalDateTime.now());

        userRepository.save(user);

        // Synchroniser les changements avec KdnsAccessor
        updateAccessorFromUser(user, oldLogin, oldFirstName, oldLastName, oldEmail);

        return new ResponseMessage("User updated successfully");
    }

    @Override
    @Transactional
    public ResponseMessage deleteUser(Long id) {
        User user = getUserById(id);
        user.setStatut(false);
        user.setLastModifiedDate(LocalDateTime.now());
        userRepository.save(user);

        // Désactiver l'accessor correspondant
        try {
            Optional<KdnsAccessor> accessor = kdnsAccessorRepository.findByLogin(user.getLogin());
            if (accessor.isPresent()) {
                KdnsAccessor kdnsAccessor = accessor.get();
                kdnsAccessor.setEndValid(LocalDateTime.now());
                kdnsAccessorRepository.save(kdnsAccessor);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la désactivation de l'accessor: " + e.getMessage());
        }

        return new ResponseMessage("User deactivated successfully");
    }

    @Override
    public List<UserResponseDTO> findAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDTO findUserById(Long id) {
        User user = getUserById(id);
        return mapEntityToDto(user);
    }

    @Override
    public UserResponseDTO findUserByLogin(String login) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec login: " + login));
        return mapEntityToDto(user);
    }

    // ============ MÉTHODES DE GESTION STATUT ============

    @Override
    @Transactional
    public ResponseMessage activateUser(Long userId, String currentUsername) {
        User user = getUserById(userId);
        user.setStatut(true);
        user.setLastModifiedBy(currentUsername);
        user.setLastModifiedDate(LocalDateTime.now());
        userRepository.save(user);

        // Réactiver l'accessor correspondant
        try {
            Optional<KdnsAccessor> accessor = kdnsAccessorRepository.findByLogin(user.getLogin());
            if (accessor.isPresent()) {
                KdnsAccessor kdnsAccessor = accessor.get();
                kdnsAccessor.setEndValid(null); // Retirer la date de fin
                kdnsAccessor.setStartValid(LocalDateTime.now());
                kdnsAccessorRepository.save(kdnsAccessor);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la réactivation de l'accessor: " + e.getMessage());
        }

        keycloakAdminService.enableUserInKeycloak(user.getLogin());
        keycloakAdminService.syncUserRolesWithKeycloak(user);
        return new ResponseMessage("User activated successfully");
    }

    @Override
    @Transactional
    public ResponseMessage deactivateUser(Long userId, String currentUsername) {
        User user = getUserById(userId);
        user.setStatut(false);
        user.setLastModifiedBy(currentUsername);
        user.setLastModifiedDate(LocalDateTime.now());
        userRepository.save(user);

        // Désactiver l'accessor correspondant
        try {
            Optional<KdnsAccessor> accessor = kdnsAccessorRepository.findByLogin(user.getLogin());
            if (accessor.isPresent()) {
                KdnsAccessor kdnsAccessor = accessor.get();
                kdnsAccessor.setEndValid(LocalDateTime.now());
                kdnsAccessorRepository.save(kdnsAccessor);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la désactivation de l'accessor: " + e.getMessage());
        }

        keycloakAdminService.disableUserInKeycloak(user.getLogin());
        return new ResponseMessage("User deactivated successfully");
    }

    @Override
    public boolean isActive(String username) {
        return userRepository.findByLogin(username)
                .map(User::getStatut)
                .orElse(false);
    }

    // ============ MÉTHODES DE VALIDATION ============

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByLogin(username);
    }

    @Override
    public boolean existsByUsernameExcludingId(String username, Long excludeId) {
        if (excludeId == null) {
            return existsByUsername(username);
        }
        return userRepository.existsByLoginAndIdNot(username, excludeId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByMatricule(String matricule) {
        return userRepository.existsByMatricule(matricule);
    }

    // ============ GESTION DES RÔLES ============

    @Override
    @Transactional
    public ResponseMessage addRoleToUser(Long userId, Long roleId, String currentUsername) {
        User user = getUserById(userId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }

        if (user.getRoles().stream().anyMatch(r -> r.getId().equals(roleId))) {
            throw new ConflictException("User already has this role");
        }

        user.getRoles().add(role);
        user.setLastModifiedBy(currentUsername);
        user.setLastModifiedDate(LocalDateTime.now());
        userRepository.save(user);

        try {
            keycloakAdminService.createRoleInKeycloak(role);
            keycloakAdminService.assignRoleToUserInKeycloak(user.getLogin(), role.getName());
        } catch (Exception e) {
            System.err.println("Erreur synchronisation Keycloak: " + e.getMessage());
        }

        return new ResponseMessage("Role added successfully");
    }

    @Override
    @Transactional
    public ResponseMessage removeRoleFromUser(Long userId, Long roleId, String currentUsername) {
        User user = getUserById(userId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (user.getRoles() == null || user.getRoles().stream().noneMatch(r -> r.getId().equals(roleId))) {
            return new ResponseMessage("User does not have this role");
        }

        user.getRoles().removeIf(r -> r.getId().equals(roleId));
        user.setLastModifiedBy(currentUsername);
        user.setLastModifiedDate(LocalDateTime.now());
        userRepository.save(user);

        try {
            keycloakAdminService.removeRoleFromUserInKeycloak(user.getLogin(), role.getName());
        } catch (Exception e) {
            System.err.println("Erreur synchronisation Keycloak: " + e.getMessage());
        }

        return new ResponseMessage("Role removed successfully");
    }

    @Override
    public Set<RoleResponseDTO> getUserRoles(Long userId) {
        User user = getUserById(userId);
        if (user.getRoles() == null) {
            return new HashSet<>();
        }
        return user.getRoles().stream()
                .map(this::mapRoleToDto)
                .collect(Collectors.toSet());
    }

    // ============ GESTION DES ACCÈS ============

    @Override
    @Transactional
    public ResponseMessage addAccesToUser(Long userId, Long accesId, String currentUsername) {
        User user = getUserById(userId);
        Acces acces = accesRepository.findById(accesId)
                .orElseThrow(() -> new ResourceNotFoundException("Access not found with id: " + accesId));

        if (user.getAcces() == null) {
            user.setAcces(new LinkedList<>());
        }

        if (user.getAcces().stream().anyMatch(a -> a.getId().equals(accesId))) {
            throw new ConflictException("User already has this access");
        }

        user.getAcces().add(acces);
        user.setLastModifiedBy(currentUsername);
        user.setLastModifiedDate(LocalDateTime.now());
        userRepository.save(user);

        return new ResponseMessage("Access added successfully");
    }

    @Override
    @Transactional
    public ResponseMessage removeAccesFromUser(Long userId, Long accesId, String currentUsername) {
        User user = getUserById(userId);

        if (user.getAcces() == null) {
            return new ResponseMessage("User has no access to remove");
        }

        boolean removed = user.getAcces().removeIf(a -> a.getId().equals(accesId));

        if (!removed) {
            return new ResponseMessage("User does not have this access");
        }

        user.setLastModifiedBy(currentUsername);
        user.setLastModifiedDate(LocalDateTime.now());
        userRepository.save(user);

        return new ResponseMessage("Access removed successfully");
    }

    @Override
    public List<AccessResponseDTO> getUserAcces(Long userId) {
        User user = getUserById(userId);
        if (user.getAcces() == null) {
            return new ArrayList<>();
        }
        return user.getAcces().stream()
                .map(this::mapAccesToDto)
                .collect(Collectors.toList());
    }

    // ============ GESTION DES LIAISONS USER-ACCESSOR ============

    @Override
    public Integer getAccessorIdByUserId(Long userId) {
        User user = getUserById(userId);
        try {
            Optional<KdnsAccessor> accessor = kdnsAccessorRepository.findByLogin(user.getLogin());
            return accessor.map(KdnsAccessor::getAccessorId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional
    public ResponseMessage linkUserToAccessor(Long userId, Integer accessorId, String currentUsername) {
        User user = getUserById(userId);

        // Vérifier que l'accessor existe
        KdnsAccessor accessor = kdnsAccessorRepository.findById(accessorId)
                .orElseThrow(() -> new ResourceNotFoundException("Accessor not found with id: " + accessorId));

        // Mettre à jour le login de l'accessor avec celui de l'utilisateur
        accessor.setLogin(user.getLogin());
        accessor.setFirstName(user.getPrenom());
        accessor.setLastName(user.getNom());
        accessor.setFullName((user.getPrenom() + " " + user.getNom()).trim());
        accessor.setEmail(user.getEmail());

        kdnsAccessorRepository.save(accessor);

        return new ResponseMessage("User linked to accessor successfully");
    }

    @Override
    @Transactional
    public ResponseMessage unlinkUserFromAccessor(Long userId, String currentUsername) {
        User user = getUserById(userId);

        try {
            Optional<KdnsAccessor> accessorOpt = kdnsAccessorRepository.findByLogin(user.getLogin());
            if (accessorOpt.isPresent()) {
                KdnsAccessor accessor = accessorOpt.get();
                // Ne pas supprimer, mais marquer comme non lié
                accessor.setLogin(null);
                accessor.setEmail(null);
                kdnsAccessorRepository.save(accessor);
            }
            return new ResponseMessage("User unlinked from accessor successfully");
        } catch (Exception e) {
            throw new RuntimeException("Error unlinking user from accessor: " + e.getMessage());
        }
    }

    @Override
    public boolean isUserLinkedToAccessor(Long userId) {
        try {
            return getAccessorIdByUserId(userId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    // ============ SYNCHRONISATION AVEC ACCESSOR ============

    @Override
    @Transactional
    public ResponseMessage syncUserWithAccessor(Long userId, String currentUsername) {
        User user = getUserById(userId);

        try {
            // Chercher un accessor existant avec le même login
            Optional<KdnsAccessor> existingAccessor = kdnsAccessorRepository.findByLogin(user.getLogin());

            if (existingAccessor.isPresent()) {
                // Mettre à jour l'accessor existant
                updateAccessorFromUser(user, user.getLogin(), user.getPrenom(), user.getNom(), user.getEmail());
                return new ResponseMessage("User synchronized with existing accessor");
            } else {
                // Créer un nouvel accessor
                createAccessorFromUser(user);
                return new ResponseMessage("New accessor created for user");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la synchronisation: " + e.getMessage());
            return new ResponseMessage("Synchronization completed with errors: " + e.getMessage());
        }
    }

    // ============ MÉTHODES UTILITAIRES ============

    @Override
    public List<UserResponseDTO> searchUsers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAllUsers();
        }

        return userRepository.findAll().stream()
                .filter(user ->
                        user.getLogin().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                user.getNom().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                user.getPrenom().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchTerm.toLowerCase()))
                )
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }



    @Override
    public List<UserResponseDTO> getActiveUserss() {
        return userRepository.findByStatutTrue().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponseDTO> getInactiveUsers() {
        return userRepository.findByStatutFalse().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    // ============ MÉTHODES KEYCLOAK ============

    @Override
    @Transactional
    public User createUserFromKeycloak(User user) {
        try {
            if (user.getLogin() == null || user.getLogin().trim().isEmpty()) {
                throw new IllegalArgumentException("Login is required");
            }

            // Vérifier si l'utilisateur existe déjà
            Optional<User> existingUser = userRepository.findByLogin(user.getLogin());
            if (existingUser.isPresent()) {
                return existingUser.get();
            }

            if (user.getKeycloakId() != null && !user.getKeycloakId().trim().isEmpty()) {
                Optional<User> existingByKeycloakId = userRepository.findByKeycloakId(user.getKeycloakId());
                if (existingByKeycloakId.isPresent()) {
                    return existingByKeycloakId.get();
                }
            }

            LocalDateTime now = LocalDateTime.now();
            user.setCreatedDate(now);
            user.setCreatedBy("keycloak-sync");
            user.setLastModifiedDate(now);
            user.setLastModifiedBy("keycloak-sync");

            if (user.getStatut() == null) {
                user.setStatut(true);
            }
            if (user.getKeycloakEnabled() == null) {
                user.setKeycloakEnabled(true);
            }

            // Gérer le matricule
            if (user.getMatricule() == null || user.getMatricule().trim().isEmpty()) {
                user.setMatricule(generateUniqueMatricule(user.getLogin()));
            } else {
                if (userRepository.existsByMatricule(user.getMatricule())) {
                    user.setMatricule(generateUniqueMatricule(user.getLogin()));
                }
            }

            // Gérer l'email
            if (user.getEmail() != null && user.getEmail().trim().isEmpty()) {
                user.setEmail(null);
            } else if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
                user.setEmail(null);
            }

            if (user.getKeycloakId() == null || user.getKeycloakId().trim().isEmpty()) {
                throw new IllegalArgumentException("Keycloak ID is required for user creation");
            }

            // Initialiser les collections
            if (user.getRoles() == null) {
                user.setRoles(new HashSet<>());
            }
            if (user.getAcces() == null) {
                user.setAcces(new LinkedList<>());
            }

            // NOUVEAU : Créer ou lier l'accessor AVANT de sauvegarder l'utilisateur
            KdnsAccessor accessor = createOrLinkAccessorForUser(user);

            // IMPORTANT : Stocker l'idAgelio dans l'utilisateur
            if (accessor != null && accessor.getAccessorId() != null) {
                user.setIdAgelio(accessor.getAccessorId().toString());
                System.out.println("✅ idAgelio assigné: " + user.getIdAgelio() + " pour l'utilisateur: " + user.getLogin());
            }

            // Sauvegarder l'utilisateur avec l'idAgelio
            User savedUser = userRepository.save(user);
            userRepository.flush();

            if (savedUser == null || savedUser.getId() == null) {
                throw new RuntimeException("User save operation failed - no ID generated");
            }

            // Assigner un rôle par défaut si nécessaire
            if (savedUser.getRoles().isEmpty()) {
                assignDefaultRole(savedUser);
            }

            System.out.println("✅ Utilisateur créé avec idAgelio: " + savedUser.getIdAgelio());
            return savedUser;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create user from Keycloak: " + e.getMessage(), e);
        }
    }




    public ResponseMessage syncAllUsersWithKeycloak() {
        try {
            List<User> allUsers = userRepository.findAll();
            int totalUsers = allUsers.size();
            int successfulSyncs = 0;

            for (User user : allUsers) {
                try {
                    if (keycloakAdminService.syncUserRolesWithKeycloak(user)) {
                        successfulSyncs++;
                    }
                } catch (Exception e) {
                    System.err.println("Erreur sync pour " + user.getLogin() + ": " + e.getMessage());
                }
            }

            String message = String.format("Users synchronized with Keycloak: %d/%d successful", successfulSyncs, totalUsers);
            return new ResponseMessage(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to sync users with Keycloak: " + e.getMessage(), e);
        }
    }

    public ResponseMessage syncUserWithKeycloak(Long userId) {
        try {
            User user = getUserById(userId);
            boolean syncSuccess = keycloakAdminService.syncUserRolesWithKeycloak(user);

            String message = syncSuccess ?
                    "User '" + user.getLogin() + "' synchronized with Keycloak successfully" :
                    "Partial synchronization for user '" + user.getLogin() + "'";

            return new ResponseMessage(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to sync user with Keycloak: " + e.getMessage(), e);
        }
    }

    // ============ MÉTHODES PRIVÉES HELPER ============

    private User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private void validateUserCreation(UserRequestDTO dto) {
        if (userRepository.existsByLogin(dto.getLogin())) {
            throw new ConflictException("Username is already taken");
        }
        if (dto.getEmail() != null && userRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("Email is already in use");
        }
        if (dto.getMatricule() != null && userRepository.existsByMatricule(dto.getMatricule())) {
            throw new ConflictException("Matricule is already in use");
        }
    }

    private void validateUserUpdate(User user, UserRequestDTO dto) {
        if (!user.getLogin().equals(dto.getLogin()) && userRepository.existsByLogin(dto.getLogin())) {
            throw new ConflictException("Username is already taken");
        }
        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("Email is already in use");
        }
        if (dto.getMatricule() != null && !dto.getMatricule().equals(user.getMatricule()) && userRepository.existsByMatricule(dto.getMatricule())) {
            throw new ConflictException("Matricule is already in use");
        }
    }

    private KdnsAccessor createOrLinkAccessorForUser(User user) {
        try {
            // 1. Chercher d'abord un accessor existant avec le même login
            Optional<KdnsAccessor> existingAccessor = kdnsAccessorRepository.findByLogin(user.getLogin());

            if (existingAccessor.isPresent()) {
                System.out.println("🔄 Accessor existant trouvé pour: " + user.getLogin());
                KdnsAccessor accessor = existingAccessor.get();

                // Mettre à jour les informations si nécessaire
                accessor.setFirstName(user.getPrenom());
                accessor.setLastName(user.getNom());
                accessor.setFullName((user.getPrenom() + " " + user.getNom()).trim());
                accessor.setEmail(user.getEmail());

                return kdnsAccessorRepository.save(accessor);
            }

            // 2. Chercher un accessor existant avec le même nom/prénom
            List<KdnsAccessor> potentialAccessors = kdnsAccessorRepository
                    .findByFirstNameAndLastName(user.getPrenom(), user.getNom());

            if (!potentialAccessors.isEmpty()) {
                // Prendre le premier accessor trouvé et l'associer
                KdnsAccessor accessor = potentialAccessors.get(0);
                System.out.println("🔄 Liaison avec accessor existant (nom/prénom): " + accessor.getAccessorId());

                accessor.setLogin(user.getLogin());
                accessor.setEmail(user.getEmail());

                return kdnsAccessorRepository.save(accessor);
            }

            // 3. Créer un nouvel accessor
            System.out.println("🆕 Création d'un nouvel accessor pour: " + user.getLogin());
            return createNewAccessorForUser(user);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création/liaison de l'accessor: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    private KdnsAccessor createNewAccessorForUser(User user) {
        try {
            KdnsAccessor accessor = new KdnsAccessor();

            // Générer un nouvel ID d'accessor
            Integer maxId = kdnsAccessorRepository.findMaxAccessorId();
            accessor.setAccessorId(maxId != null ? maxId + 1 : 1);

            accessor.setLogin(user.getLogin());
            accessor.setFirstName(user.getPrenom());
            accessor.setLastName(user.getNom());
            accessor.setFullName((user.getPrenom() + " " + user.getNom()).trim());
            accessor.setEmail(user.getEmail());
            accessor.setStartValid(LocalDateTime.now());
            accessor.setEndValid(null);
            accessor.setAccessorType(1); // Type utilisateur par défaut
            accessor.setExternalUser(0); // Utilisateur interne par défaut

            KdnsAccessor savedAccessor = kdnsAccessorRepository.save(accessor);
            System.out.println("✅ Nouvel accessor créé avec ID: " + savedAccessor.getAccessorId());

            return savedAccessor;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création de l'accessor: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private UserResponseDTO mapEntityToDto(User entity) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(entity.getId());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        dto.setDernierConnexion(entity.getDernierConnex());
        dto.setMatricule(entity.getMatricule());
        dto.setLogin(entity.getLogin());
        dto.setNom(entity.getNom());
        dto.setPrenom(entity.getPrenom());
        dto.setEmail(entity.getEmail());
        dto.setStatut(entity.getStatut());
        dto.setIdAgelio(entity.getIdAgelio());
        dto.setRoles(getUserRoles(entity.getId()));
        dto.setAcces(getUserAcces(entity.getId()));
        return dto;
    }

    private void setAuditFields(User user, String username) {
        user.setCreatedBy(username);
        user.setCreatedDate(LocalDateTime.now());
        user.setLastModifiedBy(username);
        user.setLastModifiedDate(LocalDateTime.now());
    }

    private RoleResponseDTO mapRoleToDto(Role entity) {
        RoleResponseDTO dto = new RoleResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        return dto;
    }

    private AccessResponseDTO mapAccesToDto(Acces entity) {
        AccessResponseDTO dto = new AccessResponseDTO();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        return dto;
    }

    private void mapDtoToEntity(UserRequestDTO dto, User entity) {
        entity.setLogin(dto.getLogin());
        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setEmail(dto.getEmail());
        entity.setMatricule(dto.getMatricule());

        if (dto.getStatut() != null) {
            entity.setStatut(dto.getStatut());
        } else {
            entity.setStatut(true);
        }
    }

    private String generateUniqueMatricule(String login) {
        // Générer un matricule basé sur le login + timestamp
        String baseMatricule = login.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (baseMatricule.length() > 6) {
            baseMatricule = baseMatricule.substring(0, 6);
        }

        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        String matricule = baseMatricule + timestamp;

        // Vérifier l'unicité et ajuster si nécessaire
        int counter = 1;
        String finalMatricule = matricule;
        while (userRepository.existsByMatricule(finalMatricule)) {
            finalMatricule = matricule + counter;
            counter++;
            if (counter > 999) {
                // Fallback en cas de problème
                finalMatricule = "USR" + System.currentTimeMillis();
                break;
            }
        }

        return finalMatricule;
    }

    private void assignDefaultRole(User user) {
        try {
            // Assigner le rôle "USER" par défaut s'il existe
            Optional<Role> defaultRole = roleRepository.findByName("USER");
            if (defaultRole.isPresent()) {
                if (user.getRoles() == null) {
                    user.setRoles(new HashSet<>());
                }
                user.getRoles().add(defaultRole.get());
                userRepository.save(user);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'assignation du rôle par défaut: " + e.getMessage());
        }
    }

    private void updateAccessorFromUser(User user, String oldLogin, String oldFirstName, String oldLastName, String oldEmail) {
        try {
            Optional<KdnsAccessor> accessorOpt = kdnsAccessorRepository.findByLogin(oldLogin);
            if (accessorOpt.isPresent()) {
                KdnsAccessor accessor = accessorOpt.get();

                // Mettre à jour les informations
                accessor.setLogin(user.getLogin());
                accessor.setFirstName(user.getPrenom());
                accessor.setLastName(user.getNom());
                accessor.setFullName((user.getPrenom() + " " + user.getNom()).trim());
                accessor.setEmail(user.getEmail());

                kdnsAccessorRepository.save(accessor);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour de l'accessor: " + e.getMessage());
        }
    }

    private void createAccessorFromUser(User user) {
        try {
            KdnsAccessor accessor = new KdnsAccessor();

            // Générer un nouvel ID d'accessor
            Integer maxId = kdnsAccessorRepository.findMaxAccessorId();
            accessor.setAccessorId(maxId != null ? maxId + 1 : 1);

            accessor.setLogin(user.getLogin());
            accessor.setFirstName(user.getPrenom());
            accessor.setLastName(user.getNom());
            accessor.setFullName((user.getPrenom() + " " + user.getNom()).trim());
            accessor.setEmail(user.getEmail());
            accessor.setStartValid(LocalDateTime.now());
            accessor.setEndValid(null);
            accessor.setAccessorType(1); // Type utilisateur par défaut
            accessor.setExternalUser(0); // Utilisateur interne par défaut

            kdnsAccessorRepository.save(accessor);
        } catch (Exception e) {
            System.err.println("Erreur lors de la création de l'accessor: " + e.getMessage());
        }
    }
}