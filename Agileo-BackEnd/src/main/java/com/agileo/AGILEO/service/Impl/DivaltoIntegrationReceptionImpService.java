package com.agileo.AGILEO.service.Impl;


import com.agileo.AGILEO.Dtos.EntProjection;
import com.agileo.AGILEO.entity.divalto.ENT;
import com.agileo.AGILEO.entity.divalto.MJoint;
import com.agileo.AGILEO.entity.divalto.MOUV;
import com.agileo.AGILEO.entity.primary.*;
import com.agileo.AGILEO.exception.ResourceNotFoundException;
import com.agileo.AGILEO.repository.divalto.*;
import com.agileo.AGILEO.repository.primary.*;
import com.agileo.AGILEO.service.DivaltoIntegrationReceptionService;
import com.agileo.AGILEO.service.SocPrefNoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class DivaltoIntegrationReceptionImpService implements DivaltoIntegrationReceptionService {

    @Autowired
    private ReceptionRepository receptionRepository;

    @Autowired
    private LigneReceptionRepository ligneReceptionRepository;

    @Autowired
    private KdnFileRepository kdnFileRepository;

    @Autowired
    private EntRepository entRepository;;

    @Autowired
    private MouvRepository MOUVRepository;

    @Autowired
    private MJointRepository mJointRepository;

    @Autowired
    @Qualifier("divaltoTransactionManager")
    private PlatformTransactionManager divaltoTransactionManager;

    @PersistenceContext(unitName = "divalto")
    private EntityManager divaltoEntityManager;
    @Autowired
    private SocPrefNoService socPrefNoService;
    /**
     * Point d'entrée principal : Enregistrer une réception dans Divalto
     */
    @Override
    public void integrerReceptionDansDivalto(Integer receptionId, String currentUsername) {
        System.out.println("=== DÉBUT INTÉGRATION RÉCEPTION DIVALTO ===");
        System.out.println("Réception ID: " + receptionId);
        System.out.println("Username: " + currentUsername);

        try {
            // 1. Récupérer la réception
            Reception reception = receptionRepository.findById(receptionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Réception non trouvée: " + receptionId));

            System.out.println("✅ Réception trouvée - Numéro : " + reception.getNumero());
            System.out.println("   - Commande: " + reception.getCommande());
            System.out.println("   - Référence BL: " + reception.getPinotiers());
            System.out.println("   - Statut: " + reception.getSysState());

            // 2. Récupérer les lignes via Ent_ID
            List<LigneReception> lignes = ligneReceptionRepository.findByEntId(reception.getNumero());
            System.out.println("✅ Nombre de lignes trouvées : " + lignes.size());

            if (lignes.isEmpty()) {
                System.err.println("❌ ERREUR : Aucune ligne trouvée !");
                throw new IllegalStateException("La réception n'a aucune ligne");
            }

            for (LigneReception ligne : lignes) {
                System.out.println("   Ligne - REF: " + ligne.getArticle() + ", QTE: " + ligne.getQte() +
                        ", ENRNO: " + ligne.getEnrno() + ", VTLNO: " + ligne.getVtlno());
            }

            // 3. Récupérer les fichiers
            List<KdnFile> fichiers = null;
            if (reception.getPjBc() != null) {
                fichiers = kdnFileRepository.findByGroupIdOrderByUploadDateDesc(reception.getPjBc());
                System.out.println("✅ Nombre de fichiers : " + (fichiers != null ? fichiers.size() : 0));
            } else {
                System.out.println("⚠️ Aucun groupe de fichiers (PjBc est null)");
            }

            // 4. Récupérer le PINO de la commande d'origine
            BigDecimal pinoCommande = null;
            if (reception.getCommande() != null) {
                pinoCommande = BigDecimal.valueOf(reception.getCommande());
                System.out.println("✅ PINO Commande origine : " + pinoCommande);
            }

            // 5. Transaction Divalto
            TransactionTemplate transactionTemplate = new TransactionTemplate(divaltoTransactionManager);
            final List<KdnFile> finalFichiers = fichiers;
            final BigDecimal finalPinoCommande = pinoCommande;

            System.out.println("🔄 Démarrage de la transaction Divalto...");

            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    try {
                        System.out.println("   📝 Création de l'entête BL...");

                        // ✅ Générer le numéro JOINT une seule fois pour toutes les pièces jointes
                        BigDecimal jointNumber = null;
                        if (finalFichiers != null && !finalFichiers.isEmpty()) {
                            jointNumber = getNextJoint();
                            System.out.println("   🔎 Numéro JOINT généré : " + jointNumber);
                        }

                        // Créer l'entête BL (PICOD=3) avec le numéro JOINT et CEJOINT
                        ENT entBL = creerEnteteBL(reception, currentUsername, finalFichiers, jointNumber, finalPinoCommande);
                        System.out.println("   ✅ Entête créé en mémoire - PINO prévu: " + entBL.getPino());

                        entBL = entRepository.save(entBL);
                        System.out.println("   ✅ Entête BL SAUVEGARDÉ - PINO: " + entBL.getPino());

                        // Mettre à jour la réception avec le PINO Divalto
                        reception.setBlDivalto(entBL.getPino().intValue());
                        receptionRepository.save(reception);

                        // Créer les lignes MOUV avec LILG incrémenté
                        System.out.println("   📝 Création des lignes MOUV...");
                        int ligneNumber = 1;
                        for (LigneReception ligne : lignes) {
                            MOUV mouvLig = creerLigneMouv(ligne, entBL, currentUsername, ligneNumber);
                            mouvLig = MOUVRepository.save(mouvLig);
                            System.out.println("   ✅ Ligne MOUV SAUVEGARDÉE - LILG: " + ligneNumber +
                                    ", REF: " + mouvLig.getRef() + ", ENRNO: " + mouvLig.getEnrno());

                            // Mettre à jour le statut d'intégration de la ligne
                            ligne.setIntegre(1); // Intégré
                            ligne.setBlDiva(entBL.getPino().intValue());
                            ligneReceptionRepository.save(ligne);

                            ligneNumber++;
                        }

                        // Créer les pièces jointes avec le MÊME numéro JOINT
                        if (finalFichiers != null && !finalFichiers.isEmpty()) {
                            System.out.println("   📝 Création des pièces jointes...");
                            for (KdnFile fichier : finalFichiers) {
                                MJoint mJoint = creerPieceJointe(fichier, entBL, currentUsername, jointNumber);
                                mJoint = mJointRepository.save(mJoint);
                                System.out.println("   ✅ Pièce jointe SAUVEGARDÉE - JOINT: " + jointNumber +
                                        ", NOM: " + mJoint.getFicc());
                            }
                        }

                        System.out.println("✅ Transaction Divalto RÉUSSIE - Commit en cours...");

                    } catch (Exception e) {
                        System.err.println("❌ ERREUR dans la transaction Divalto: " + e.getMessage());
                        e.printStackTrace();
                        status.setRollbackOnly();
                        throw new RuntimeException("Erreur Divalto: " + e.getMessage(), e);
                    }
                }
            });

            System.out.println("=== FIN INTÉGRATION RÉCEPTION DIVALTO - SUCCÈS ===");

        } catch (Exception e) {
            System.err.println("❌ ERREUR GLOBALE lors de l'intégration Divalto: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur Divalto: " + e.getMessage(), e);
        }
    }

    /**
     * Créer l'entête BL pour Divalto (PICOD=3)
     */
    @Override
    public ENT creerEnteteBL(Reception reception, String currentUsername,
                             List<KdnFile> fichiers, BigDecimal jointNumber, BigDecimal pinoCommande) {
        ENT entBL = new ENT();
        Optional<EntProjection> result = entRepository.findEntInfoByPinoAndPicod(pinoCommande);

        entBL.setCe1("A");
        entBL.setCe2(" ");
        entBL.setCe3("1");
        entBL.setCe4("1");
        entBL.setCe5("1");
        entBL.setCe6(" ");
        entBL.setCe7(" ");
        entBL.setCe8(" ");
        entBL.setCe9(" ");
        entBL.setCea(" ");
        entBL.setCeb(" ");
        entBL.setCec(" ");
        entBL.setCed(" ");
        entBL.setCee(" ");
        entBL.setCef(" ");
        entBL.setDos("1");
        entBL.setTicod("F");
        entBL.setPicod(BigDecimal.valueOf(3));
        entBL.setTiers(result.get().getTiers());
        entBL.setPrefpino("");
        BigDecimal nextPiNo = socPrefNoService.getNextPinoForBL();

        entBL.setPino(nextPiNo);
        entBL.setPidt(LocalDate.from(reception.getSysModificationDate()));
        entBL.setEtb("");
        entBL.setStatus(BigDecimal.valueOf(2));
        entBL.setDev("MAD");
        entBL.setOp("F  ");
        String paddedUsername = String.format("%-20s", currentUsername.toUpperCase());
        entBL.setUsercr(paddedUsername);
        entBL.setUsermo("");
        entBL.setRepr0001("");
        entBL.setRepr0002("");
        entBL.setRepr0003("");
        entBL.setRibcod("");
        entBL.setMarche("");
        entBL.setProjet(result.get().getProjet());
        entBL.setDepo(result.get().getDepo());
        entBL.setAdrtiers0001("                    "); // 20 espaces
        entBL.setAdrtiers0002("                    ");
        entBL.setAdrtiers0003("                    ");
        entBL.setAdrtiers0004("                    ");
        entBL.setAdrtiers0005("                    ");
        entBL.setAdrcod0001("        "); // 8 espaces
        entBL.setAdrcod0002("        ");
        entBL.setAdrcod0003("        ");
        entBL.setAdrcod0004("        ");
        entBL.setAdrcod0005("        ");
        entBL.setBlmod("");
        entBL.setRegl(result.get().getRegl());
        entBL.setTour("");
        String piRef = "";
        if (reception.getNumero() != null) {
            piRef = "N° BL Agileo: " + reception.getNumero();
        }
        entBL.setPiref(piRef);
        entBL.setPinotiers(reception.getPinotiers());
        entBL.setTierspayer("");
        entBL.setTiersgrp("");
        entBL.setTiersrlv("");
        entBL.setBapsalcod("");
        entBL.setSalcod("");
        entBL.setPrefrlvno("");
        entBL.setRlvno(BigDecimal.ZERO);
        entBL.setRlvdt(null);
        entBL.setDeldemdt(result.get().getDeldemdt());

        entBL.setDelaccdt(result.get().getDelaccdt());
        entBL.setDelrepdt(null);
        entBL.setEchdt(null);
        entBL.setTafam("");
        entBL.setTafamx("");
        entBL.setRefam("");
        entBL.setRefamx("");
        entBL.setTacod("");
        entBL.setRemcod("");
        entBL.setCofam("");
        entBL.setCofamv0001("");
        entBL.setCofamv0002("");
        entBL.setCofamv0003("");
        entBL.setAxe0001("");
        entBL.setAxe0002("");
        entBL.setAxe0003("");
        entBL.setAxe0004("");
        entBL.setEtano(" ");
        entBL.setTxtedcodd("");
        entBL.setTxtedcodf("");
        entBL.setContact("");
        entBL.setPrefblasno("");
        entBL.setBlasno(BigDecimal.ZERO);
        entBL.setBlasdepo("");
        entBL.setTpft("");
        entBL.setAvenant("");
        entBL.setCesintcod(BigDecimal.ZERO);
        entBL.setPromotacod("");
        entBL.setPromoremcod("");
        entBL.setPrefcdnopere("");
        entBL.setCdnopere(BigDecimal.ZERO);
        entBL.setTpvbl(BigDecimal.ZERO);
        entBL.setDeeeinccod(BigDecimal.valueOf(0));
        entBL.setPrefpina("");
        entBL.setPina(BigDecimal.ZERO);
        entBL.setUsercrdh(LocalDateTime.now());
        entBL.setUsermodh(null);
        entBL.setCenote(BigDecimal.ONE);
        entBL.setNote(BigDecimal.ZERO);
        entBL.setTxtcodd(BigDecimal.ONE);
        entBL.setTxtcodf(BigDecimal.ONE);
        entBL.setTxtnoted(BigDecimal.ZERO);
        entBL.setTxtnotef(BigDecimal.ZERO);
        entBL.setOrigine(result.get().getOrigine());
        entBL.setHtmt(BigDecimal.valueOf(9999.99));           // Montant HT
        entBL.setTtcmt(BigDecimal.valueOf(99999.99));
        entBL.setHtpdtmt(BigDecimal.valueOf(99999.99));
        entBL.setEscp(BigDecimal.valueOf(0.00));
        entBL.setAcmt(BigDecimal.valueOf(0,00));
        entBL.setSoacmt(BigDecimal.valueOf(0.00));
        entBL.setRemmt(BigDecimal.valueOf(0.00));
        entBL.setRem1(BigDecimal.valueOf(0.00));
        entBL.setRemtyp1(BigDecimal.ONE);
        entBL.setFouhtmt(BigDecimal.valueOf(0.00));
        entBL.setFouescmt(BigDecimal.valueOf(0.00));       // Escompte fournisseur
        entBL.setFoutvamt(BigDecimal.valueOf(0.00));
        entBL.setDevp(BigDecimal.valueOf(1.0000));
        entBL.setPiedno0001(BigDecimal.ZERO);
        entBL.setPiedno0002(BigDecimal.ZERO);
        entBL.setPiedno0003(BigDecimal.ZERO);
        entBL.setPiedmt0001(BigDecimal.valueOf(0.00));
        entBL.setPiedmt0002(BigDecimal.valueOf(0.00));
        entBL.setPiedmt0003(BigDecimal.valueOf(0.00));
        entBL.setNbex(BigDecimal.ONE);
        entBL.setPirelcod(BigDecimal.ONE);
        entBL.setRelcod(BigDecimal.valueOf(2));
        entBL.setEditcod(BigDecimal.ONE);
        entBL.setTrcod(BigDecimal.ONE);
        entBL.setBoredicod(BigDecimal.ONE);
        entBL.setAscod(BigDecimal.ONE);
        entBL.setEchvcod(BigDecimal.ONE);
        entBL.setEncasscod(BigDecimal.ONE);
        entBL.setAdrtyp0001(BigDecimal.ONE);
        entBL.setAdrtyp0002(BigDecimal.ONE);
        entBL.setAdrtyp0003(BigDecimal.ONE);
        entBL.setAdrtyp0004(BigDecimal.ONE);
        entBL.setAdrtyp0005(BigDecimal.ONE);
        entBL.setPriocod(BigDecimal.ZERO);
        entBL.setHtcod(BigDecimal.ONE);
        entBL.setStres(BigDecimal.ONE);
        entBL.setFamod(BigDecimal.ZERO);
        entBL.setPeriod(BigDecimal.ZERO);
        entBL.setPorcod(BigDecimal.ONE);
        entBL.setPoicod(BigDecimal.ONE);
        entBL.setVolcod(BigDecimal.ONE);
        entBL.setPorfrfl(BigDecimal.valueOf(2));
        entBL.setPoitot(BigDecimal.valueOf(0,00));
        entBL.setVoltot(BigDecimal.valueOf(0,00));
        entBL.setColinb(BigDecimal.ZERO);
        BigDecimal totalQuantite = ligneReceptionRepository.sumQuantiteByEntId(reception.getNumero());

        entBL.setRefnb(totalQuantite);
        entBL.setTourrg(BigDecimal.ZERO);
        entBL.setRem0001(BigDecimal.valueOf(0,00));
        entBL.setRem0002(BigDecimal.valueOf(0,00));
        entBL.setRem0003(BigDecimal.valueOf(0,00));
        entBL.setRemtyp0001(BigDecimal.valueOf(2));
        entBL.setRemtyp0002(BigDecimal.valueOf(2));
        entBL.setRemtyp0003(BigDecimal.valueOf(2));
        entBL.setComp0001(BigDecimal.valueOf(0.00));
        entBL.setComp0002(BigDecimal.valueOf(0.00));
        entBL.setComp0003(BigDecimal.valueOf(0.00));
        entBL.setPortheomt(BigDecimal.valueOf(0.00));
        entBL.setRempietot(BigDecimal.valueOf(0.00));
        entBL.setTransjrnb(BigDecimal.ZERO);
        entBL.setOfascod(BigDecimal.ZERO);
        entBL.setFinalField(BigDecimal.ONE);
        entBL.setQuacod(BigDecimal.valueOf(2));
        if (fichiers != null && !fichiers.isEmpty()) {
            entBL.setCejoint(BigDecimal.valueOf(2));
            entBL.setJoint(jointNumber);
            entBL.setCenote(BigDecimal.valueOf(1)); // Note présente
        } else {
            entBL.setCejoint(BigDecimal.ONE);
            entBL.setJoint(BigDecimal.ZERO);

        }
        entBL.setDeeemt(BigDecimal.ZERO);
        entBL.setFoudeeemt(BigDecimal.valueOf(0,00));
        entBL.setPrgcdeflg(BigDecimal.ONE);
        entBL.setBqcpce(result.get().getBqcpce());
        entBL.setPoincod(BigDecimal.ONE);
        entBL.setPointot(BigDecimal.valueOf(0,00));
        entBL.setPrioreg(BigDecimal.ZERO);
        entBL.setTvatie("0");
        entBL.setStlgtgamcod("");
        entBL.setDtflg(BigDecimal.ONE);
        entBL.setSynchrofl(BigDecimal.ONE);
        entBL.setIcpfl(BigDecimal.ONE);
        entBL.setLieuinct("");
        entBL.setPorfrcod(BigDecimal.ONE);
        entBL.setPorfrval(BigDecimal.ZERO);
        entBL.setTransicod("");
        entBL.setTvablcd3("");
        entBL.setCeatraitefl(BigDecimal.ZERO);
        entBL.setSitecod("");
        entBL.setUpDemandeur(String.format("%-20s", currentUsername.toUpperCase()));
        entBL.setUpDaterecuperation(null);
        entBL.setBexno(BigDecimal.ZERO);
        entBL.setBlqfl(BigDecimal.ONE);
        entBL.setConfirmationfl(BigDecimal.ZERO);
        entBL.setTaxcplffl(BigDecimal.ONE);
        entBL.setTaxsfvfl(BigDecimal.ONE);
        entBL.setTvaautoliqfl(BigDecimal.ONE);
        entBL.setUnlogcod(BigDecimal.ONE);
        entBL.setUnlogtot(BigDecimal.valueOf(0,00));
        entBL.setUntyp("");
        entBL.setValfindt(null);
        entBL.setVersiondevisno(BigDecimal.ZERO);
        entBL.setVersiondevisoripino(BigDecimal.ZERO);
        entBL.setVersiondevisoriprefpino("");
        entBL.setBprelcod(BigDecimal.ZERO);
        entBL.setCatpicod("");
        entBL.setCircuitvalidationblfl(BigDecimal.ZERO);
        entBL.setCircuitvalidationfctfl(BigDecimal.ZERO);
        entBL.setCondexp("");
        entBL.setEtablno(" ");
        entBL.setFraisappcod("");
        entBL.setGouvfacblqfl(BigDecimal.ONE);
        entBL.setIndiceno(BigDecimal.ZERO);
        entBL.setModeexp("");
        entBL.setMotif("");
        entBL.setPiecedt(null);
        entBL.setPrefsitno("");
        entBL.setRemseuilfl(BigDecimal.ONE);
        entBL.setSitno(BigDecimal.ZERO);
        entBL.setTransitfl(BigDecimal.ZERO);
        entBL.setAcomptetyp(BigDecimal.ZERO);
        entBL.setBidon(BigDecimal.ZERO);
        entBL.setBpjrnb(BigDecimal.ZERO);
        entBL.setPaiementtyp(BigDecimal.ZERO);
        entBL.setResjrnb(BigDecimal.ZERO);
        entBL.setStnfl(BigDecimal.ZERO);
        entBL.setFano(BigDecimal.ZERO);
        entBL.setPreffano("");
        entBL.setReglimmfl(BigDecimal.ONE);
        entBL.setTiersfact("");
        entBL.setBtfullpino("0");
        entBL.setBtpino(BigDecimal.ZERO);
        entBL.setBtprefpino("");
        entBL.setBtretourfl(BigDecimal.ZERO);
        entBL.setBtstatus(BigDecimal.ZERO);







        return entBL;
    }

    /**
     * Créer une ligne MOUV dans Divalto (pour réception)
     */
    @Override
    public MOUV creerLigneMouv(LigneReception ligne, ENT entBL,
                               String currentUsername, int ligneNumber) {
        MOUV mouv = new MOUV();

        // ID sera généré automatiquement
        // mouv.setMouvId() - auto-generated

        // ENRNO - Numéro d'enregistrement
        mouv.setEnrno(BigDecimal.valueOf(ligne.getEnrno()));

        // TICOD - Type de mouvement
        mouv.setTicod("F");

        // DOS - Dossier
        mouv.setDos("1");

        // ETB - Établissement
        mouv.setEtb(entBL != null && entBL.getEtb() != null ? entBL.getEtb() : "948");

        // Codes établissement (CE1-CEF)
        mouv.setCe1("C");
        mouv.setCe2("1");
        mouv.setCe3("1");
        mouv.setCe4("");
        mouv.setCe5("");
        mouv.setCe6("1");
        mouv.setCe7("1");
        mouv.setCe8(" ");
        mouv.setCe9("1");
        mouv.setCea(" ");
        mouv.setCeb(" ");
        mouv.setCec(" ");
        mouv.setCed(" ");
        mouv.setCee(" ");
        mouv.setCef(" ");

        // DEPO - Dépôt
        mouv.setDepo(entBL.getDepo() != null ? entBL.getDepo() : "");

        // Données devis (DV)
        mouv.setPrefdvno("");
        mouv.setDvno(BigDecimal.ZERO);
        mouv.setDvdt(null);
        mouv.setDvlg(BigDecimal.ZERO);
        mouv.setDvslg(BigDecimal.ZERO);

        // PICOD - Code pièce
        mouv.setPicod(BigDecimal.valueOf(3));

        // REF - Référence article
        mouv.setRef(ligne.getArticle() != null ? ligne.getArticle() : "");

        // DVCE4
        mouv.setDvce4("");

        // PREFCDNO
        mouv.setPrefcdno("");

        // SREF - Sous-références
        mouv.setSref1(ligne.getSref1() != null ? ligne.getSref1() : "");
        mouv.setSref2(ligne.getSref2() != null ? ligne.getSref2() : "");

        // TIERS - Code tiers
        mouv.setTiers(entBL.getTiers());



        mouv.setRem0001(BigDecimal.valueOf(0,00));
        mouv.setRem0002(BigDecimal.valueOf(0,00));
        mouv.setRem0003(BigDecimal.valueOf(0,00));

        // Données BL (Bon de Livraison)
        mouv.setBlno( entBL.getPino());
        mouv.setBldt(entBL.getPidt());



        mouv.setFano(BigDecimal.ZERO);
        mouv.setFadt(null);

        // Données commande (CD)
        mouv.setCdno(BigDecimal.valueOf(ligne.getCommande()));
        mouv.setCddt( null);

        // OP - Opérateur
        mouv.setOp("F");

        // DEV - Devise
        mouv.setDev("MAD");

        // Utilisateurs
        mouv.setUsercr(currentUsername);
        mouv.setUsermo(currentUsername);

        // PROJET
        mouv.setProjet(entBL != null && entBL.getProjet() != null ? entBL.getProjet() : "");

        // DES - Désignation
        mouv.setDes(ligne.getDeseignation() != null ? ligne.getDeseignation() : "");






        // MARCHE
        mouv.setMarche(entBL != null && entBL.getMarche() != null ? entBL.getMarche() : "");

        // Dates de création et modification
        LocalDateTime now = LocalDateTime.now();
        mouv.setUsercrdh(now);
        mouv.setUsermodh(now);

        mouv.setStatus(BigDecimal.valueOf(2));

        mouv.setBlqte(ligne.getQte() != null ? ligne.getQte() : BigDecimal.ZERO);


        // Unités
        mouv.setRefun(ligne.getUnite() != null ? ligne.getUnite() : "UN");
        mouv.setVenun(ligne.getUnite() != null ? ligne.getUnite() : "UN");

        // Prix
        mouv.setPub(BigDecimal.ZERO);
        mouv.setPpar( BigDecimal.ZERO);



        // Montants
   /*     BigDecimal quantite = ligne.getQuantiteRecue() != null ? ligne.getQuantiteRecue() : BigDecimal.ZERO;
        BigDecimal prix = ligne.getPrixUnitaire() != null ? ligne.getPrixUnitaire() : BigDecimal.ZERO;
        BigDecimal montantHT = quantite.multiply(new BigDecimal(999.99));*/
        BigDecimal montantHT = new BigDecimal(999.99);


        mouv.setRemmt(BigDecimal.ZERO);


        // Colonnes techniques et administratives
        mouv.setCenote(BigDecimal.valueOf(1));
        mouv.setCmptotmt(montantHT);
        mouv.setCoecod("");
        mouv.setCofamr("");
        mouv.setCofamv0001("");
        mouv.setCofamv0002("");
        mouv.setCofamv0003("");

        // Commissions et compensations
        mouv.setCommt0001(BigDecimal.ZERO);
        mouv.setCommt0002(BigDecimal.ZERO);
        mouv.setCommt0003(BigDecimal.ZERO);
        mouv.setComp0001(BigDecimal.ZERO);
        mouv.setComp0002(BigDecimal.ZERO);
        mouv.setComp0003(BigDecimal.ZERO);

        // Indices et codes
        mouv.setAfrindice("");
        mouv.setAppremmt(BigDecimal.ZERO);
        mouv.setAppremmtun(BigDecimal.ZERO);
        mouv.setAvenant("");

        // Axes analytiques
        mouv.setAxe0001("");
        mouv.setAxe0002(entBL != null && entBL.getProjet() != null ? entBL.getProjet() : "");
        mouv.setAxe0003("");
        mouv.setAxe0004("");

        // Numéros et références
        mouv.setBesoinno(BigDecimal.ZERO);
        mouv.setBlasenrno(BigDecimal.ZERO);
        mouv.setBlce4("1");
        mouv.setBlenrno(BigDecimal.ZERO);
        mouv.setBllg(BigDecimal.valueOf(ligneNumber));
        mouv.setBlslg(BigDecimal.ZERO);

        // Dates et bons de production
        mouv.setBpdt(null);
        mouv.setBpligcompfl(BigDecimal.ZERO);
        mouv.setBpno(BigDecimal.ZERO);

        // Flags et options
        mouv.setCadeaufl(BigDecimal.ZERO);
        mouv.setCdnopere(BigDecimal.ZERO);
        mouv.setCdqte(BigDecimal.ZERO);

        // Configurateur
        mouv.setConfigurateurlino(BigDecimal.ZERO);
        mouv.setConfigurateurmonostatus(BigDecimal.ZERO);
        mouv.setConfigurateurmultistatus(BigDecimal.ZERO);
        mouv.setConfigurateurref("");
        mouv.setConfigurateursref1("");
        mouv.setConfigurateursref2("");

        // Contrats et comptabilité
        mouv.setContratcod("");
        mouv.setCptv("");
        mouv.setCrtotmt(montantHT);
        mouv.setCtmfl(BigDecimal.valueOf(1));

        // Codes et références
        mouv.setDeccod(BigDecimal.valueOf(1));
        mouv.setDepoorig("");
        mouv.setDtrenrno(BigDecimal.ZERO);
        mouv.setDtrgrp(BigDecimal.ZERO);
        mouv.setDtrtype(BigDecimal.ZERO);
        mouv.setDvenrno(BigDecimal.ZERO);
        mouv.setDvqte(BigDecimal.ZERO);

        // Codes divers
        mouv.setEdcod("");
        mouv.setElemno(BigDecimal.ZERO);
        mouv.setEmbqte(BigDecimal.ZERO);
        mouv.setEmbun("");

        // ENRNO collections
        mouv.setEnrnoc0001(BigDecimal.ZERO);
        mouv.setEnrnoc0002(BigDecimal.ZERO);
        mouv.setEnrnoc0003(BigDecimal.ZERO);
        mouv.setEnrnoc0004(BigDecimal.ZERO);
        mouv.setEnrnocad(BigDecimal.ZERO);
        mouv.setEnrnop0001(BigDecimal.ZERO);
        mouv.setEnrnop0002(BigDecimal.ZERO);
        mouv.setEnrnop0003(BigDecimal.ZERO);
        mouv.setEnrnop0004(BigDecimal.ZERO);

        // Codes facture
        mouv.setFace4(" ");
        mouv.setFalg(BigDecimal.ZERO);
        mouv.setFamontgim(BigDecimal.ZERO);
        mouv.setFapubgim(BigDecimal.ZERO);
        mouv.setFaqte(BigDecimal.ZERO);
        mouv.setFaslg(BigDecimal.ZERO);

        // Divers
        // Divers
        mouv.setFillersens(BigDecimal.ZERO);
        mouv.setFoufadtgim(null);
        mouv.setFoufanogim("");
        mouv.setFoufaqtegim(BigDecimal.ZERO);

        // Frais
        mouv.setFraisappcod("");
        mouv.setFraisfl(BigDecimal.ZERO);
        mouv.setFraisimpactflg(BigDecimal.ZERO);
        mouv.setFraismt(BigDecimal.ZERO);
        mouv.setFraismtgim(BigDecimal.ZERO);
        mouv.setFraisvalidtyp(BigDecimal.ZERO);

        // Dates et codes
        mouv.setGadt(null);
        mouv.setGamseq("");
        mouv.setGimcod("");
        mouv.setGpafl(BigDecimal.ZERO);
        mouv.setGratuitfl(BigDecimal.ZERO);

        // Codes IAG
        mouv.setIagcdenrno(BigDecimal.ZERO);
        mouv.setIagfluxlien("");
        mouv.setIcpfl(BigDecimal.ZERO);
        mouv.setLigne(BigDecimal.valueOf(0));
        mouv.setLivdirectfl(BigDecimal.ZERO);

        // Montants et motifs
        mouv.setMont(montantHT);
        mouv.setMotif("");
        mouv.setMotifsolde("");
        mouv.setMvcod(BigDecimal.valueOf(2));
        mouv.setMvstat(BigDecimal.valueOf(1));
        mouv.setNote(BigDecimal.ZERO);

        // Numéros d'offre et options
        mouv.setOfno(BigDecimal.ZERO);
        mouv.setOptionfl(BigDecimal.ONE);
        mouv.setOptionvalidefl(BigDecimal.ONE);
        mouv.setPaforf(BigDecimal.ONE);
        mouv.setPagcod("");
        mouv.setPanachefl(BigDecimal.ONE);
        mouv.setPatotmt(BigDecimal.valueOf(0.000000));

        // Périodes
        mouv.setPeriodeddt(null);
        mouv.setPeriodefdt(null);
        mouv.setPfcno(BigDecimal.ZERO);
        mouv.setPosition("");

        // Préfixes
        mouv.setPrefblno("");
        mouv.setPrefcdnopere("");
        mouv.setPreffano("");
        mouv.setPrefofno("");

        // Quantités programme
        mouv.setPrgqte(BigDecimal.ZERO);
        mouv.setPrgrefqte(BigDecimal.ZERO);
        mouv.setPriocod(BigDecimal.ZERO);
        mouv.setPrixspecialfl(BigDecimal.ZERO);

        // Codes promotion
        mouv.setPromoremcod("");
        mouv.setPromotacod("");
        mouv.setPromotyp(BigDecimal.valueOf(1));
        mouv.setPubtyp(BigDecimal.valueOf(1));
        mouv.setPubun(ligne.getUnite() != null ? ligne.getUnite() : "UN");

        // Prix et statuts
        mouv.setPunetori(BigDecimal.ZERO);
        mouv.setPustat(BigDecimal.valueOf(1));
        mouv.setPvcod(BigDecimal.valueOf(1));

        // Quantités types
        mouv.setQte1(BigDecimal.ZERO);
        mouv.setQte2(BigDecimal.ZERO);
        mouv.setQte3(BigDecimal.ZERO);
        mouv.setQtetyp(BigDecimal.valueOf(1));

        // Codes divers
        mouv.setRebucod("");
        mouv.setRecptno(BigDecimal.ZERO);
        mouv.setRefamr("");
        mouv.setRefamrx("");
        mouv.setReffo("");
        mouv.setRefqte(BigDecimal.ZERO);
        mouv.setReglecod("");
        mouv.setCdce4(BigDecimal.valueOf(8));
         mouv.setCdenrno(BigDecimal.ZERO);
        // Codes relation
        mouv.setRelcod0001(BigDecimal.ZERO);
        mouv.setRelcod0002(BigDecimal.valueOf(2));
        mouv.setRelcod0003(BigDecimal.valueOf(2));
mouv.setArtind("");
        // Remises détaillées



        mouv.setRemcod("");
        mouv.setRemcodcad("");

        // Remboursements
        mouv.setRempiemt0001(BigDecimal.ZERO);
        mouv.setRempiemt0002(BigDecimal.ZERO);
        mouv.setRempiemt0003(BigDecimal.ZERO);
        mouv.setRempiemt0004(BigDecimal.ZERO);
        mouv.setRgpenrno(BigDecimal.ZERO);
        mouv.setSens(BigDecimal.valueOf(1));
        mouv.setSolderelfl(BigDecimal.ZERO);
        mouv.setStres(BigDecimal.valueOf(1));
        mouv.setSynchrofl(BigDecimal.ONE);

        // Codes tarifs
        mouv.setTacod("");
        mouv.setTafamr("");
        mouv.setTafamrx("");
        mouv.setTicket(BigDecimal.ZERO);

        // Tiers externes
        mouv.setTiersexterne("");
        mouv.setTiersfou2("");

        // TVA
        mouv.setTvaart("1");
        mouv.setTvanassujettiefl(BigDecimal.valueOf(1));

        // Textes
        mouv.setTxtcod(BigDecimal.ZERO);
        mouv.setTxtedcod("");
        mouv.setTxtnote(BigDecimal.ZERO);

        // Type d'unité
        mouv.setUntyp("");


        mouv.setArtind("");

        mouv.setCdlg(BigDecimal.ZERO);
        mouv.setCdslg(BigDecimal.ZERO);
        mouv.setConfurateurartind("");
        mouv.setPcod0001(BigDecimal.valueOf(4));
        mouv.setPcod0002(BigDecimal.valueOf(4));
        mouv.setPcod0003(BigDecimal.valueOf(2));
        mouv.setPcod0004(BigDecimal.valueOf(2));
        mouv.setPcod0005(BigDecimal.valueOf(2));
        mouv.setPcod0006(BigDecimal.valueOf(4));
        mouv.setRempiepart0001(BigDecimal.ZERO);
        mouv.setRempiepart0002(BigDecimal.ZERO);
        mouv.setRempiepart0003(BigDecimal.ZERO);
        mouv.setRempiepart0004(BigDecimal.ZERO);
        mouv.setRemtyp0001(BigDecimal.ONE)  ;
        mouv.setRemtyp0002(BigDecimal.ONE);
        mouv.setRemtyp0003(BigDecimal.ONE)	;
        mouv.setRepr0001("");
        mouv.setRepr0002("");
        mouv.setRepr0003("");
        mouv.setIaglientyp(BigDecimal.ZERO);



        return mouv;
    }

    /**
     * Créer une pièce jointe dans MJOINT
     */
    @Override
    public MJoint creerPieceJointe(KdnFile fichier, ENT entBL,
                                   String currentUsername, BigDecimal jointNumber) {
        MJoint mJoint = new MJoint();

        // ✅ Utiliser le numéro JOINT passé en paramètre
        mJoint.setJoint(jointNumber);

        mJoint.setApplic("DAV");
        mJoint.setJointobj("ENT"); // Pour BL

        String fileName = fichier.getName();
        if (fichier.getExtension() != null && !fichier.getExtension().isEmpty()) {
            fileName += "." + fichier.getExtension();
        }
        mJoint.setFicc(fileName);
        mJoint.setChemin("//cetus/divalto/rb217/fichiers/ficjoints_op");

        mJoint.setLib80("");

        if (fichier.getSize() != null) {
            mJoint.setTaillefic(BigDecimal.valueOf(fichier.getSize()));
        } else {
            mJoint.setTaillefic(BigDecimal.ZERO);
        }

        mJoint.setConffic("");
        mJoint.setConfl("");
        mJoint.setSuppfl(BigDecimal.ZERO);
        mJoint.setCrypst(BigDecimal.ZERO);
        mJoint.setMotcle("");
        mJoint.setNaturejointcod("ENT_F_BL"); // ✅ Nature pour BL fournisseur
        mJoint.setVersionagileo(BigDecimal.ZERO);
        mJoint.setIdagileofile(BigDecimal.ZERO);
        mJoint.setIdagileo(BigDecimal.ZERO);
        mJoint.setIdagileodoc(BigDecimal.ZERO);

        LocalDateTime now = LocalDateTime.now();
        mJoint.setUsercrdh(now);
        mJoint.setCredh(now);
        mJoint.setUsermodh(now);
        mJoint.setModifdh(now);

        String paddedUsername = String.format("%-20s", currentUsername.toUpperCase());
        mJoint.setUsercr(paddedUsername);
        mJoint.setUsermo(paddedUsername);

        return mJoint;
    }



    /**
     * Obtenir le prochain JOINT
     */
    private BigDecimal getNextJoint() {
        try {
            String sql = "SELECT MAX(JOINT) FROM MJOINT";
            Query query = divaltoEntityManager.createNativeQuery(sql);
            Object result = query.getSingleResult();

            if (result != null) {
                BigDecimal maxJoint = new BigDecimal(result.toString());
                return maxJoint.add(BigDecimal.ONE);
            } else {
                return BigDecimal.ONE;
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du dernier JOINT: {}", e.getMessage());
            return BigDecimal.ONE;
        }
    }

    /**
     * Obtenir le prochain ENRNO (identifiant ligne MOUV)
     */
    private BigDecimal getNextEnrNo() {
        try {
            String sql = "SELECT MAX(ENRNO) FROM MOUV";
            Query query = divaltoEntityManager.createNativeQuery(sql);
            Object result = query.getSingleResult();

            if (result != null) {
                BigDecimal maxEnrNo = new BigDecimal(result.toString());
                return maxEnrNo.add(BigDecimal.ONE);
            } else {
                return new BigDecimal("300000");
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du dernier ENRNO: {}", e.getMessage());
            return new BigDecimal("300000");
        }
    }

    /**
     * Extraire le dépôt du code de commande (3 derniers chiffres)
     */
    private String extraireDepotDuCodeCommande(Integer commandeId) {
        try {
            // Récupérer le code affaire depuis la commande
            String sql = "SELECT TOP 1 PROJET FROM ENT WHERE PICOD=2 AND DOS='1' AND TICOD='F' " +
                    "AND CAST(REPLACE(PROJET, 'CH', '') AS INT) = ?";
            Query query = divaltoEntityManager.createNativeQuery(sql);
            query.setParameter(1, commandeId);
            Object result = query.getSingleResult();

            if (result != null) {
                String projet = result.toString().trim();

                // Supprimer le préfixe "CH" si présent
                String base = projet.startsWith("CH") ? projet.substring(2) : projet;

                // Extraire les 3 derniers chiffres
                String numeros = base.replaceAll("[^0-9]", "");
                if (numeros.length() >= 3) {
                    return numeros.substring(numeros.length() - 3);
                } else if (!numeros.isEmpty()) {
                    return numeros;
                }
            }

            return "1"; // Valeur par défaut

        } catch (Exception e) {
            log.error("Erreur lors de l'extraction du dépôt: {}", e.getMessage());
            return "1";
        }
    }
}