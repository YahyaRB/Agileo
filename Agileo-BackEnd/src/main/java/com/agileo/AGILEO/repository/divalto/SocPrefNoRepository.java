package com.agileo.AGILEO.repository.divalto;

import com.agileo.AGILEO.entity.divalto.SocPrefNo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SocPrefNoRepository extends JpaRepository<SocPrefNo, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SocPrefNo s WHERE s.picod = 3 AND s.ticod = 'F' AND s.dos = '1'")
    Optional<SocPrefNo> findByPicodAndTicodAndDosForReception();

    @Modifying
    @Query(value = "UPDATE SOCPREFNO SET PINO = PINO + 1, USERMODH = GETDATE() " +
            "WHERE PICOD = 3 AND TICOD = 'F' AND DOS = '1'",
            nativeQuery = true)
    int incrementPinoReception();


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SocPrefNo s WHERE s.picod = 3 AND s.ticod = 'I' AND s.dos = '1'")
    Optional<SocPrefNo> findByPicodAndTicodAndDosForConsommation();

    @Modifying
    @Query(value = "UPDATE SOCPREFNO SET PINO = PINO + 1, USERMODH = GETDATE() " +
            "WHERE PICOD = 3 AND TICOD = 'I' AND DOS = '1'",
            nativeQuery = true)
    int incrementPinoConsommation();



}