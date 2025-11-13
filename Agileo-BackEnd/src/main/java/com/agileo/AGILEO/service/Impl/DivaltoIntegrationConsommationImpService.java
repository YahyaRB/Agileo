package com.agileo.AGILEO.service.Impl;

import com.agileo.AGILEO.entity.divalto.ART;
import com.agileo.AGILEO.entity.divalto.ENT;
import com.agileo.AGILEO.entity.divalto.MOUV;
import com.agileo.AGILEO.entity.divalto.Mvtl;
import com.agileo.AGILEO.entity.primary.*;
import com.agileo.AGILEO.exception.BadRequestException;
import com.agileo.AGILEO.exception.ResourceNotFoundException;
import com.agileo.AGILEO.repository.divalto.*;
import com.agileo.AGILEO.repository.primary.*;
import com.agileo.AGILEO.service.DivaltoIntegrationConsommationService;
import com.agileo.AGILEO.service.SocPrefNoService;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DivaltoIntegrationConsommationImpService implements DivaltoIntegrationConsommationService {

    @Autowired
    private ConsommationRepository consommationRepository;

    @Autowired
    private LigneConsommationRepository ligneConsommationRepository;

    @Autowired
    private ArtRepository artRepository;

    @Autowired
    private EntRepository entRepository;

    @Autowired
    private SocnoRepository socnoRepository;

    @Autowired
    private MouvRepository mouvRepository;

    @Autowired
    private MvtlRepository mvtlRepository;

    @Autowired
    private SocPrefNoService socPrefNoService;

    @Autowired
    @Qualifier("divaltoTransactionManager")
    private PlatformTransactionManager divaltoTransactionManager;

    @PersistenceContext(unitName = "divalto")
    private EntityManager divaltoEntityManager;

    @Autowired
    private AffaireRepository affaireRepository;

    /**
     * ⚠️ MÉTHODE CORRIGÉE - VERSION AVEC GESTION D'ERREUR AMÉLIORÉE ⚠️
     * Point d'entrée principal : Enregistrer une consommation dans Divalto
     */
    @Override
    public void integrerConsommationDansDivalto(Integer consommationId, String currentUsername) {

        try {
            // 1. Récupérer et valider la consommation
            Consommation consommation = consommationRepository.findById(consommationId)
                    .orElseThrow(() -> {
                        log.error("❌ Consommation {} non trouvée", consommationId);
                        return new ResourceNotFoundException("Consommation non trouvée: " + consommationId);
                    });
            // 2. Récupérer et valider les lignes de consommation
            List<LigneConsommation> lignes = ligneConsommationRepository.findByNumCons(consommation.getIdBc());

            if (lignes == null || lignes.isEmpty()) {
                throw new BadRequestException("La consommation n'a aucune ligne");
            }

            // 3. Récupérer l'affaire pour avoir le projet
            String projet = consommation.getChantier();
            if (projet == null || projet.trim().isEmpty()) {
                throw new BadRequestException("Le chantier est obligatoire pour la consommation");
            }

            // 4. Transaction Divalto avec gestion d'erreur robuste
            TransactionTemplate transactionTemplate = new TransactionTemplate(divaltoTransactionManager);

            log.info("🔄 Démarrage de la transaction Divalto...");

            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    try {
                        // Créer l'entête de consommation (PICOD=3, OP=IS)
                        ENT entConsommation = creerEnteteConsommation(consommation, currentUsername, projet, lignes);
                        try {
                            entConsommation = entRepository.save(entConsommation);
                        } catch (Exception e) {
                            log.error("❌ ERREUR SAUVEGARDE entête Consommation - PINO: {}, Message: {}",
                                    entConsommation.getPino(), e.getMessage(), e);
                            throw new RuntimeException("Échec sauvegarde entête Consommation - PINO: "
                                    + entConsommation.getPino() + " - " + e.getMessage(), e);
                        }
                        try {
                            mettreAJourEntetesBC_BL(lignes, entConsommation.getPino(), entConsommation);
                        } catch (Exception e) {
                            log.error("❌ Erreur mise à jour entêtes BC/BL: {}", e.getMessage(), e);
                            if (!e.getMessage().contains("Aucun BC trouvé")) {
                                throw new RuntimeException("Échec mise à jour BC/BL: " + e.getMessage(), e);
                            }
                        }

                        try {
                            consommationRepository.save(consommation);
                        } catch (Exception e) {
                            log.error("❌ Erreur sauvegarde consommation: {}", e.getMessage(), e);
                            throw new RuntimeException("Échec sauvegarde consommation: " + e.getMessage(), e);
                        }

                        // 🆕 CORRECTION: Gestion de List<Mvtl>
                        for (LigneConsommation ligne : lignes) {
                            try {
                                MOUV mouv = creerLigneMouv(ligne, entConsommation, currentUsername);
                                mouv = mouvRepository.save(mouv);

                                // ✅ NOUVELLE APPROCHE : Créer ET sauvegarder directement
                                List<Mvtl> mvtls = creerLigneMvtl(ligne, mouv, entConsommation, currentUsername);

                                // 🆕 PLUS BESOIN DE SAUVEGARDER ICI - C'est fait dans creerLigneMvtl
                                log.info("🎯 Article {} traité - {} lignes consommation créées",
                                        ligne.getRef(), mvtls.size());

                            } catch (Exception e) {
                                log.error("❌ Erreur création ligne pour REF {}: {}", ligne.getRef(), e.getMessage(), e);
                                throw new RuntimeException("Échec création ligne MOUV/MVTL pour "
                                        + ligne.getRef() + ": " + e.getMessage(), e);
                            }
                        }

                    } catch (Exception e) {
                        log.error("❌ ERREUR dans la transaction Divalto: {}", e.getMessage(), e);
                        status.setRollbackOnly();
                        throw new RuntimeException("Échec de l'intégration Divalto: " + e.getMessage(), e);
                    }
                }
            });

        } catch (ResourceNotFoundException | BadRequestException e) {
            log.error("❌ Erreur de validation: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ ERREUR CRITIQUE lors de l'intégration: {}", e.getMessage(), e);
            throw new RuntimeException("Échec de l'intégration de la consommation dans Divalto: " + e.getMessage(), e);
        }
    }

    /**
     * Créer l'entête ENT pour une consommation
     * PICOD=3, TICOD=I, OP=IS, STATUS=2
     */
    private ENT creerEnteteConsommation(Consommation consommation, String username, String projet, List<LigneConsommation> lignes) {
        ENT ent = new ENT();
        ent.setCe1("A");
        ent.setDos("1");
        ent.setTicod("I"); // Type = Interne (sortie)
        ent.setPicod(BigDecimal.valueOf(3)); // Code pièce = 3
        ent.setTiers("I0000000"); // Tiers interne

        // Générer le PINO
        BigDecimal pino = socPrefNoService.getNextPinoForCONS();

        ent.setPino(pino);
        ent.setPrefpino("");

        // Date
        ent.setPidt(LocalDate.from(consommation.getDateC()));

        // Établissement et statut
        ent.setEtb("");
        ent.setStatus(BigDecimal.valueOf(2)); // Statut = 2 (validé)

        // Devise et opération
        ent.setDev("MAD");
        ent.setOp("IS"); // Opération = IS (sortie de stock)

        // Utilisateur
        ent.setUsercr(username != null && username.length() <= 20 ? username.toUpperCase() : "ROOT");
        ent.setUsermo(username != null && username.length() <= 20 ? username.toUpperCase() : "ROOT");

        // Dates de création/modification
        LocalDateTime now = LocalDateTime.now();
        ent.setUsercrdh(now);
        ent.setUsermodh(now);

        // Projet (affaire)
        ent.setProjet(projet);

        // Extraire le dépôt du code affaire
        String depot = extraireDepotDuCodeAffaire(projet);
        ent.setDepo(depot);

        // Montants (seront calculés à partir des lignes)
        ent.setHtmt(BigDecimal.valueOf(58963));/*/*/
        ent.setTtcmt(BigDecimal.valueOf(77896));/*/*/
        ent.setHtpdtmt(BigDecimal.valueOf(8561));/*/*/

        // Nombre de références
        double sommeQte = lignes.stream()
                .mapToDouble(LigneConsommation::getQte)
                .sum();

        ent.setRefnb(BigDecimal.valueOf(896));/*/*/
        ent.setOrigine(BigDecimal.ZERO);

        // ========== CHAMPS CE (CE2-CE9, CEA-CEF) ==========
        ent.setCe2("");
        ent.setCe3("");
        ent.setCe4("1");
        ent.setCe5("");
        ent.setCe6("");
        ent.setCe7("");
        ent.setCe8("");
        ent.setCe9("");
        ent.setCea("");
        ent.setCeb("");
        ent.setCec("");
        ent.setCed("");
        ent.setCee("");
        ent.setCef("");

        // ========== CHAMPS STRING - REPRÉSENTANTS ==========
        ent.setRepr0001("");
        ent.setRepr0002("");
        ent.setRepr0003("");

        // ========== CHAMPS STRING - CODES DIVERS ==========
        ent.setRibcod("");
        ent.setMarche("");

        // ========== ADRESSES TIERS ==========
        ent.setAdrtiers0001("");
        ent.setAdrtiers0002("");
        ent.setAdrtiers0003("");
        ent.setAdrtiers0004("");
        ent.setAdrtiers0005("");

        ent.setAdrcod0001("");
        ent.setAdrcod0002("");
        ent.setAdrcod0003("");
        ent.setAdrcod0004("");
        ent.setAdrcod0005("");

        // ========== CHAMPS STRING - GESTION COMMERCIALE ==========
        ent.setBlmod("");
        ent.setRegl("");
        ent.setTour("");
        ent.setPiref("");
        ent.setPinotiers("");
        ent.setTierspayer("");
        ent.setTiersgrp("");
        ent.setTiersrlv("");
        ent.setBapsalcod("");
        ent.setSalcod("");
        ent.setPrefrlvno("");

        // ========== CHAMPS STRING - FAMILLES ET TAXES ==========
        ent.setTafam("");
        ent.setTafamx("");
        ent.setRefam("");
        ent.setRefamx("");
        ent.setTacod("");
        ent.setRemcod("");
        ent.setCofam("");
        ent.setCofamv0001("");
        ent.setCofamv0002("");
        ent.setCofamv0003("");

        // ========== CHAMPS STRING - AXES ANALYTIQUES ==========
        ent.setAxe0001("");
        ent.setAxe0002("");
        ent.setAxe0003("");
        ent.setAxe0004("");

        // ========== CHAMPS STRING - TEXTES ET ÉDITION ==========
        ent.setEtano("");
        ent.setTxtedcodd("");
        ent.setTxtedcodf("");
        ent.setContact("");
        ent.setPrefblasno("");
        ent.setBlasdepo("");
        ent.setTpft("");
        ent.setAvenant("");
        ent.setPromotacod("");
        ent.setPromoremcod("");
        ent.setPrefcdnopere("");
        ent.setPrefpina("");
        ent.setBqcpce("AWB");
        ent.setTvatie("");
        ent.setStlgtgamcod("");
        ent.setLieuinct("");
        ent.setTransicod("");
        ent.setTvablcd3("");
        ent.setSitecod("");
        ent.setUpDemandeur("");

        // ========== CHAMPS STRING - AUTRES ==========
        ent.setCatpicod("");
        ent.setCondexp("");
        ent.setEtablno("");
        ent.setFraisappcod("");
        ent.setModeexp("");
        ent.setMotif("");
        ent.setPrefsitno("");
        ent.setUntyp("");
        ent.setVersiondevisoriprefpino("");
        ent.setPreffano("");
        ent.setTiersfact("");
        ent.setBtfullpino("0");
        ent.setBtprefpino("");

        // ========== CHAMPS BIGDECIMAL - MONTANTS ==========
        ent.setAcmt(BigDecimal.ZERO);
        ent.setSoacmt(BigDecimal.ZERO);
        ent.setRemmt(BigDecimal.ZERO);
        ent.setRem1(BigDecimal.ZERO);
        ent.setFouhtmt(BigDecimal.ZERO);
        ent.setFouescmt(BigDecimal.ZERO);
        ent.setFoutvamt(BigDecimal.ZERO);
        ent.setEscp(BigDecimal.ZERO);
        ent.setPortheomt(BigDecimal.ZERO);
        ent.setRempietot(BigDecimal.ZERO);
        ent.setDeeemt(BigDecimal.ZERO);
        ent.setFoudeeemt(BigDecimal.ZERO);

        // ========== CHAMPS BIGDECIMAL - MONTANTS PIEDS ==========
        ent.setPiedmt0001(BigDecimal.ZERO);
        ent.setPiedmt0002(BigDecimal.ZERO);
        ent.setPiedmt0003(BigDecimal.ZERO);

        // ========== CHAMPS BIGDECIMAL - QUANTITÉS ==========
        ent.setPoitot(BigDecimal.ZERO);
        ent.setVoltot(BigDecimal.ZERO);
        ent.setPointot(BigDecimal.ZERO);
        ent.setUnlogtot(BigDecimal.ZERO);
        ent.setPorfrval(BigDecimal.ZERO);

        // ========== CHAMPS BIGDECIMAL - REMISES ==========
        ent.setRem0001(BigDecimal.ZERO);
        ent.setRem0002(BigDecimal.ZERO);
        ent.setRem0003(BigDecimal.ZERO);

        // ========== CHAMPS BIGDECIMAL - CODES ET NUMÉROS ==========
        ent.setRlvno(BigDecimal.ZERO);
        ent.setBlasno(BigDecimal.ZERO);
        ent.setCesintcod(BigDecimal.ZERO);
        ent.setCdnopere(BigDecimal.ZERO);
        ent.setTpvbl(BigDecimal.ZERO);
        ent.setDeeeinccod(BigDecimal.ZERO);
        ent.setPina(BigDecimal.ZERO);
        ent.setCenote(BigDecimal.ONE);
        ent.setNote(BigDecimal.ZERO);
        ent.setTxtcodd(BigDecimal.ONE);
        ent.setTxtcodf(BigDecimal.ONE);
        ent.setTxtnoted(BigDecimal.ZERO);
        ent.setTxtnotef(BigDecimal.ZERO);
        ent.setDevp(BigDecimal.ZERO);
        ent.setPiedno0001(BigDecimal.ZERO);
        ent.setPiedno0002(BigDecimal.ZERO);
        ent.setPiedno0003(BigDecimal.ZERO);
        ent.setNbex(BigDecimal.ZERO);
        ent.setPirelcod(BigDecimal.ZERO);
        ent.setRelcod(BigDecimal.ONE);
        ent.setEditcod(BigDecimal.valueOf(2));
        ent.setTrcod(BigDecimal.ZERO);
        ent.setBoredicod(BigDecimal.ZERO);
        ent.setAscod(BigDecimal.ONE);
        ent.setEchvcod(BigDecimal.ONE);
        ent.setEncasscod(BigDecimal.ZERO);

        // ========== CHAMPS BIGDECIMAL - TYPES D'ADRESSE ==========
        ent.setAdrtyp0001(BigDecimal.ZERO);
        ent.setAdrtyp0002(BigDecimal.ZERO);
        ent.setAdrtyp0003(BigDecimal.ZERO);
        ent.setAdrtyp0004(BigDecimal.ZERO);
        ent.setAdrtyp0005(BigDecimal.ZERO);

        // ========== CHAMPS BIGDECIMAL - CODES DIVERS ==========
        ent.setPriocod(BigDecimal.ZERO);
        ent.setHtcod(BigDecimal.ONE);
        ent.setStres(BigDecimal.valueOf(1));
        ent.setFamod(BigDecimal.ZERO);
        ent.setPeriod(BigDecimal.ZERO);
        ent.setPorcod(BigDecimal.ZERO);
        ent.setPoicod(BigDecimal.ZERO);
        ent.setVolcod(BigDecimal.ZERO);
        ent.setPorfrfl(BigDecimal.ZERO);
        ent.setColinb(BigDecimal.ZERO);
        ent.setTourrg(BigDecimal.ZERO);

        // ========== CHAMPS BIGDECIMAL - TYPES DE REMISE ==========
        ent.setRemtyp1(BigDecimal.ZERO);
        ent.setRemtyp0001(BigDecimal.ZERO);
        ent.setRemtyp0002(BigDecimal.ZERO);
        ent.setRemtyp0003(BigDecimal.ZERO);

        // ========== CHAMPS BIGDECIMAL - COMPOSANTS ==========
        ent.setComp0001(BigDecimal.ZERO);
        ent.setComp0002(BigDecimal.ZERO);
        ent.setComp0003(BigDecimal.ZERO);

        // ========== CHAMPS BIGDECIMAL - JOURNAUX ET TRANSACTIONS ==========
        ent.setTransjrnb(BigDecimal.ZERO);
        ent.setOfascod(BigDecimal.ZERO);
        ent.setFinalField(BigDecimal.valueOf(2));
        ent.setQuacod(BigDecimal.ZERO);
        ent.setCejoint(BigDecimal.valueOf(2));
        ent.setJoint(BigDecimal.valueOf(0));
        ent.setPrgcdeflg(BigDecimal.ONE);
        ent.setPoincod(BigDecimal.ZERO);
        ent.setPrioreg(BigDecimal.ZERO);

        // ========== CHAMPS BIGDECIMAL - FLAGS (0 ou 1) ==========
        ent.setDtflg(BigDecimal.ONE);
        ent.setSynchrofl(BigDecimal.ONE);
        ent.setIcpfl(BigDecimal.ONE);
        ent.setPorfrcod(BigDecimal.ZERO);
        ent.setCeatraitefl(BigDecimal.ZERO);
        ent.setBexno(BigDecimal.ZERO);
        ent.setBlqfl(BigDecimal.ONE);
        ent.setConfirmationfl(BigDecimal.ZERO);
        ent.setTaxcplffl(BigDecimal.ONE);
        ent.setTaxsfvfl(BigDecimal.ONE);
        ent.setTvaautoliqfl(BigDecimal.ONE);
        ent.setUnlogcod(BigDecimal.ZERO);
        ent.setVersiondevisno(BigDecimal.ZERO);
        ent.setVersiondevisoripino(BigDecimal.ZERO);
        ent.setBprelcod(BigDecimal.ZERO);
        ent.setCircuitvalidationblfl(BigDecimal.ZERO);
        ent.setCircuitvalidationfctfl(BigDecimal.ZERO);
        ent.setGouvfacblqfl(BigDecimal.ONE);
        ent.setIndiceno(BigDecimal.ZERO);
        ent.setRemseuilfl(BigDecimal.ONE);
        ent.setSitno(BigDecimal.ZERO);
        ent.setTransitfl(BigDecimal.ONE);
        ent.setAcomptetyp(BigDecimal.ZERO);
        ent.setBidon(BigDecimal.ZERO);
        ent.setBpjrnb(BigDecimal.ZERO);
        ent.setPaiementtyp(BigDecimal.ZERO);
        ent.setResjrnb(BigDecimal.ZERO);
        ent.setStnfl(BigDecimal.ZERO);
        ent.setFano(BigDecimal.ZERO);
        ent.setReglimmfl(BigDecimal.ZERO);
        ent.setBtpino(BigDecimal.ZERO);
        ent.setBtretourfl(BigDecimal.ZERO);
        ent.setBtstatus(BigDecimal.ZERO);

        // ========== CHAMPS DATE (LocalDate) ==========
        ent.setRlvdt(null);
        ent.setDeldemdt(null);
        ent.setDelaccdt(null);
        ent.setDelrepdt(null);
        ent.setEchdt(null);
        ent.setUpDaterecuperation(null);
        ent.setValfindt(null);
        ent.setPiecedt(null);

        log.info("✅ Entête consommation créé - PINO: {}, Projet: {}, Dépôt: {}", pino, projet, depot);

        return ent;
    }

    /**
     * Créer une ligne MOUV pour la consommation
     */
    private MOUV creerLigneMouv(LigneConsommation ligne, ENT entConsommation, String username) {
        MOUV mouv = new MOUV();

        // ========== CHAMPS DE BASE ==========
        mouv.setCe1("C");
        mouv.setCe2("1");
        mouv.setCe3("1");
        mouv.setDos("1");

        // ========== RÉFÉRENCE ARTICLE ==========
        mouv.setRef(ligne.getRef());
        mouv.setSref1(ligne.getSref1() != null ? ligne.getSref1() : "");
        mouv.setSref2(ligne.getSref2() != null ? ligne.getSref2() : "");

        // ========== LIEN AVEC L'ENTÊTE ==========
        mouv.setTicod("I");
        mouv.setPicod(BigDecimal.valueOf(3));
        mouv.setTiers("I0000000");

        // ========== NUMÉRO D'ENREGISTREMENT ==========
        mouv.setEnrno(socnoRepository.findByNumEnrgForUpdate().add(BigDecimal.ONE));
        socnoRepository.incrementNumEnrg();
        // ========== QUANTITÉS ==========  /*/*
        mouv.setQte1(BigDecimal.ZERO);
        mouv.setQte2(BigDecimal.ZERO);
        mouv.setQte3(BigDecimal.ZERO);
        mouv.setFaqte(BigDecimal.valueOf(0.000));
        mouv.setRefqte(BigDecimal.valueOf(ligne.getQte()));
        mouv.setEmbqte(BigDecimal.valueOf(0.000));

        // ========== OPÉRATION ET UTILISATEUR ==========
        mouv.setOp("IS");
        mouv.setUsercr(username != null && username.length() <= 20 ? username.toUpperCase() : "ROOT");
        mouv.setUsermo("");

        // ========== DATES ==========
        LocalDateTime now = LocalDateTime.now();
        mouv.setUsercrdh(now);
        mouv.setUsermodh(null);

        // ========== DÉPÔT ET ÉTABLISSEMENT ==========
        mouv.setDepo(entConsommation.getDepo());
        mouv.setEtb(entConsommation.getEtb() != null ? entConsommation.getEtb() : "");

        // ========== PROJET ==========
        mouv.setProjet(entConsommation.getProjet());

        // ========== DESCRIPTION ==========
        mouv.setDes(ligne.getDes() != null ? ligne.getDes() : "");

        // ========== UNITÉS ==========
        mouv.setVenun(ligne.getUnite() != null ? ligne.getUnite() : "PCS");
        mouv.setRefun(ligne.getUnite() != null ? ligne.getUnite() : "PCS");
        mouv.setPubun("");
        mouv.setEmbun("");

        // ========== MONTANTS ==========
        mouv.setPub(BigDecimal.ZERO);
        mouv.setPpar(BigDecimal.ZERO);
        mouv.setMont(BigDecimal.ZERO);

        // ========== STATUT ET SENS ==========
        mouv.setStatus(BigDecimal.valueOf(2));
        mouv.setStres(BigDecimal.valueOf(1));

        // ========== DEVISE ==========
        mouv.setDev("MAD");

        // ========== POSITION ET SENS ==========
        mouv.setPosition("");
        mouv.setSens(BigDecimal.valueOf(2)); // Sens = 2 (sortie)
        mouv.setCenote(BigDecimal.valueOf(1));

        // ========== CHAMPS CE ADDITIONNELS ==========
        mouv.setCe4("");
        mouv.setCe5("");
        mouv.setCe6(" ");
        mouv.setCe7("1");
        mouv.setCe8("");
        mouv.setCe9("1");
        mouv.setCea("");
        mouv.setCeb("");
        mouv.setCec("");
        mouv.setCed("");
        mouv.setCee("");
        mouv.setCef("");

        // ========== CHAMPS DEVIS ==========
        mouv.setPrefdvno("");
        mouv.setDvno(BigDecimal.ZERO);
        mouv.setDvdt(null);
        mouv.setDvlg(BigDecimal.ZERO);
        mouv.setDvslg(BigDecimal.ZERO);
        mouv.setDvce4("");
        mouv.setDvenrno(BigDecimal.ZERO);
        mouv.setDvqte(BigDecimal.ZERO);

        // ========== CHAMPS BON DE LIVRAISON ==========
        mouv.setPrefblno("");
        mouv.setBlno(BigDecimal.ZERO);
        mouv.setBlno(entConsommation.getPino());
        mouv.setBldt(entConsommation.getPidt());
        mouv.setBlce4("1");
        mouv.setBlenrno(mouv.getEnrno());
        mouv.setBllg(BigDecimal.ONE);
        mouv.setBlslg(BigDecimal.ZERO);
        mouv.setBlqte(BigDecimal.valueOf(ligne.getQte()));

        // ========== CHAMPS COMMANDE ==========
        mouv.setPrefcdno("");
        mouv.setCdno(BigDecimal.ZERO);
        mouv.setCddt(null);
        mouv.setCdlg(BigDecimal.ZERO);
        mouv.setCdslg(BigDecimal.ZERO);
        mouv.setCdce4("");
        mouv.setCdenrno(BigDecimal.ZERO);
        mouv.setCdqte(BigDecimal.ZERO);
        mouv.setCdnopere(BigDecimal.ZERO);
        mouv.setPrefcdnopere("");

        // ========== CHAMPS FACTURE ==========
        mouv.setPreffano("");
        mouv.setFano(BigDecimal.ZERO);
        mouv.setFadt(entConsommation.getPidt());
        mouv.setFace4("");
        mouv.setFalg(BigDecimal.ZERO);
        mouv.setFaslg(BigDecimal.ZERO);

        // ========== CHAMPS ARTICLE ==========
        mouv.setArtind("");
        mouv.setConfurateurartind("");

        // ========== CHAMPS MARCHÉ ==========
        mouv.setMarche("");

        // ========== CHAMPS REMISE ==========
        mouv.setRemmt(BigDecimal.ZERO);
        mouv.setRem0001(BigDecimal.ZERO);
        mouv.setRem0002(BigDecimal.ZERO);
        mouv.setRem0003(BigDecimal.ZERO);
        mouv.setRemcod("");
        mouv.setRemcodcad("");
        mouv.setRemtyp0001(BigDecimal.ZERO);
        mouv.setRemtyp0002(BigDecimal.ZERO);
        mouv.setRemtyp0003(BigDecimal.ZERO);

        // ========== CHAMPS REMISE PIED ==========
        mouv.setRempiemt0001(BigDecimal.ZERO);
        mouv.setRempiemt0002(BigDecimal.ZERO);
        mouv.setRempiemt0003(BigDecimal.ZERO);
        mouv.setRempiemt0004(BigDecimal.ZERO);
        mouv.setRempiepart0001(BigDecimal.ZERO);
        mouv.setRempiepart0002(BigDecimal.ZERO);
        mouv.setRempiepart0003(BigDecimal.ZERO);
        mouv.setRempiepart0004(BigDecimal.ZERO);

        // ========== CHAMPS COMPTABILITÉ ==========
        mouv.setCoecod("");
        mouv.setCofamr("");
        mouv.setCofamv0001("");
        mouv.setCofamv0002("");
        mouv.setCofamv0003("");
        mouv.setCommt0001(BigDecimal.ZERO);
        mouv.setCommt0002(BigDecimal.ZERO);
        mouv.setCommt0003(BigDecimal.ZERO);
        mouv.setComp0001(BigDecimal.ZERO);
        mouv.setComp0002(BigDecimal.ZERO);
        mouv.setComp0003(BigDecimal.ZERO);
        mouv.setCptv("");

        // ========== CHAMPS AXES ANALYTIQUES ==========
        mouv.setAxe0001("");
        mouv.setAxe0002(entConsommation.getProjet());
        mouv.setAxe0003("");
        mouv.setAxe0004("");

        // ========== CHAMPS AFRIQUE INDICE ==========
        mouv.setAfrindice("");

        // ========== CHAMPS AVENANT ==========
        mouv.setAvenant("");

        // ========== CHAMPS BESOIN ==========
        mouv.setBesoinno(BigDecimal.ZERO);

        // ========== CHAMPS BON DE LIVRAISON ASSURÉ ==========
        mouv.setBlasenrno(BigDecimal.ZERO);

        // ========== CHAMPS BON DE PRÉPARATION ==========
        mouv.setBpdt(null);
        mouv.setBpligcompfl(BigDecimal.ZERO);
        mouv.setBpno(BigDecimal.ZERO);

        // ========== CHAMPS CADEAU ==========
        mouv.setCadeaufl(BigDecimal.ZERO);

        // ========== CHAMPS CONFIGURATEUR ==========
        mouv.setConfigurateurlino(BigDecimal.ZERO);
        mouv.setConfigurateurmonostatus(BigDecimal.ZERO);
        mouv.setConfigurateurmultistatus(BigDecimal.ZERO);
        mouv.setConfigurateurref("");
        mouv.setConfigurateursref1("");
        mouv.setConfigurateursref2("");

        // ========== CHAMPS CONTRAT ==========
        mouv.setContratcod("");

        // ========== CHAMPS CTM ==========
        mouv.setCtmfl(BigDecimal.ONE);

        // ========== CHAMPS DÉCLARATION ==========
        mouv.setDeccod(BigDecimal.ONE);

        // ========== CHAMPS DÉPÔT ORIGINE ==========
        mouv.setDepoorig("");

        // ========== CHAMPS DTR ==========
        mouv.setDtrenrno(BigDecimal.ZERO);
        mouv.setDtrgrp(BigDecimal.ZERO);
        mouv.setDtrtype(BigDecimal.ZERO);

        // ========== CHAMPS ÉDITION ==========
        mouv.setEdcod("");

        // ========== CHAMPS ÉLÉMENT ==========
        mouv.setElemno(BigDecimal.ZERO);

        // ========== CHAMPS ENRNO COMPOSANTS ==========
        mouv.setEnrnoc0001(BigDecimal.ZERO);
        mouv.setEnrnoc0002(BigDecimal.ZERO);
        mouv.setEnrnoc0003(BigDecimal.ZERO);
        mouv.setEnrnoc0004(BigDecimal.ZERO);
        mouv.setEnrnocad(BigDecimal.ZERO);

        // ========== CHAMPS ENRNO PARENTS ==========
        mouv.setEnrnop0001(BigDecimal.ZERO);
        mouv.setEnrnop0002(BigDecimal.ZERO);
        mouv.setEnrnop0003(BigDecimal.ZERO);
        mouv.setEnrnop0004(BigDecimal.ZERO);

        // ========== CHAMPS FILLER ==========
        mouv.setFillersens(BigDecimal.ZERO);

        // ========== CHAMPS FOURNISSEUR GIM ==========
        mouv.setFoufadtgim(null);
        mouv.setFoufanogim("");
        mouv.setFoufaqtegim(BigDecimal.ZERO);

        // ========== CHAMPS FRAIS ==========
        mouv.setFraisappcod("");
        mouv.setFraisfl(BigDecimal.ZERO);
        mouv.setFraisimpactflg(BigDecimal.ZERO);
        mouv.setFraismt(BigDecimal.ZERO);
        mouv.setFraismtgim(BigDecimal.ZERO);
        mouv.setFraisvalidtyp(BigDecimal.ZERO);

        // ========== CHAMPS GARANTIE ==========
        mouv.setGadt(null);

        // ========== CHAMPS GAMME ==========
        mouv.setGamseq("");

        // ========== CHAMPS GIM ==========
        mouv.setGimcod("");
        mouv.setFamontgim(BigDecimal.ZERO);
        mouv.setFapubgim(BigDecimal.ZERO);

        // ========== CHAMPS GPA ==========
        mouv.setGpafl(BigDecimal.ZERO);

        // ========== CHAMPS GRATUIT ==========
        mouv.setGratuitfl(BigDecimal.ZERO);

        // ========== CHAMPS IAG ==========
        mouv.setIagcdenrno(BigDecimal.ZERO);
        mouv.setIagfluxlien("");
        mouv.setIaglientyp(BigDecimal.ZERO);

        // ========== CHAMPS ICP ==========
        mouv.setIcpfl(BigDecimal.ONE);

        // ========== CHAMPS LIGNE ==========
        mouv.setLigne(BigDecimal.ZERO);

        // ========== CHAMPS LIVRAISON DIRECTE ==========
        mouv.setLivdirectfl(BigDecimal.valueOf(2));

        // ========== CHAMPS MOTIF ==========
        mouv.setMotif("");
        mouv.setMotifsolde("");

        // ========== CHAMPS MOUVEMENT ==========
        mouv.setMvcod(BigDecimal.valueOf(2));
        mouv.setMvstat(BigDecimal.ONE);

        // ========== CHAMPS NOTE ==========
        mouv.setNote(BigDecimal.ZERO);

        // ========== CHAMPS ORDRE DE FABRICATION ==========
        mouv.setOfno(BigDecimal.ZERO);
        mouv.setPrefofno("");

        // ========== CHAMPS OPTION ==========
        mouv.setOptionfl(BigDecimal.ONE);
        mouv.setOptionvalidefl(BigDecimal.ONE);

        // ========== CHAMPS PRIX FORFAITAIRE ==========
        mouv.setPaforf(BigDecimal.ONE);

        // ========== CHAMPS PAGE ==========
        mouv.setPagcod("");

        // ========== CHAMPS PANACHE ==========
        mouv.setPanachefl(BigDecimal.ONE);

        // ========== CHAMPS CODES P ==========
        mouv.setPcod0001(BigDecimal.valueOf(4));
        mouv.setPcod0002(BigDecimal.valueOf(4));
        mouv.setPcod0003(BigDecimal.valueOf(2));
        mouv.setPcod0004(BigDecimal.valueOf(2));
        mouv.setPcod0005(BigDecimal.valueOf(2));
        mouv.setPcod0006(BigDecimal.valueOf(4));

        // ========== CHAMPS PÉRIODE ==========
        mouv.setPeriodeddt(null);
        mouv.setPeriodefdt(null);

        // ========== CHAMPS PFC ==========
        mouv.setPfcno(BigDecimal.ZERO);

        // ========== CHAMPS PROGRAMME ==========
        mouv.setPrgqte(BigDecimal.valueOf(ligne.getQte()));
        mouv.setPrgrefqte(BigDecimal.valueOf(ligne.getQte()));

        // ========== CHAMPS PRIORITÉ ==========
        mouv.setPriocod(BigDecimal.ZERO);

        // ========== CHAMPS PRIX SPÉCIAL ==========
        mouv.setPrixspecialfl(BigDecimal.ZERO);

        // ========== CHAMPS PROMOTION ==========
        mouv.setPromoremcod("");
        mouv.setPromotacod("");
        mouv.setPromotyp(BigDecimal.ONE);

        // ========== CHAMPS PRIX PUBLIC ==========  d
        mouv.setPubtyp(BigDecimal.ONE);
        mouv.setPunetori(BigDecimal.ZERO);
        mouv.setPustat(BigDecimal.ZERO);

        // ========== CHAMPS PV ==========
        mouv.setPvcod(BigDecimal.ONE);

        // ========== CHAMPS TYPE DE QUANTITÉ ==========
        mouv.setQtetyp(BigDecimal.ONE);

        // ========== CHAMPS REBU ==========
        mouv.setRebucod("");

        // ========== CHAMPS RÉCEPTION ==========
        mouv.setRecptno(BigDecimal.ZERO);
        mouv.setRefamr("");
        mouv.setRefamrx("");
        mouv.setReffo("");
        mouv.setReglecod("");
        mouv.setRelcod0001(BigDecimal.valueOf(2));
        mouv.setRelcod0002(BigDecimal.valueOf(2));
        mouv.setRelcod0003(BigDecimal.valueOf(2));

        mouv.setRepr0001("");
        mouv.setRepr0002("");
        mouv.setRepr0003("");
        mouv.setRgpenrno(BigDecimal.ZERO);

        mouv.setSolderelfl(BigDecimal.ZERO);

        // ========== CHAMPS SYNCHRO ==========
        mouv.setSynchrofl(BigDecimal.ONE);

        // ========== CHAMPS TAXE ==========
        mouv.setTacod("");
        mouv.setTafamr("");
        mouv.setTafamrx("");
        mouv.setTvaart("1");
        mouv.setTvanassujettiefl(BigDecimal.ONE);

        // ========== CHAMPS TICKET ==========
        mouv.setTicket(BigDecimal.ZERO);

        // ========== CHAMPS TIERS ==========
        mouv.setTiersexterne("");
        mouv.setTiersfou2("");

        // ========== CHAMPS TEXTE ==========
        mouv.setTxtcod(BigDecimal.ONE);
        mouv.setTxtedcod("");
        mouv.setTxtnote(BigDecimal.ZERO);

        // ========== CHAMPS TYPE UNITÉ ==========
        mouv.setUntyp("");

        // ========== CHAMPS MONTANTS DIVERS ==========
        BigDecimal crtotmt = mouvRepository.getCrtotmtByDepoAndRef(mouv.getDepo(), mouv.getRef());
        System.out.println("crtotmt : "+crtotmt);
        System.out.println("ligne.getQte() : "+ligne.getQte());
        mouv.setCrtotmt(crtotmt.multiply(BigDecimal.valueOf(ligne.getQte())));
        mouv.setCmptotmt(crtotmt.multiply(BigDecimal.valueOf(ligne.getQte())));
        mouv.setAppremmt(BigDecimal.ZERO);
        mouv.setAppremmtun(BigDecimal.ZERO);
        mouv.setPatotmt(BigDecimal.ZERO);

        return mouv;
    }

    private List<Mvtl> creerLigneMvtl(LigneConsommation ligne, MOUV mouv, ENT entConsommation, String username) {

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("🔵 DÉBUT creerLigneMvtl - CORRECTION FIFO");
        log.info("📌 Article: {}, Quantité totale: {}, Dépôt: {}",
                ligne.getRef(), ligne.getQte(), entConsommation.getDepo());

        // 1. Récupérer les entrées en stock AVEC TRI FIFO
        List<Mvtl> listeMVTLBrute = mvtlRepository.listeArticlesaConsommer(
                entConsommation.getDepo(),
                ligne.getRef()
        );

        if (listeMVTLBrute == null || listeMVTLBrute.isEmpty()) {
            log.error("❌ Aucune entrée en stock pour l'article {}", ligne.getRef());
            throw new RuntimeException("Aucune entrée en stock pour l'article " + ligne.getRef());
        }

        // 2. Filtrer ET TRIER pour FIFO strict
        List<Mvtl> listeMVTL = listeMVTLBrute.stream()
                .filter(mvtl -> mvtl.getSens() != null && mvtl.getSens().compareTo(BigDecimal.ONE) == 0)
                .filter(mvtl -> mvtl.getStqte() != null && mvtl.getStqte().compareTo(BigDecimal.ZERO) > 0)
                .sorted((m1, m2) -> m1.getVtlno().compareTo(m2.getVtlno())) // 🆕 TRI FIFO EXPLICITE
                .collect(Collectors.toList());

        log.info("📦 Entrées en stock disponibles: {}", listeMVTL.size());

        // 🆕 DEBUG : Afficher les réceptions disponibles
        for (Mvtl mvtl : listeMVTL) {
            log.info("  📋 Réception VTLNO: {}, STQTE: {}", mvtl.getVtlno(), mvtl.getStqte());
        }

        if (listeMVTL.isEmpty()) {
            throw new RuntimeException("Stock insuffisant pour l'article " + ligne.getRef());
        }

        // 3. Liste pour stocker TOUTES les lignes de consommation créées
        List<Mvtl> lignesConsommation = new ArrayList<>();

        // 4. Traitement FIFO - UNE LIGNE PAR RÉCEPTION
        BigDecimal qteRestante = BigDecimal.valueOf(ligne.getQte());

        log.info("🔄 DÉBUT traitement FIFO - Quantité à répartir: {}", qteRestante);

        int numeroLigne = 0;

        for (Mvtl mvtlStock : listeMVTL) {
            if (qteRestante.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("✅ Quantité totale consommée - Arrêt de la boucle");
                break;
            }

            numeroLigne++;
            BigDecimal stqteActuel = mvtlStock.getStqte();
            BigDecimal qteConsommeeReception;
            BigDecimal nouveauStock;

            log.info("🔄 Ligne {} - Réception VTLNO: {}, Stock actuel: {}, Reste à consommer: {}",
                    numeroLigne, mvtlStock.getVtlno(), stqteActuel, qteRestante);

            // Calculer la quantité à consommer sur cette réception spécifique
            if (qteRestante.compareTo(stqteActuel) >= 0) {
                // 🔥 CONSOMMATION TOTALE de cette réception
                qteConsommeeReception = stqteActuel;
                qteRestante = qteRestante.subtract(stqteActuel);
                nouveauStock = BigDecimal.ZERO;
                log.info("  ➤ Consommation TOTALE - Qté consommée: {}, Reste: {}",
                        qteConsommeeReception, qteRestante);
            } else {
                // 🔥 CONSOMMATION PARTIELLE de cette réception
                qteConsommeeReception = qteRestante;
                nouveauStock = stqteActuel.subtract(qteRestante);
                qteRestante = BigDecimal.ZERO;
                log.info("  ➤ Consommation PARTIELLE - Qté consommée: {}, Nouveau stock: {}",
                        qteConsommeeReception, nouveauStock);
            }

            // 🆕 IMPORTANTE : Mise à jour du stock EN PREMIER
            log.info("  🔄 Mise à jour stock réception VTLNO: {} → {}", mvtlStock.getVtlno(), nouveauStock);
            mvtlRepository.updateStockOnly(mvtlStock.getVtlno(), nouveauStock);

            // 🆕 CRÉER la ligne de consommation pour CETTE réception spécifique
            log.info("  🔨 Création ligne consommation {} pour réception VTLNO: {}, QTE: {}",
                    numeroLigne, mvtlStock.getVtlno(), qteConsommeeReception);

            Mvtl mvtlSortie = creerLigneConsommationPourReception(
                    mvtlStock, qteConsommeeReception, mouv, entConsommation, username, numeroLigne);

            // 🆕 AJOUTER à la liste (IMPORTANT !)
            lignesConsommation.add(mvtlSortie);

            log.info("  ✅ Ligne {} créée - VTLNO: {}, VTLNA: {}, QTE: {}",
                    numeroLigne, mvtlSortie.getVtlno(), mvtlSortie.getVtlna(), qteConsommeeReception);

            // 🆕 SAUVEGARDER IMMÉDIATEMENT chaque ligne pour éviter les conflits
            try {
                mvtlRepository.saveAndFlush(mvtlSortie);
                log.info("  💾 Ligne {} sauvegardée avec succès", numeroLigne);
            } catch (Exception e) {
                log.error("❌ Erreur sauvegarde ligne {} : {}", numeroLigne, e.getMessage());
                throw new RuntimeException("Erreur sauvegarde ligne consommation: " + e.getMessage());
            }
        }

        // Vérifier que toute la quantité a été traitée
        if (qteRestante.compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Stock insuffisant pour l'article " + ligne.getRef()
                    + ". Quantité manquante: " + qteRestante);
        }

        // 5. Mise à jour du stock total article
        try {
            ART article = artRepository.findByRef(ligne.getRef());
            if (article != null) {
                BigDecimal nouvelleQuantite = article.getSttotqte().subtract(BigDecimal.valueOf(ligne.getQte()));
                article.setSttotqte(nouvelleQuantite);
                artRepository.saveAndFlush(article);
                log.info("✅ Stock total article mis à jour: {}", nouvelleQuantite);
            }
        } catch (Exception e) {
            log.warn("⚠️ Erreur mise à jour stock total article: {}", e.getMessage());
        }

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("🟢 FIFO TERMINÉ - {} lignes consommation créées pour PINO: {}",
                lignesConsommation.size(), entConsommation.getPino());
        log.info("═══════════════════════════════════════════════════════════════");

        return lignesConsommation;
    }

    /**
     * Crée une ligne de consommation pour une réception spécifique
     */
    private Mvtl creerLigneConsommationPourReception(Mvtl mvtlReference, BigDecimal qteConsommee,
                                                     MOUV mouv, ENT entConsommation, String username, int numeroLigne) {

        log.info("🔨 Création ligne consommation #{} pour réception VTLNO: {}, QTE: {}",
                numeroLigne, mvtlReference.getVtlno(), qteConsommee);

        Mvtl mvtlSortie = new Mvtl();
        mvtlSortie.setMvtlId(null);

        // ⭐ COPIER TOUS LES CHAMPS DEPUIS LA RÉFÉRENCE ⭐
        mvtlSortie.setCe1(mvtlReference.getCe1());
        mvtlSortie.setCe2(mvtlReference.getCe2());
        mvtlSortie.setCe3(mvtlReference.getCe3());
        mvtlSortie.setCe4(mvtlReference.getCe4());
        mvtlSortie.setCe5(mvtlReference.getCe5() != null ? mvtlReference.getCe5() : "");
        mvtlSortie.setCe6(" ");
        mvtlSortie.setCe7(mvtlReference.getCe7() != null ? mvtlReference.getCe7() : "");
        mvtlSortie.setCe8(mvtlReference.getCe8() != null ? mvtlReference.getCe8() : "");
        mvtlSortie.setCe9(mvtlReference.getCe9() != null ? mvtlReference.getCe9() : "");
        mvtlSortie.setCea("");

        mvtlSortie.setDos(mvtlReference.getDos());
        mvtlSortie.setRef(mvtlReference.getRef());
        mvtlSortie.setSref1(mvtlReference.getSref1() != null ? mvtlReference.getSref1() : "");
        mvtlSortie.setSref2(mvtlReference.getSref2() != null ? mvtlReference.getSref2() : "");
        mvtlSortie.setEtb(mvtlReference.getEtb());
        mvtlSortie.setDepo(mvtlReference.getDepo());
        mvtlSortie.setLieu("");  // 🔥 Vider pour sortie
        mvtlSortie.setColino(mvtlReference.getColino() != null ? mvtlReference.getColino() : "");
        mvtlSortie.setSerie(mvtlReference.getSerie() != null ? mvtlReference.getSerie() : "");
        mvtlSortie.setNst(mvtlReference.getNst() != null ? mvtlReference.getNst() : "N");
        mvtlSortie.setStdtsql(mvtlReference.getStdtsql());
        mvtlSortie.setPrefpino("");  // 🔥 Vider pour sortie
        mvtlSortie.setBlaslieu("");  // 🔥 Vider pour sortie

        // 🔥 VALEURS SPÉCIFIQUES SORTIE
        mvtlSortie.setTicod("I");
        mvtlSortie.setPicod(BigDecimal.valueOf(3));
        mvtlSortie.setTiers("I0000000");
        mvtlSortie.setOp("IS");
        mvtlSortie.setUsercr(username != null && username.length() <= 20 ? username.toUpperCase() : "ROOT");
        mvtlSortie.setUsermo(username != null && username.length() <= 20 ? username.toUpperCase() : "ROOT");
        mvtlSortie.setEnrno(mouv.getEnrno());
        mvtlSortie.setLilg(BigDecimal.valueOf(numeroLigne + 1)); // 🆕 Ligne incrémentée
        mvtlSortie.setDeldt(null);

        // 🎯 TRAÇABILITÉ CRUCIALE
        BigDecimal vtlno = getNextVtlNo();
        BigDecimal vtlna = mvtlReference.getVtlno(); // ⭐ LIEN VERS RÉCEPTION SOURCE
        mvtlSortie.setVtlno(vtlno);
        mvtlSortie.setVtlna(vtlna);
        socnoRepository.incrementVtlnoEnrg();

        mvtlSortie.setSens(BigDecimal.valueOf(2)); // SORTIE
        mvtlSortie.setPino(entConsommation.getPino());
        mvtlSortie.setCdvtlno(BigDecimal.ZERO);
        mvtlSortie.setTicketres(BigDecimal.ZERO);
        mvtlSortie.setBldt(entConsommation.getPidt());
        mvtlSortie.setDeldemdt(null);
        mvtlSortie.setDelaccdt(null);
        mvtlSortie.setDelrepdt(null);
        mvtlSortie.setBlasvtlno(BigDecimal.ZERO);
        mvtlSortie.setPerempdt(mvtlReference.getPerempdt());
        mvtlSortie.setRcono(BigDecimal.ZERO);

        // 📅 DATES
        LocalDateTime now = LocalDateTime.now();
        mvtlSortie.setUsercrdh(now);
        mvtlSortie.setUsermodh(now);

        // 📊 QUANTITÉS - SEULEMENT CETTE RÉCEPTION
        mvtlSortie.setQte(qteConsommee);
        mvtlSortie.setRefqte(qteConsommee);
        mvtlSortie.setStqte(BigDecimal.ZERO);  // Stock = 0 pour sortie
        mvtlSortie.setResqte(BigDecimal.ZERO);
        mvtlSortie.setStres(BigDecimal.valueOf(1));

        // 💰 COÛTS
        BigDecimal cr = mvtlReference.getCr() != null ? mvtlReference.getCr() : BigDecimal.ZERO;
        mvtlSortie.setCr(cr);
        mvtlSortie.setCncr(BigDecimal.ZERO);
        mvtlSortie.setCmp(cr);
        mvtlSortie.setCrgam(BigDecimal.ZERO);

        // 🔧 AUTRES CHAMPS
        mvtlSortie.setStatus(BigDecimal.ZERO);
        mvtlSortie.setOfrescod(BigDecimal.ONE);
        mvtlSortie.setPrevflg(BigDecimal.ONE);
        mvtlSortie.setBpdetno(BigDecimal.ZERO);
        mvtlSortie.setTicketmress(BigDecimal.ZERO);
        mvtlSortie.setContratno(BigDecimal.ZERO);
        mvtlSortie.setMatlilg(BigDecimal.ZERO);
        mvtlSortie.setRmno(BigDecimal.ZERO);
        mvtlSortie.setActno(BigDecimal.ZERO);
        mvtlSortie.setTiersstock("");
        mvtlSortie.setManutcod("");
        mvtlSortie.setSeriefou("");
        mvtlSortie.setArtind("");

        log.info("🎯 Ligne #{} préparée - VTLNO: {}, VTLNA: {}, QTE: {}, LILG: {}",
                numeroLigne, vtlno, vtlna, qteConsommee, mvtlSortie.getLilg());

        return mvtlSortie;
    }
    /**
     * Obtenir le prochain VTLNO
     */
    private BigDecimal getNextVtlNo() {
        try {
            String sql = "SELECT MAX(VTLNO) FROM MVTL";
            Query query = divaltoEntityManager.createNativeQuery(sql);
            Object result = query.getSingleResult();

            if (result != null) {
                BigDecimal maxVtlNo = new BigDecimal(result.toString());
                return maxVtlNo.add(BigDecimal.ONE);
            } else {
                return new BigDecimal("400000");
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du dernier VTLNO: {}", e.getMessage());
            return new BigDecimal("400000");
        }
    }

    /**
     * Extraire le dépôt du code affaire (3 derniers chiffres)
     */
    private String extraireDepotDuCodeAffaire(String codeAffaire) {
        try {
            if (codeAffaire == null || codeAffaire.trim().isEmpty()) {
                return "RB4"; // Valeur par défaut
            }

            String base = codeAffaire.startsWith("CH") ? codeAffaire.substring(2) : codeAffaire;

            // Extraire tous les chiffres
            String chiffres = base.replaceAll("[^0-9]", "");

            if (chiffres.length() >= 3) {
                return chiffres.substring(chiffres.length() - 3);
            }

            return "RB4"; // Valeur par défaut

        } catch (Exception e) {
            log.error("Erreur lors de l'extraction du dépôt: {}", e.getMessage());
            return "RB4";
        }
    }

    /**
     * ⭐⭐⭐ MÉTHODE DÉCOMMENTÉE - ESSENTIELLE POUR VOTRE APPLICATION ⭐⭐⭐
     * Mettre à jour les entêtes BC/BL
     */
    private void mettreAJourEntetesBC_BL(List<LigneConsommation> lignes, BigDecimal pinoCommande, ENT entBL) {
        try {
            log.info("🔄 Début mise à jour entêtes BC/BL pour PINO: {}", pinoCommande);

            // Récupérer l'entête BC
            ENT enteteBC = entRepository.findByPinoAndPicod(pinoCommande, BigDecimal.valueOf(2));

            if (enteteBC == null) {
                log.warn("⚠️ Aucun entête BC trouvé pour PINO: {} - Pas de mise à jour nécessaire", pinoCommande);
                return; // Ne pas bloquer si pas de BC associé
            }

            log.info("✅ Entête BC trouvé - PINO: {}, HTMT: {}", enteteBC.getPino(), enteteBC.getHtmt());

            // Calculer montant réception
            BigDecimal montantReception = calculerMontantReceptionDepuisMouv(lignes);
            log.info("💰 Montant réception calculé: {}", montantReception);

            // ⭐ CALCULER LE TAUX TVA DEPUIS LE BC ⭐
            BigDecimal tauxTva = BigDecimal.ONE;
            if (enteteBC.getHtmt() != null && enteteBC.getHtmt().compareTo(BigDecimal.ZERO) != 0
                    && enteteBC.getTtcmt() != null) {
                tauxTva = enteteBC.getTtcmt().divide(enteteBC.getHtmt(), 4, RoundingMode.HALF_UP);
            }
            log.info("📊 Taux TVA calculé: {}", tauxTva);

            // Mettre à jour BC (soustraire)
            BigDecimal montantBC = enteteBC.getHtmt().subtract(montantReception);
            enteteBC.setTtcmt(montantBC.multiply(tauxTva).setScale(2, RoundingMode.HALF_UP));
            enteteBC.setHtmt(montantBC);
            enteteBC.setHtpdtmt(montantBC);
            entRepository.save(enteteBC);
            log.info("✅ BC mis à jour - Nouveau montant HT: {}", montantBC);

            // ⭐ METTRE À JOUR BL AVEC CALCULS CORRECTS ⭐
            entBL.setHtmt(BigDecimal.valueOf(0.000));
            entBL.setHtpdtmt(BigDecimal.valueOf(0.000));
            entBL.setTtcmt(BigDecimal.valueOf(0.000));

            // Somme des quantités
            BigDecimal totalQte = lignes.stream()
                    .map(LigneConsommation::getQte)
                    .filter(Objects::nonNull)
                    .map(BigDecimal::valueOf)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Affectation
            entBL.setRefnb(totalQte);

            // ⭐ FIX 3: REMPIETOT (calculé proportionnellement du BC) ⭐
            BigDecimal rempietot = BigDecimal.ZERO;
            if (enteteBC.getHtmt() != null && enteteBC.getHtmt().compareTo(BigDecimal.ZERO) != 0
                    && enteteBC.getRempietot() != null) {
                // Calcul proportionnel : (montant_réception / montant_BC_original) * remise_BC_originale
                BigDecimal montantBCOriginal = enteteBC.getHtmt().add(montantReception); // BC avant soustraction
                BigDecimal ratio = montantReception.divide(montantBCOriginal, 4, RoundingMode.HALF_UP);
                rempietot = enteteBC.getRempietot().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            }
            entBL.setRempietot(rempietot);

            entRepository.save(entBL);

            log.info("✅ BC updated: {} | BL created: {}", enteteBC.getHtmt(), montantReception);
            log.info("✅ BL TTCMT: {} (taux TVA: {})", entBL.getTtcmt(), tauxTva);
            log.info("✅ BL REFNB: {} | REMPIETOT: {}", entBL.getRefnb(), entBL.getRempietot());

        } catch (Exception e) {
            log.error("❌ Erreur lors de la mise à jour des entêtes BC/BL: {}", e.getMessage(), e);
            throw new RuntimeException("Échec mise à jour BC/BL: " + e.getMessage(), e);
        }
    }

    /**
     * Calculer le montant de réception depuis les lignes MOUV
     */
    private BigDecimal calculerMontantReceptionDepuisMouv(List<LigneConsommation> lignes) {
        BigDecimal total = BigDecimal.ZERO;

        for (LigneConsommation ligne : lignes) {
            try {
                Optional<MOUV> mouvOpt = mouvRepository.findLigneCommandeByPinoAndRef(
                        BigDecimal.valueOf(ligne.getNumCons()), ligne.getRef());

                if (mouvOpt.isPresent()) {
                    BigDecimal prix = mouvOpt.get().getPub();
                    BigDecimal qte = BigDecimal.valueOf(ligne.getQte());
                    total = total.add(prix.multiply(qte));
                    log.debug("📝 Ligne {} - Prix: {}, Qte: {}, Sous-total: {}",
                            ligne.getRef(), prix, qte, prix.multiply(qte));
                } else {
                    log.warn("⚠️ MOUV non trouvé pour commande: {}, article: {}");
                }
            } catch (Exception e) {
                log.error("❌ Erreur calcul montant pour ligne {}: {}", ligne.getRef(), e.getMessage());
                // Continue avec les autres lignes
            }
        }

        log.info("💰 Montant total calculé: {}", total);
        return total;
    }
}