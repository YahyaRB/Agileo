package com.agileo.AGILEO.service;

import com.agileo.AGILEO.entity.divalto.ENT;

import com.agileo.AGILEO.entity.divalto.MJoint;
import com.agileo.AGILEO.entity.divalto.MOUV;
import com.agileo.AGILEO.entity.primary.Reception;
import com.agileo.AGILEO.entity.primary.LigneReception;
import com.agileo.AGILEO.entity.primary.KdnFile;

import java.math.BigDecimal;
import java.util.List;

public interface DivaltoIntegrationReceptionService {

    void integrerReceptionDansDivalto(Integer receptionId, String currentUsername);

    ENT creerEnteteBL(Reception reception, String currentUsername,
                      List<KdnFile> fichiers, BigDecimal jointNumber, BigDecimal pinoCommande);

    MOUV creerLigneMouv(LigneReception ligne, ENT entBL,
                        String currentUsername, int ligneNumber);

    MJoint creerPieceJointe(KdnFile fichier, ENT entBL,
                            String currentUsername, BigDecimal jointNumber);
}