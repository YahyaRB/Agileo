package com.agileo.AGILEO.repository.primary;

import com.agileo.AGILEO.entity.primary.KdnFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface KdnFileRepository extends JpaRepository<KdnFile, Integer> {

    // Trouver les fichiers actifs par groupe
    @Query("SELECT f FROM KdnFile f WHERE f.groupId = :groupId AND f.sysState = 1 ORDER BY f.sysCreationDate DESC")
    List<KdnFile> findActiveFilesByGroupId(@Param("groupId") Integer groupId);


    // NOUVELLE MÉTHODE: Trouver les fichiers actifs par groupe triés par date de création descendante
    @Query("SELECT f FROM KdnFile f WHERE f.groupId = :groupId AND f.sysState = 1 ORDER BY f.sysCreationDate DESC")
    List<KdnFile> findByGroupIdOrderByUploadDateDesc(@Param("groupId") Integer groupId);
    // Trouver un fichier par hash
    Optional<KdnFile> findByHashAndSysState(String hash, Integer sysState);

    // Compter les fichiers actifs dans un groupe
    @Query("SELECT COUNT(f) FROM KdnFile f WHERE f.groupId = :groupId AND f.sysState = 1")
    Long countActiveFilesByGroupId(@Param("groupId") Integer groupId);
    // Calculer la taille totale des fichiers dans un groupe
    @Query("SELECT COALESCE(SUM(f.size), 0) FROM KdnFile f WHERE f.groupId = :groupId AND f.sysState = 1")
    Long getTotalSizeByGroupId(@Param("groupId") Integer groupId);

    // Marquer un fichier comme supprimé (soft delete)
    @Modifying
    @Transactional
    @Query("UPDATE KdnFile f SET f.sysState = 65536, f.sysModificationDate = CURRENT_TIMESTAMP WHERE f.fileId = :fileId")
    void markAsDeleted(@Param("fileId") Integer fileId);




}