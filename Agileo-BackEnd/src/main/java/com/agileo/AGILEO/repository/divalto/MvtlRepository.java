package com.agileo.AGILEO.repository.divalto;

import com.agileo.AGILEO.entity.divalto.MOUV;
import com.agileo.AGILEO.entity.divalto.Mvtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MvtlRepository extends JpaRepository<Mvtl, Integer> {



    @Query(value = "SELECT * FROM MVTL WHERE PINO = :pino AND PICOD = 2 AND TICOD = 'F' AND DOS = '1' AND REF = :ref", nativeQuery = true)
    Optional<Mvtl> findMouvementByEnrno(
            @Param("ref") String ref,
            @Param("pino") BigDecimal pino
    );

    @Query("SELECT MAX(m.cr) FROM Mvtl m WHERE m.depo = :depo AND m.cr > 0 AND m.dos = '1' AND m.ref = :ref ORDER BY m.mvtlId DESC")
    BigDecimal coutArticleByDepo(@Param("depo") String depo, @Param("ref") String ref);



    @Query(value = "SELECT MAX(MVTL_ID) FROM MVTL", nativeQuery = true)
    Integer findMaxMvtlId();

    @Query(value = "SELECT TOP 1 COALESCE(CAST(NULLIF(RTRIM(LTRIM(CR)), '') AS DECIMAL(18,2)), 0) " +
            "FROM MVTL WHERE DEPO = :depo AND DOS = '1' AND REF = :ref " +
            "AND CR IS NOT NULL AND CR > 0 ORDER BY MVTL_ID DESC",
            nativeQuery = true)
    BigDecimal getCrByDepoAndRef(@Param("depo") String depo, @Param("ref") String ref);

    @Query("SELECT m FROM Mvtl m WHERE m.depo = :depo AND m.stqte > 0 AND m.dos = '1' AND m.ref = :ref order by m.mvtlId asc")
    List<Mvtl> listeArticlesaConsommer(@Param("depo") String depo, @Param("ref") String ref);

    @Modifying
    @Query(value = "UPDATE MVTL SET STQTE = :nouveauStock WHERE VTLNO = :vtlno", nativeQuery = true)
    void updateStockOnly(@Param("vtlno") BigDecimal vtlno, @Param("nouveauStock") BigDecimal nouveauStock);
}
