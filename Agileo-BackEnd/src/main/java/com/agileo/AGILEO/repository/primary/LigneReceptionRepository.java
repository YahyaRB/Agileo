package com.agileo.AGILEO.repository.primary;

import com.agileo.AGILEO.entity.primary.LigneReception;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LigneReceptionRepository extends JpaRepository<LigneReception, Integer> {

    /**
     * Trouver les lignes par ID de réception (Ent_ID)
     */
    List<LigneReception> findByEntId(Integer entId);

    /**
     * Trouver les lignes par réception (commande)
     */
    List<LigneReception> findByCommande(Integer commande);

    /**
     * Supprimer toutes les lignes d'une réception par Commande
     */
    @Modifying
    @Transactional
    void deleteByCommande(Integer commande);

    List<LigneReception> findByAffaireIn(List<String> affaires);
    List<LigneReception> findByAffaireInAndSysCreationDateAfter(List<String> affaires, LocalDateTime date);
    List<LigneReception> findBySysCreationDateAfter(LocalDateTime date);
}