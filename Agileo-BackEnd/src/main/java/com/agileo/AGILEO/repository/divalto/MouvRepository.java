package com.agileo.AGILEO.repository.divalto;

import com.agileo.AGILEO.entity.divalto.ENT;
import com.agileo.AGILEO.entity.divalto.MOUV;
import com.agileo.AGILEO.entity.divalto.Mvtl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface MouvRepository extends JpaRepository<MOUV, Integer> {

    @Query("SELECT m FROM MOUV m WHERE m.cdno = :cdno AND m.picod = 2 AND m.ticod = 'F' AND m.dos = '1' AND m.ref = :ref")
    Optional<MOUV> findLigneCommandeByPinoAndRef(
            @Param("cdno") BigDecimal cdno,
            @Param("ref") String ref
    );
    @Query("SELECT max(m.bllg) FROM MOUV m WHERE m.cdno = :cdno AND m.picod = 3 AND m.ticod = 'F' AND m.dos = '1' AND m.ref = :ref")
    Integer maxNbLigneBLByBC(
            @Param("cdno") BigDecimal cdno,
            @Param("ref") String ticod
    );

    @Query("SELECT count(m.mouvId) FROM MOUV m WHERE m.cdno = :cdno AND m.picod = 2 AND m.ticod = 'F' AND m.dos = '1' ")
    Integer countLigneBC(
            @Param("cdno") BigDecimal cdno);

    @Query("SELECT m FROM MOUV m WHERE m.depo = :depo AND m.crtotmt > 0 AND m.dos = '1' AND m.ref = :ref ORDER BY m.mouvId DESC")
    List<MOUV> findCoutArticleByDepoList(@Param("depo") String depo, @Param("ref") String ref);

    // Méthode wrapper qui retourne le premier
    default Optional<MOUV> coutArticleByDepo(String depo, String ref) {
        List<MOUV> results = findCoutArticleByDepoList(depo, ref);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

   /* @Query(value = "SELECT TOP 1 COALESCE(CAST(NULLIF(RTRIM(LTRIM(CRTOTMT)), '') AS DECIMAL(18,2)), 0) " +
            "FROM MOUV WHERE DEPO = :depo AND DOS = '1' AND REF = :ref " +
            "AND CRTOTMT IS NOT NULL AND RTRIM(LTRIM(CRTOTMT)) != '' " +
            "ORDER BY MOUV_ID DESC", nativeQuery = true)*/
    @Query(value = "SELECT TOP 1 PUB FROM MOUV WHERE DEPO = :depo AND DOS = '1' AND REF = :ref " +
            "AND TICOD='F' AND PUB>0 " , nativeQuery = true)
    BigDecimal getCrtotmtByDepoAndRef(@Param("depo") String depo, @Param("ref") String ref);

}