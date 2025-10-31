package com.agileo.AGILEO.repository.divalto;

import com.agileo.AGILEO.Dtos.EntProjection;
import com.agileo.AGILEO.entity.divalto.ENT;
import org.springframework.data.jpa.repository.JpaRepository;
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
}