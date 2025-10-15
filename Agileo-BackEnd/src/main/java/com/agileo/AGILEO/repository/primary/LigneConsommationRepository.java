package com.agileo.AGILEO.repository.primary;

import com.agileo.AGILEO.entity.primary.LigneConsommation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface LigneConsommationRepository extends JpaRepository<LigneConsommation, Integer> {

    /**
     * Trouver les lignes par consommation
     */
    List<LigneConsommation> findByNumCons(Integer numCons);


    /**
     * Supprimer toutes les lignes d'une consommation
     */
    @Modifying
    @Transactional
    void deleteByNumCons(Integer numCons);

    @Query("SELECT l FROM LigneConsommation l WHERE l.numCons IN :consommationIds AND l.ref = :referenceArticle")
    List<LigneConsommation> findByNumConsInAndRef(@Param("consommationIds") List<Integer> consommationIds,
                                                  @Param("referenceArticle") String referenceArticle);
    /**
     * Compter les lignes par consommation
     */
    @Query("SELECT COUNT(lc) FROM LigneConsommation lc WHERE lc.numCons = :consommationId")
    Long countByNumCons(@Param("consommationId") Integer consommationId);

    Long countByNumConsIn(List<Integer> numCons);
    long count();

    @Query("SELECT COUNT(l) FROM LigneConsommation l JOIN Consommation c ON l.numCons = c.idBc WHERE c.chantier IN :affaires")
    Long countByAffaireIn(@Param("affaires") List<String> affaires);
    List<LigneConsommation> findByNumConsIn(List<Integer> numCons);
}
