package com.agileo.AGILEO.repository.secondary;

import com.agileo.AGILEO.entity.secondary.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ============ MÉTHODES DE RECHERCHE PAR IDENTIFIANTS ============

    Optional<User> findByLogin(String login);

    Optional<User> findByEmail(String email);

    Optional<User> findByMatricule(String matricule);

    Optional<User> findByKeycloakId(String keycloakId);

    // ============ MÉTHODES D'EXISTENCE/VALIDATION ============

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);

    boolean existsByMatricule(String matricule);

    boolean existsByKeycloakId(String keycloakId);

    /**
     * Vérifier l'existence d'un login en excluant un ID spécifique
     */
    boolean existsByLoginAndIdNot(String login, Long id);

    /**
     * Vérifier l'existence d'un email en excluant un ID spécifique
     */
    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * Vérifier l'existence d'un matricule en excluant un ID spécifique
     */
    boolean existsByMatriculeAndIdNot(String matricule, Long id);

    // ============ MÉTHODES DE RECHERCHE PAR STATUT ============

    List<User> findByStatutTrue();

    List<User> findByStatutFalse();

    List<User> findByStatut(Boolean statut);

    // ============ MÉTHODES DE RECHERCHE AVANCÉE ============

    /**
     * Rechercher des utilisateurs par terme de recherche (nom, prénom, login, email)
     */
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.nom) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.prenom) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.login) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.matricule) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchUsers(@Param("searchTerm") String searchTerm);

    /**
     * Trouver tous les utilisateurs avec Keycloak activé
     */
    List<User> findByKeycloakEnabledTrue();

    /**
     * Trouver tous les utilisateurs avec Keycloak désactivé
     */
    List<User> findByKeycloakEnabledFalse();

    /**
     * Trouver des utilisateurs par rôle
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    /**
     * Trouver des utilisateurs par code d'accès
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.acces a WHERE a.code = :accessCode")
    List<User> findByAccessCode(@Param("accessCode") String accessCode);

    /**
     * Compter les utilisateurs actifs
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.statut = true")
    Long countActiveUsers();

    /**
     * Compter les utilisateurs inactifs
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.statut = false")
    Long countInactiveUsers();

    /**
     * Trouver des utilisateurs créés par un utilisateur spécifique
     */
    List<User> findByCreatedBy(String createdBy);

    /**
     * Trouver des utilisateurs modifiés récemment
     */
    @Query("SELECT u FROM User u WHERE u.lastModifiedDate >= CURRENT_DATE")
    List<User> findRecentlyModifiedUsers();

    /**
     * Trouver des utilisateurs sans email
     */
    @Query("SELECT u FROM User u WHERE u.email IS NULL OR u.email = ''")
    List<User> findUsersWithoutEmail();

    /**
     * Trouver des utilisateurs avec des logins spécifiques (pour la synchronisation)
     */
    @Query("SELECT u FROM User u WHERE u.login IN :logins")
    List<User> findByLoginIn(@Param("logins") List<String> logins);
}