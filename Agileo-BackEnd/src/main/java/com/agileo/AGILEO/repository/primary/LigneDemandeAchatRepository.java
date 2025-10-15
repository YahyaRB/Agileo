package com.agileo.AGILEO.repository.primary;

import com.agileo.AGILEO.entity.primary.LigneDemandeAchat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LigneDemandeAchatRepository extends JpaRepository<LigneDemandeAchat, Integer> {
    // Rechercher toutes les lignes d’une demande d’achat
    List<LigneDemandeAchat> findByDa(Integer demandeAchatId);

    List<LigneDemandeAchat> findByDemandeAchat_ChantierIn(List<String> chantiers);
}
