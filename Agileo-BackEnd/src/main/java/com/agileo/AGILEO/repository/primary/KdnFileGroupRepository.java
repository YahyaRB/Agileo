package com.agileo.AGILEO.repository.primary;

import com.agileo.AGILEO.entity.primary.KdnFileGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface KdnFileGroupRepository extends JpaRepository<KdnFileGroup, Integer> {

    // Trouver les groupes actifs
    List<KdnFileGroup> findBySysState(Integer sysState);

    // Trouver un groupe par nom de stockage
    Optional<KdnFileGroup> findByStorageNameAndSysState(String storageName, Integer sysState);

    // Marquer un groupe comme supprimé (soft delete)
    @Modifying
    @Transactional
    @Query("UPDATE KdnFileGroup g SET g.sysState = 0, g.sysModificationDate = CURRENT_TIMESTAMP WHERE g.groupId = :groupId")
    void markAsDeleted(@Param("groupId") Integer groupId);

    // Trouver les groupes vides (sans fichiers actifs)
    @Query("SELECT g FROM KdnFileGroup g WHERE g.sysState = 1 AND g.groupId NOT IN " +
            "(SELECT DISTINCT f.groupId FROM KdnFile f WHERE f.sysState = 1)")
    List<KdnFileGroup> findEmptyGroups();

    // Statistiques
    @Query("SELECT COUNT(g) FROM KdnFileGroup g WHERE g.sysState = 1")
    Long countActiveGroups();
}