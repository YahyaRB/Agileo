package com.agileo.AGILEO.repository.primary;

import com.agileo.AGILEO.entity.primary.Consommation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConsommationRepository extends JpaRepository<Consommation, Integer> {

    /**
     * Trouver les consommations par chantier (affaire)
     */
    List<Consommation> findByChantier(String chantier);
    List<Consommation> findByLogin(Integer login);
    List<Consommation> findByChantierIn(List<String> chantiers);
    List<Consommation> findByChantierInAndDateCAfter(List<String> chantiers, LocalDateTime date);
    List<Consommation> findByDateCAfter(LocalDateTime date);
    long count();
    Long countByChantierIn(List<String> chantiers);
    @Query("SELECT count(c) FROM Consommation c WHERE c.login = :login")
    Long countTotalConsommationByLogin(@Param("login") Integer login);

    List<Consommation> findByStatut(Integer statut);

    @Query("SELECT c FROM Consommation c WHERE c.statut = 0 AND c.chantier LIKE %:depot")
    List<Consommation> findBrouillonByDepot(@Param("depot") String depot);

    @Query("SELECT FUNCTION('YEAR', c.dateC) AS yr, FUNCTION('MONTH', c.dateC) AS mn, COUNT(c) " +
            "FROM Consommation c " +
            "WHERE c.login = :login AND c.dateC IS NOT NULL AND c.dateC >= :startDate " +
            "GROUP BY FUNCTION('YEAR', c.dateC), FUNCTION('MONTH', c.dateC) " +
            "ORDER BY FUNCTION('YEAR', c.dateC), FUNCTION('MONTH', c.dateC)")
    List<Object[]> countMonthlyByLogin(@Param("login") Integer login, @Param("startDate") LocalDateTime startDate);
}