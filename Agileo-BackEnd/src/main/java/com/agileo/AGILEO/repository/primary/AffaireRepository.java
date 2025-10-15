package com.agileo.AGILEO.repository.primary;

import com.agileo.AGILEO.entity.primary.Affaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AffaireRepository extends JpaRepository<Affaire, String> {

    Optional<Affaire> findByAffaire(String affaire);
    List<Affaire> findByAffaireIn(List<String> affaireCodes);
}