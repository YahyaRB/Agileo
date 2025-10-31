package com.agileo.AGILEO.repository.divalto;

import com.agileo.AGILEO.entity.divalto.DaoLig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DaoLigRepository extends JpaRepository<DaoLig, Integer> {

    /**
     * Trouver toutes les lignes d'une demande
     */
    List<DaoLig> findByDaoNo(BigDecimal daoNo);

    /**
     * Trouver le DAOLGNO maximum pour un DAONO donné
     */
    @Query("SELECT MAX(d.daolgno) FROM DaoLig d WHERE d.daoNo = :daoNo")
    Optional<BigDecimal> findMaxDaolgnoByDaoNo(@Param("daoNo") BigDecimal daoNo);
}