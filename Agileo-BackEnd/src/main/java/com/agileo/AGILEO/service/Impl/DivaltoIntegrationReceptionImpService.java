package com.agileo.AGILEO.service.Impl;


import com.agileo.AGILEO.Dtos.EntProjection;
import com.agileo.AGILEO.entity.divalto.*;
import com.agileo.AGILEO.entity.primary.*;
import com.agileo.AGILEO.exception.ResourceNotFoundException;
import com.agileo.AGILEO.repository.divalto.*;
import com.agileo.AGILEO.repository.primary.*;
import com.agileo.AGILEO.service.DivaltoIntegrationReceptionService;
import com.agileo.AGILEO.service.SocPrefNoService;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
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
    private ArtRepository artRepository;

    @Autowired
    private EntRepository entRepository;;

    @Autowired
    private MouvRepository mouvRepository;

    @Autowired
    private SocnoRepository socnoRepository;
    @Autowired
    private MJointRepository mJointRepository;

    @Autowired
    @Qualifier("divaltoTransactionManager")
    private PlatformTransactionManager divaltoTransactionManager;

    @PersistenceContext(unitName = "divalto")
    private EntityManager divaltoEntityManager;
    @Autowired
    private SocPrefNoService socPrefNoService;
    @Autowired
    private MvtlRepository mvtlRepository;


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

                        try {
                            entBL = entRepository.save(entBL);
                            System.out.println("✅ Entête BL SAUVEGARDÉ avec succès - PINO: " + entBL.getPino());
                        } catch (Exception e) {
                            System.err.println("❌ ERREUR SAUVEGARDE entête BL:");
                            System.err.println("   PINO tenté: " + entBL.getPino());
                            System.err.println("   Message: " + e.getMessage());
                            if (e.getCause() != null) {
                                System.err.println("   Cause: " + e.getCause().getMessage());
                            }
                            throw new RuntimeException("Échec sauvegarde entête BL - PINO: " + entBL.getPino(), e);
                        }

                        System.out.println("   ✅ Entête BL SAUVEGARDÉ - PINO: " + entBL.getPino());

// ⭐ APPEL DES MÉTHODES POUR METTRE À JOUR LES ENTÊTES ⭐
                        System.out.println("   🔄 Mise à jour des entêtes BC/BL...");
                        mettreAJourEntetesBC_BL(lignes, finalPinoCommande, entBL);
                        System.out.println("   ✅ Entêtes BC/BL mis à jour avec succès !");

// Mettre à jour la réception avec le PINO Divalto
                        reception.setBlDivalto(entBL.getPino().intValue());
                        receptionRepository.save(reception);

                        // Créer les lignes MOUV avec LILG incrémenté
                        System.out.println("   📝 Création des lignes MOUV...");
                        int ligneNumber = 1;


                        int nbLigneBL=0;

                        for (LigneReception ligne : lignes) {
                            Integer maxnbLignesDivalto=mouvRepository.maxNbLigneBLByBC( BigDecimal.valueOf(ligne.getCommande()),
                                    ligne.getArticle());
                            if(maxnbLignesDivalto != null){
                                nbLigneBL=maxnbLignesDivalto;
                            }else{
                                nbLigneBL=0;
                            }
                            nbLigneBL++;

                            Optional<MOUV> mouvOpt = mouvRepository.findLigneCommandeByPinoAndRef(
                                    BigDecimal.valueOf(ligne.getCommande()),
                                    ligne.getArticle()
                            );

                            if (mouvOpt.isPresent()) {
                                MOUV mouvForUpdate = mouvOpt.get();
                                MOUV mouvForInsert = new MOUV();
                                BeanUtils.copyProperties(mouvForUpdate, mouvForInsert);

                                Mvtl mvtlOriginal = mvtlRepository.findMouvementByEnrno(mouvForInsert.getRef(), BigDecimal.valueOf(ligne.getCommande())).get();

                                // Créer une référence pour l'update (sera transformé en BL)
                                Mvtl mvtlForUpdate = mvtlOriginal;

                                // Créer une copie pour l'insert (nouveau BC avec reste)
                                Mvtl mvtlForInsert = new Mvtl();
                                BeanUtils.copyProperties(mvtlOriginal, mvtlForInsert);

                                // 1️⃣ Modifier l'existant
                                mouvForUpdate.setCe7("1");
                                mouvForUpdate.setCe9("1");
                                mouvForUpdate.setPicod(BigDecimal.valueOf(3));
                                mouvForUpdate.setCdlg(BigDecimal.valueOf(2));
                                mouvForUpdate.setCdce4("8");
                                mouvForUpdate.setBlno(entBL.getPino());
                                mouvForUpdate.setBldt(entBL.getPidt());
                                mouvForUpdate.setBllg(BigDecimal.valueOf(nbLigneBL));
                                mouvForUpdate.setBlce4("1");
                                mouvForUpdate.setCdqte(ligne.getQte());
                                mouvForUpdate.setBlqte(ligne.getQte());
                                BigDecimal qte = ligne.getQte();
                                BigDecimal pub = mouvForUpdate.getPub();
                                BigDecimal montant = qte.multiply(pub);
                                mouvForUpdate.setMont(montant);
                                mouvForUpdate.setCrtotmt(montant); //
                                mouvForUpdate.setCmptotmt(montant); //
                                mouvForUpdate.setPrgqte(ligne.getQte());
                                mouvForUpdate.setArtind("        ");
                                mouvForUpdate.setBlenrno(mouvForUpdate.getEnrno());
                                mouvForUpdate.setSolderelfl(BigDecimal.ONE);
                                mouvForUpdate.setPustat(mouvForUpdate.getPub());
                                mouvForUpdate.setRefqte(qte);
                                mouvForUpdate.setLivdirectfl(BigDecimal.valueOf(2));

                                mouvRepository.save(mouvForUpdate);
                                mouvRepository.flush();
                                // 2️⃣ Détacher l'entité du contexte de persistance
                                divaltoEntityManager.detach(mouvForUpdate);

                                // 3️⃣ Réinitialiser l'ID pour créer une nouvelle ligne
                                BigDecimal nouvelleCdqte = mouvForInsert.getCdqte().subtract(ligne.getQte());
                                if (nouvelleCdqte.compareTo(BigDecimal.ZERO) > 0){
                                    divaltoEntityManager.detach(mouvForInsert);
                                    mouvForInsert.setMouvId(null);

                                    mouvForInsert.setEnrno(socnoRepository.findByNumEnrgForUpdate().add(BigDecimal.ONE));
                                    socnoRepository.incrementNumEnrg();
                                    mouvForInsert.setCdqte(nouvelleCdqte);
                                    mouvForInsert.setRefqte(nouvelleCdqte);
                                    mouvForInsert.setCe7(" ");
                                    mouvForInsert.setCe9(" ");
                                    mouvForInsert.setArtind("        ");
                                    mouvForInsert.setCdce4("1");
                                    mouvForInsert.setBllg(BigDecimal.ONE);
                                    mouvForInsert.setBlno(BigDecimal.ZERO);
                                    mouvForInsert.setBllg(BigDecimal.ZERO);
                                    mouvForInsert.setLivdirectfl(BigDecimal.valueOf(2));
                                    mouvForInsert.setBlqte(BigDecimal.ZERO);
                                    mouvForInsert.setBlqte(BigDecimal.ZERO);
                                    mouvForInsert.setBlenrno(BigDecimal.ZERO);
                                    mouvForInsert.setSolderelfl(BigDecimal.ZERO);
                                    mouvForInsert.setBldt(null);
                                    mouvForInsert.setBlce4(" ");
                                    mouvForInsert.setPicod(BigDecimal.valueOf(2));
                                    BigDecimal nouveauMontant = nouvelleCdqte.multiply(mouvForInsert.getPub());
                                    mouvForInsert.setMont(nouveauMontant);
                                    mouvForInsert.setCrtotmt(nouveauMontant);
                                    mouvForInsert.setCmptotmt(nouveauMontant);
                                    // 5️⃣ Sauvegarder = INSERT d'une nouvelle ligne
                                    MOUV mouvBC=mouvForInsert;
                                    mouvRepository.save(mouvForInsert);
                                    mouvRepository.flush();
                                    System.out.println("   ✅ Nouvelle ligne créée par clonage"+mouvForInsert);
                                    ///////  creation ventilation BC////////
                                    System.out.println("mvtlForInsert : "+mvtlForInsert);
                                    divaltoEntityManager.detach(mvtlForInsert);
                                    mvtlForInsert.setMvtlId(null);
                                    mvtlForInsert.setCe3("1");
                                    mvtlForInsert.setCe1("V");
                                    mvtlForInsert.setCe4("1");
                                    mvtlForInsert.setEnrno(mouvBC.getEnrno());
                                    mvtlForInsert.setUsercrdh(LocalDate.now().atStartOfDay());
                                    BigDecimal nouveauVTLNO = socnoRepository.findByVtlnoForUpdate().add(BigDecimal.ONE);
                                    mvtlForInsert.setVtlno(nouveauVTLNO);
                                    socnoRepository.incrementVtlnoEnrg();
                                    mvtlForInsert.setQte(mouvBC.getCdqte());
                                    mvtlForInsert.setRefqte(mouvBC.getCdqte());
                                    mvtlForInsert.setArtind("        "); // ✅ 8 espaces

                                    mvtlRepository.save(mvtlForInsert);
                                    mvtlRepository.flush();
                                    System.out.println("   ✅ Nouvelle ligne MVTL BC créée par clonage");
                                }

/// ///////////////////////////////////////////Code MVTL////////////////////////////////////////////////
                                // ✅ CORRECTION ICI : Exclure mvtlId lors de la copie pour BL


                                System.out.println("MVTL DETAIL xxxxxxxx : " + mvtlForUpdate);


                                // 1️⃣ Modifier l'existant pour BL
                                mvtlForUpdate.setCe4("1");
                                mvtlForUpdate.setCe1("V");
                                mvtlForUpdate.setArtind("        "); // ✅ 8 espaces
                                mvtlForUpdate.setCe3("");
                                mvtlForUpdate.setCea("1");
                                mvtlForUpdate.setPicod(BigDecimal.valueOf(3));
                                mvtlForUpdate.setEnrno(mouvForUpdate.getEnrno());
                                mvtlForUpdate.setUsermo(String.format("%-20s", currentUsername.toUpperCase()));
                                mvtlForUpdate.setUsermodh(LocalDate.now().atStartOfDay());
                                mvtlForUpdate.setBldt(mouvForUpdate.getBldt());
                                mvtlForUpdate.setCmp(mvtlForUpdate.getCr());
                                mvtlForUpdate.setPino(entBL.getPino()); // ✅ CORRECTION: Utiliser le PINO du BL au lieu du BC
                                mvtlForUpdate.setQte(mouvForUpdate.getBlqte());
                                mvtlForUpdate.setRefqte(mouvForUpdate.getBlqte());
                                mvtlForUpdate.setStqte(mouvForUpdate.getBlqte());
                                mvtlForUpdate.setStatus(BigDecimal.valueOf(2));

                                mvtlRepository.save(mvtlForUpdate);
                                mvtlRepository.flush();
                                System.out.println("   ✅ Ligne MVTL BL mise à jour");

                                // 5. Mise à jour du stock total de l'article
                                try {
                                    ART article = artRepository.findByRef(ligne.getArticle());
                                    if (article != null) {
                                        BigDecimal nouvelleQuantite = article.getSttotqte().add(ligne.getQte());
                                        article.setSttotqte(nouvelleQuantite);
                                        artRepository.saveAndFlush(article);
                                        log.info("✅ Stock total article mis à jour: {}", nouvelleQuantite);
                                    }
                                } catch (Exception e) {
                                    log.warn("⚠️ Erreur mise à jour stock total article: {}", e.getMessage());
                                }

                                ligne.setIntegre(1);
                                ligne.setBlDiva(entBL.getPino().intValue());
                                ligneReceptionRepository.save(ligne);

                                ligneNumber++;
                            }
                        }
                        Integer nbLignesBC= mouvRepository.countLigneBC(BigDecimal.valueOf(reception.getCommande()));
                        System.out.println("nbLignesBC : "+nbLignesBC);
                        if(nbLignesBC==0 || nbLignesBC == null){
                            System.out.println();
                            entRepository.updateEntBCPerime(BigDecimal.valueOf(reception.getCommande()));
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
                             List<KdnFile> fichiers, BigDecimal jointNumber,
                             BigDecimal pinoCommande) {

        System.out.println("🔄 Création sécurisée de l'entête BL...");

        try {
            // 1. Récupérer l'entête BC existant
            ENT enteteBC = entRepository.findByPinoAndPicod(pinoCommande, BigDecimal.valueOf(2));
            if (enteteBC == null) {
                throw new IllegalStateException("BC non trouvé avec PINO: " + pinoCommande);
            }

            // 2. Créer copie sécurisée
            ENT entBL = new ENT();
            BeanUtils.copyProperties(enteteBC, entBL);
            entBL.setEntId(null);

            // 3. ⭐ GÉNÉRER PINO SÉCURISÉ ⭐

            entBL.setPino(socPrefNoService.getNextPinoForBL());

            // 4. Modifier les champs qui changent
            entBL.setCe3("1");
            entBL.setPicod(BigDecimal.valueOf(3));
            entBL.setStatus(BigDecimal.valueOf(2));
            entBL.setPirelcod(BigDecimal.ONE);
            entBL.setPiref("N° BL Agileo: "+reception.getNumero());
            // 5. ⭐ SÉCURISER LES MONTANTS ⭐
            entBL.setHtmt(BigDecimal.ZERO);
            entBL.setTtcmt(BigDecimal.ZERO);
            entBL.setHtpdtmt(BigDecimal.ZERO);

            // 6. ⭐ SÉCURISER LES CHAMPS CALCULÉS ⭐
            entBL.setRefnb(BigDecimal.ZERO);
            entBL.setRempietot(BigDecimal.ZERO);

            // 7. Dates et utilisateur
            LocalDateTime now = LocalDateTime.now();
            entBL.setUsercrdh(now);
            entBL.setUsermodh(now);

            LocalDateTime localDateTime = reception.getSysModificationDate();
            String dateFormatted = localDateTime.toLocalDate().toString(); // Format yyyy-MM-dd
            entBL.setPidt(LocalDate.parse(dateFormatted));
            String paddedUsername = String.format("%-20s", currentUsername.toUpperCase());
            entBL.setUsercr(paddedUsername);
            entBL.setUsermo(paddedUsername);

            // 8. PINOTIERS sécurisé
            if (reception.getPinotiers() != null && !reception.getPinotiers().trim().isEmpty()) {
                String pinotiers = reception.getPinotiers().trim();
                if (pinotiers.length() > 20) pinotiers = pinotiers.substring(0, 20);
                entBL.setPinotiers(String.format("%-20s", pinotiers));
            } else {
                entBL.setPinotiers("                    ");
            }

            // 9. Pièces jointes sécurisées
            if (fichiers != null && !fichiers.isEmpty() && jointNumber != null) {
                // Vérifier que JOINT n'est pas trop grand
                if (jointNumber.compareTo(new BigDecimal("999999999")) <= 0) {
                    entBL.setCejoint(BigDecimal.valueOf(2));
                    entBL.setJoint(jointNumber);
                } else {
                    entBL.setCejoint(BigDecimal.ONE);
                    entBL.setJoint(BigDecimal.ZERO);
                    System.err.println("⚠️ JOINT trop grand, mis à 0 : " + jointNumber);
                }
            } else {
                entBL.setCejoint(BigDecimal.ONE);
            }

            System.out.println("✅ Entête BL sécurisé créé - PINO: " + entBL.getPino());
            return entBL;

        } catch (Exception e) {
            System.err.println("❌ ERREUR création entête BL: " + e.getMessage());
            throw new RuntimeException("Échec création entête BL", e);
        }
    }

// ========================================================================
// ACTION 3 : AJOUTER CETTE MÉTHODE SÉCURISÉE POUR LE PINO
// ========================================================================

    private BigDecimal getNextPinoSecurise() {
        try {
            String sql = "SELECT MAX(PINO) FROM ENT WHERE DOS='1'";
            Query query = divaltoEntityManager.createNativeQuery(sql);
            Object result = query.getSingleResult();

            BigDecimal maxPino = result != null ? new BigDecimal(result.toString()) : new BigDecimal("50000");
            BigDecimal nouveauPino = maxPino.add(BigDecimal.ONE);

            // ⭐ VALIDATION CRITIQUE : Vérifier la longueur ⭐
            String pinoStr = nouveauPino.toString();
            if (pinoStr.length() > 10) {  // Adapter selon votre contrainte DB
                throw new RuntimeException("PINO généré trop long: " + pinoStr + " (" + pinoStr.length() + " chiffres)");
            }

            System.out.println("🔢 PINO sécurisé généré: " + nouveauPino + " (longueur: " + pinoStr.length() + ")");
            return nouveauPino;

        } catch (Exception e) {
            System.err.println("❌ Erreur génération PINO sécurisé: " + e.getMessage());
            throw new RuntimeException("Impossible de générer un PINO valide", e);
        }
    }

    @Override
    public MOUV creerLigneMouv(LigneReception ligne, ENT entBL,
                               String currentUsername, int ligneNumber) {

        return null;
    }

    /**
     * Créer une pièce jointe dans MJOINT
     */
    @Override
    public MJoint creerPieceJointe(KdnFile fichier, ENT entBL,
                                   String currentUsername, BigDecimal jointNumber) {
        MJoint mJoint = new MJoint();
System.out.println("jointNumber : "+jointNumber);
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
        System.out.println("mJoint.getJoint() : "+mJoint.getJoint());
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
   /* private String extraireDepotDuCodeCommande(Integer commandeId) {
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
    }*/
    private void mettreAJourEntetesBC_BL(List<LigneReception> lignes, BigDecimal pinoCommande, ENT entBL) {
        // Récupérer l'entête BC
        ENT enteteBC = entRepository.findByPinoAndPicod(pinoCommande, BigDecimal.valueOf(2));

        if (enteteBC != null) {
            // Calculer montant réception
            BigDecimal montantReception = calculerMontantReceptionDepuisMouv(lignes);

            // ⭐ CALCULER LE TAUX TVA DEPUIS LE BC ⭐
            BigDecimal tauxTva = BigDecimal.ONE;
            if (enteteBC.getHtmt() != null && enteteBC.getHtmt().compareTo(BigDecimal.ZERO) != 0
                    && enteteBC.getTtcmt() != null) {
                tauxTva = enteteBC.getTtcmt().divide(enteteBC.getHtmt(), 4, BigDecimal.ROUND_HALF_UP);
            }

            // Mettre à jour BC (soustraire)
            BigDecimal Montant = enteteBC.getHtmt().subtract(montantReception);
            enteteBC.setTtcmt(Montant.multiply(tauxTva));
            enteteBC.setHtmt(Montant);
            enteteBC.setHtpdtmt(Montant);
            entRepository.save(enteteBC);

            // ⭐ METTRE À JOUR BL AVEC CALCULS CORRECTS ⭐
            entBL.setHtmt(montantReception.setScale(2, RoundingMode.HALF_UP));
            entBL.setHtpdtmt(montantReception.setScale(2, RoundingMode.HALF_UP));

            // ⭐ FIX 1: TTCMT avec TVA ⭐
            entBL.setTtcmt(montantReception.multiply(tauxTva).setScale(2, RoundingMode.HALF_UP));

            // ⭐ FIX 2: REFNB (nombre de références distinctes) ⭐
            long refnb = lignes.stream()
                    .map(LigneReception::getArticle)
                    .filter(article -> article != null && !article.trim().isEmpty())
                    .distinct()
                    .count();
            entBL.setRefnb(BigDecimal.valueOf(refnb).setScale(0, RoundingMode.DOWN));

            // ⭐ FIX 3: REMPIETOT (calculé proportionnellement du BC) ⭐
            BigDecimal rempietot = BigDecimal.ZERO;
            if (enteteBC.getHtmt() != null && enteteBC.getHtmt().compareTo(BigDecimal.ZERO) != 0
                    && enteteBC.getRempietot() != null) {
                // Calcul proportionnel : (montant_réception / montant_BC_original) * remise_BC_originale
                BigDecimal montantBCOriginal = enteteBC.getHtmt().add(montantReception); // BC avant soustraction
                BigDecimal ratio = montantReception.divide(montantBCOriginal, 4, BigDecimal.ROUND_HALF_UP);
                rempietot = enteteBC.getRempietot().multiply(ratio);
            }
            entBL.setRempietot(rempietot.setScale(2, BigDecimal.ROUND_HALF_UP));

            entRepository.save(entBL);

            System.out.println("✅ BC updated: " + enteteBC.getHtmt() + " | BL created: " + montantReception);
            System.out.println("✅ BL TTCMT: " + entBL.getTtcmt() + " (taux TVA: " + tauxTva + ")");
            System.out.println("✅ BL REFNB: " + entBL.getRefnb() + " | REMPIETOT: " + entBL.getRempietot());
        }
    }

    private BigDecimal calculerMontantReceptionDepuisMouv(List<LigneReception> lignes) {
        BigDecimal total = BigDecimal.ZERO;
        for (LigneReception ligne : lignes) {
            Optional<MOUV> mouvOpt = mouvRepository.findLigneCommandeByPinoAndRef(
                    BigDecimal.valueOf(ligne.getCommande()), ligne.getArticle());
            if (mouvOpt.isPresent()) {
                BigDecimal prix = mouvOpt.get().getPub();
                BigDecimal qte = ligne.getQte();
                total = total.add(prix.multiply(qte));
            }
        }
        return total;
    }
    private void mettreAJourChampsCalcules(ENT entBL, List<LigneReception> lignes) {
        // Calculer REFNB (nombre de références distinctes)
        long refnb = lignes.stream()
                .map(LigneReception::getArticle)
                .distinct()
                .count();
        entBL.setRefnb(BigDecimal.valueOf(refnb).setScale(0, RoundingMode.DOWN));

        // Calculer REMPIETOT selon votre logique métier
        BigDecimal rempietot = BigDecimal.ZERO;
        for (LigneReception ligne : lignes) {
            // Ajouter votre logique de calcul de remise
            // rempietot = rempietot.add(...);
        }
        entBL.setRempietot(rempietot);

        // Sauvegarder les modifications
        entRepository.save(entBL);

        System.out.println("✅ Champs calculés mis à jour - REFNB: " + refnb + ", REMPIETOT: " + rempietot);
    }
    private BigDecimal getNextPino() {
        try {
            String sql = "SELECT MAX(PINO) FROM ENT WHERE DOS='1'";
            Query query = divaltoEntityManager.createNativeQuery(sql);
            Object result = query.getSingleResult();

            if (result != null) {
                BigDecimal maxPino = new BigDecimal(result.toString());
                return maxPino.add(BigDecimal.ONE);
            } else {
                return new BigDecimal("50000"); // Valeur de départ
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du dernier PINO: {}", e.getMessage());
            return new BigDecimal("50000");
        }
    }

}