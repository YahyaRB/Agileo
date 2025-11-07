package com.agileo.AGILEO.repository.divalto;

import com.agileo.AGILEO.entity.divalto.MOUV;
import com.agileo.AGILEO.entity.divalto.SocPrefNo;
import com.agileo.AGILEO.entity.divalto.Socno;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;

public interface SocnoRepository extends JpaRepository<Socno, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s.enrno FROM Socno s WHERE  s.dos = '1'")
    BigDecimal findByNumEnrgForUpdate();

    @Modifying
    @Query(value = "UPDATE SOCNO SET ENRNO = ENRNO + 1 WHERE DOS = '1'",
            nativeQuery = true)
    int incrementNumEnrg();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s.vtlno FROM Socno s WHERE  s.dos = '1'")
    BigDecimal findByVtlnoForUpdate();

    @Modifying
    @Query(value = "UPDATE SOCNO SET VTLNO = VTLNO + 1 WHERE DOS = '1'",
            nativeQuery = true)
    int incrementVtlnoEnrg();
}
