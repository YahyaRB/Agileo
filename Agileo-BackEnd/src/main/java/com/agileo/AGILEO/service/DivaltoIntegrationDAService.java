package com.agileo.AGILEO.service;

import com.agileo.AGILEO.entity.divalto.DaoEnt;
import com.agileo.AGILEO.entity.divalto.DaoLig;
import com.agileo.AGILEO.entity.divalto.MJoint;
import com.agileo.AGILEO.entity.primary.DemandeAchat;
import com.agileo.AGILEO.entity.primary.KdnFile;
import com.agileo.AGILEO.entity.primary.LigneDemandeAchat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


public interface DivaltoIntegrationDAService {

     void integrerDemandeAchatDansDivalto(Integer demandeId, String currentUsername);


    /**
     * Convertir LocalDateTime en entier YYYYMMDD
     */
     Integer convertirDateVersEntier(LocalDateTime date) ;

    /**
     * Tronquer une chaîne à la longueur maximale
     */
     String truncateString(String value, int maxLength);
     DaoEnt creerEnteteDao(DemandeAchat demande, String currentUsername, List<KdnFile> fichiers, BigDecimal jointNumber);
     DaoLig creerLigneDaoLig(LigneDemandeAchat ligne, DaoEnt daoEnt, String currentUsername, int ligneNumber);
     MJoint creerPieceJointe(KdnFile fichier, DaoEnt daoEnt, String currentUsername, BigDecimal jointNumber);
}