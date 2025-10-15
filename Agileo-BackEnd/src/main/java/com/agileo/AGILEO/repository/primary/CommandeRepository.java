package com.agileo.AGILEO.repository.primary;

import com.agileo.AGILEO.entity.primary.Commande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {
    Commande findByCommande(Long commande);
    /**
     * Trouver les commandes par code d'affaire
     */
    List<Commande> findByAffaireCode(String affaireCode);


    List<Commande> findByCe(String ce);

    /**
     * Récupérer tous les fournisseurs distincts
     */
    @Query("SELECT DISTINCT c.fournisseur FROM Commande c WHERE c.fournisseur IS NOT NULL ORDER BY c.fournisseur")
    List<String> findDistinctFournisseurs();

    /**
     * Récupérer les fournisseurs distincts pour une affaire donnée
     */
    @Query("SELECT DISTINCT c.fournisseur FROM Commande c WHERE c.affaireCode = :affaireCode AND c.fournisseur IS NOT NULL ORDER BY c.fournisseur")
    List<String> findDistinctFournisseursByAffaire(@Param("affaireCode") String affaireCode);

    List<Commande> findByAffaireCodeIn(List<String> affaireCodes);
    Long countByAffaireCode(String affaireCode);



}