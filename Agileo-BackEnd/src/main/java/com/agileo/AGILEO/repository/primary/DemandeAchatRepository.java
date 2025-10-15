package com.agileo.AGILEO.repository.primary;

import com.agileo.AGILEO.entity.primary.DemandeAchat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface DemandeAchatRepository extends JpaRepository<DemandeAchat, Integer>, PagingAndSortingRepository<DemandeAchat, Integer> {
    // Recherche par chantier
    List<DemandeAchat> findByChantier(String chantier);
    List<DemandeAchat> findByNumDa(String numDa);
    List<DemandeAchat> findByLogin(Integer login);
    Page<DemandeAchat> findByLogin(Integer login, Pageable pageable);
    List<DemandeAchat> findByChantierIn(List<String> chantiers);
    List<DemandeAchat> findByChantierInAndDateDaAfter(List<String> chantiers, LocalDateTime date);
    List<DemandeAchat> findByDateDaAfter(LocalDateTime date);
    Long countByChantierAndStatutNot(String chantier, Integer statut);
    long count();
    Long countByStatut(Integer statut);
    Long countByLogin(Integer login);
    @Query("SELECT COUNT(d) FROM DemandeAchat d WHERE d.login = :login AND d.statut = :statut")
    Long countByLoginAndStatut(@Param("login") Integer login, @Param("statut") Integer statut);
    @Query("SELECT count(c) FROM DemandeAchat c WHERE c.login = :login")
    Long countTotalDAByLogin(@Param("login") Integer login);

}
