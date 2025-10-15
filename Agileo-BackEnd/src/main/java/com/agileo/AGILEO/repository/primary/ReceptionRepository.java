package com.agileo.AGILEO.repository.primary;

import com.agileo.AGILEO.entity.primary.Reception;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceptionRepository extends JpaRepository<Reception, Integer> {

    List<Reception> findByCommande(Integer commande);

    List<Reception> findBySysCreatorId(Integer sysCreatorId);

    Page<Reception> findBySysCreatorId(Integer sysCreatorId, Pageable pageable);

    // ✅ MÉTHODE MANQUANTE - Compter les réceptions par utilisateur
    Long countBySysCreatorId(Integer sysCreatorId);

    // ✅ ALIAS POUR LA COMPATIBILITÉ (si utilisé ailleurs)
    default Long countTotalReceptionByLogin(Integer accessorId) {
        return countBySysCreatorId(accessorId);
    }

    // Méthodes de recherche
    @Query("SELECT r FROM Reception r WHERE " +
            "CAST(r.numero AS string) LIKE %:search% OR " +
            "CAST(r.commande AS string) LIKE %:search% OR " +
            "LOWER(r.pinotiers) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Reception> searchReceptions(@Param("search") String search, Pageable pageable);

    @Query("SELECT r FROM Reception r WHERE r.sysCreatorId = :creatorId AND (" +
            "CAST(r.numero AS string) LIKE %:search% OR " +
            "CAST(r.commande AS string) LIKE %:search% OR " +
            "LOWER(r.pinotiers) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Reception> searchReceptionsByCreator(
            @Param("creatorId") Integer creatorId,
            @Param("search") String search,
            Pageable pageable
    );

    // ✅ BONUS: Méthodes utiles pour les statistiques du dashboard

    // Compter les réceptions par statut
    @Query("SELECT COUNT(r) FROM Reception r WHERE r.sysState = :statut")
    Long countByStatut(@Param("statut") Integer statut);

    // Compter les réceptions par statut et créateur
    @Query("SELECT COUNT(r) FROM Reception r WHERE r.sysCreatorId = :creatorId AND r.sysState = :statut")
    Long countByCreatorAndStatut(@Param("creatorId") Integer creatorId, @Param("statut") Integer statut);

    // Compter toutes les réceptions
    @Query("SELECT COUNT(r) FROM Reception r")
    Long countTotalReceptions();

    // Compter les réceptions envoyées (statut = 1)
    @Query("SELECT COUNT(r) FROM Reception r WHERE r.sysState = 1")
    Long countReceptionsEnvoyees();

    // Compter les brouillons (statut = 0 ou null)
    @Query("SELECT COUNT(r) FROM Reception r WHERE r.sysState IS NULL OR r.sysState = 0")
    Long countReceptionsBrouillons();

    // Compter les réceptions envoyées par utilisateur
    @Query("SELECT COUNT(r) FROM Reception r WHERE r.sysCreatorId = :creatorId AND r.sysState = 1")
    Long countReceptionsEnvoyeesByCreator(@Param("creatorId") Integer creatorId);

    // Compter les brouillons par utilisateur
    @Query("SELECT COUNT(r) FROM Reception r WHERE r.sysCreatorId = :creatorId AND (r.sysState IS NULL OR r.sysState = 0)")
    Long countReceptionsBrouillonsByCreator(@Param("creatorId") Integer creatorId);
}