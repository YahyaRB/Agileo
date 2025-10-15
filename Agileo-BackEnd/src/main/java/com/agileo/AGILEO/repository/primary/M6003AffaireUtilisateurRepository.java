package com.agileo.AGILEO.repository.primary;

import com.agileo.AGILEO.entity.primary.M6003AffaireUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface M6003AffaireUtilisateurRepository extends JpaRepository<M6003AffaireUtilisateur, Integer> {

    List<M6003AffaireUtilisateur> findByAccessoirId(Integer accessoirId);

    List<M6003AffaireUtilisateur> findBySysState(Integer sysState);

    @Query("SELECT m FROM M6003AffaireUtilisateur m WHERE  m.accessoirId = :accessoirId")
    Optional<M6003AffaireUtilisateur> findByAffaireAndAccessoirId( @Param("accessoirId") Integer accessoirId);

    Optional<M6003AffaireUtilisateur> findByAffaireAndAccessoirId(String affaire, Integer accessoirId);

}