package com.agileo.AGILEO.repository.secondary;



import com.agileo.AGILEO.entity.secondary.PieceJointe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PieceJointeRepository extends JpaRepository<PieceJointe, Long> {

    List<PieceJointe> findByDemandeAchatId(Integer demandeAchatId);

    List<PieceJointe> findByConsommationId(Integer consommationId);

    @Query("SELECT p FROM PieceJointe p WHERE p.demandeAchatId = :demandeId AND p.id = :pieceJointeId")
    PieceJointe findByDemandeAchatIdAndId(@Param("demandeId") Long demandeId, @Param("pieceJointeId") Long pieceJointeId);

    @Query("SELECT p FROM PieceJointe p WHERE p.receptionId = :receptionId")
    List<PieceJointe> findByReceptionId(@Param("receptionId") Integer receptionId);

    void deleteByDemandeAchatId(Long demandeAchatId);
}