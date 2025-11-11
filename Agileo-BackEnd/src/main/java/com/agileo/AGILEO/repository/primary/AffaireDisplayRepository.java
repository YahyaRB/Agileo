package com.agileo.AGILEO.repository.primary;

import com.agileo.AGILEO.entity.primary.AffaireDisplay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface AffaireDisplayRepository extends JpaRepository<AffaireDisplay, String> {

    /**
     * Trouver une affaire par son code pour l'affichage
     * @param affaireCode le code de l'affaire
     * @return l'affaire avec son libellé (même si non autorisée en saisie)
     */
    Optional<AffaireDisplay> findById(String affaireCode);
}