package com.agileo.AGILEO.repository.divalto;

import com.agileo.AGILEO.Dtos.EntProjection;
import com.agileo.AGILEO.entity.divalto.ENT;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface EntRepository extends JpaRepository<ENT, Integer> {
    @Query("SELECT e.bqcpce as bqcpce, e.origine as origine, " +
            "e.deldemdt as deldemdt, e.delaccdt as delaccdt, e.regl as regl , e.depo as depo , e.tiers as tiers , e.projet as projet " +
            "FROM ENT e WHERE e.pino = :pino AND e.picod = 2")
    Optional<EntProjection> findEntInfoByPinoAndPicod(
            @Param("pino") BigDecimal pino);
    @Query("SELECT e FROM ENT e WHERE e.pino = :pino AND e.picod = :picod AND e.ticod = :ticod AND e.dos = :dos")
    Optional<ENT> findByPinoAndPicodAndTicodAndDos(
            @Param("pino") BigDecimal pino,
            @Param("picod") BigDecimal picod,
            @Param("ticod") String ticod,
            @Param("dos") String dos
    );

    @Query("SELECT e FROM ENT e WHERE e.pino = :pino AND e.picod = :picod")
    ENT findByPinoAndPicod(@Param("pino") BigDecimal pino, @Param("picod") BigDecimal picod);

    @Modifying
    @Query("UPDATE ENT e set e.ce4='8' WHERE e.pino = :pino AND e.picod = 2 AND e.dos='1' AND e.ticod='F' ")
    void updateEntBCPerime(@Param("pino") BigDecimal pino);
}