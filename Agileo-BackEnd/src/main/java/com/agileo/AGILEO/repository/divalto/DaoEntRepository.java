package com.agileo.AGILEO.repository.divalto;

import com.agileo.AGILEO.entity.divalto.DaoEnt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DaoEntRepository extends JpaRepository<DaoEnt, Long> {

    @Query("SELECT COALESCE(MAX(d.daoNo), 0) FROM DaoEnt d WHERE d.dos = :dos")
    Long findMaxDaoNoByDos(Integer dos);

    @Query("SELECT COALESCE(MAX(d.daoEntId), 0) FROM DaoEnt d")
    Long findMaxDaoEnt();
}