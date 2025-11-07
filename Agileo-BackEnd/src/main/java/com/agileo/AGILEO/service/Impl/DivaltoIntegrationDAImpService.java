package com.agileo.AGILEO.service.Impl;

import com.agileo.AGILEO.entity.divalto.DaoEnt;
import com.agileo.AGILEO.entity.divalto.DaoLig;
import com.agileo.AGILEO.entity.divalto.MJoint;
import com.agileo.AGILEO.entity.primary.DemandeAchat;
import com.agileo.AGILEO.entity.primary.KdnFile;
import com.agileo.AGILEO.entity.primary.LigneDemandeAchat;
import com.agileo.AGILEO.exception.ResourceNotFoundException;

import com.agileo.AGILEO.repository.divalto.DaoEntRepository;
import com.agileo.AGILEO.repository.divalto.DaoLigRepository;
import com.agileo.AGILEO.repository.divalto.MJointRepository;
import com.agileo.AGILEO.repository.primary.DemandeAchatRepository;
import com.agileo.AGILEO.repository.primary.KdnFileRepository;
import com.agileo.AGILEO.repository.primary.LigneDemandeAchatRepository;
import com.agileo.AGILEO.service.DivaltoIntegrationDAService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class DivaltoIntegrationDAImpService implements DivaltoIntegrationDAService {

    @Autowired
    private DemandeAchatRepository demandeAchatRepository;

    @Autowired
    private LigneDemandeAchatRepository ligneDemandeRepository;

    @Autowired
    private KdnFileRepository kdnFileRepository;

    @Autowired
    private DaoEntRepository daoEntRepository;

    @Autowired
    private DaoLigRepository daoLigRepository;

    @Autowired
    private MJointRepository mJointRepository;

    @Autowired
    @Qualifier("divaltoTransactionManager")
    private PlatformTransactionManager divaltoTransactionManager;

    @PersistenceContext(unitName = "divalto")
    private EntityManager divaltoEntityManager;

    @Value("${app.path.globalVariable:C:\\\\Agileo\\\\Extension\\\\Storage\\\\public}")
    private String baseFtpPath;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Point d'entrée principal : Enregistrer une demande d'achat dans Divalto
     */
    @Override
    public void integrerDemandeAchatDansDivalto(Integer demandeId, String currentUsername) {
        System.out.println("=== DÉBUT INTÉGRATION DIVALTO ===");
        System.out.println("Demande ID: " + demandeId);
        System.out.println("Username: " + currentUsername);

        try {
            // 1. Récupérer la demande
            DemandeAchat demande = demandeAchatRepository.findById(demandeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée: " + demandeId));

            System.out.println("✅ Demande trouvée : " + demande.getNumDa());
            System.out.println("   - Chantier: " + demande.getChantier());
            System.out.println("   - Login: " + demande.getLogin());
            System.out.println("   - Statut: " + demande.getStatut());

            // 2. Récupérer les lignes
            List<LigneDemandeAchat> lignes = ligneDemandeRepository.findByDa(demandeId);
            System.out.println("✅ Nombre de lignes trouvées : " + lignes.size());

            if (lignes.isEmpty()) {
                System.err.println("❌ ERREUR : Aucune ligne trouvée !");
                throw new IllegalStateException("La demande n'a aucune ligne d'article");
            }

            for (LigneDemandeAchat ligne : lignes) {
                System.out.println("   Ligne - REF: " + ligne.getRef() + ", QTE: " + ligne.getQte());
            }

            // 3. Récupérer les fichiers
            List<KdnFile> fichiers = null;
            if (demande.getPjDa() != null) {
                fichiers = kdnFileRepository.findActiveFilesByGroupId(demande.getPjDa());
                System.out.println("✅ Nombre de fichiers : " + (fichiers != null ? fichiers.size() : 0));
            } else {
                System.out.println("⚠️ Aucun groupe de fichiers (PjDa est null)");
            }

            // 4. Transaction Divalto
            TransactionTemplate transactionTemplate = new TransactionTemplate(divaltoTransactionManager);
            final List<KdnFile> finalFichiers = fichiers;

            System.out.println("🔄 Démarrage de la transaction Divalto...");

            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    try {
                        System.out.println("   🔍 Création de l'entête DAO...");

                        // ✅ Générer le numéro JOINT une seule fois pour toutes les pièces jointes
                        BigDecimal jointNumber = null;
                        if (finalFichiers != null && !finalFichiers.isEmpty()) {
                            jointNumber = getNextJoint();
                            System.out.println("   📎 Numéro JOINT généré : " + jointNumber);
                        }

                        // Créer l'entête DAO avec le numéro JOINT et CEJOINT
                        DaoEnt daoEnt = creerEnteteDao(demande, currentUsername, finalFichiers, jointNumber);
                        System.out.println("   ✅ Entête créé en mémoire - DAONO prévu: " + daoEnt.getDaoNo());

                        daoEnt = daoEntRepository.save(daoEnt);
                        System.out.println("   ✅ Entête DAO SAUVEGARDÉ - ID: " + daoEnt.getDaoEntId() + ", DAONO: " + daoEnt.getDaoNo());

                        // Créer les lignes DAO avec LILG incrémenté
                        System.out.println("   🔍 Création des lignes DAO...");
                        int ligneNumber = 1;
                        for (LigneDemandeAchat ligne : lignes) {
                            DaoLig daoLig = creerLigneDaoLig(ligne, daoEnt, currentUsername, ligneNumber);
                            daoLig = daoLigRepository.save(daoLig);
                            System.out.println("   ✅ Ligne DAO SAUVEGARDÉE - LILG: " + ligneNumber + ", REF: " + daoLig.getRef());
                            ligneNumber++;
                        }

                        // Créer les pièces jointes avec le MÊME numéro JOINT
                        if (finalFichiers != null && !finalFichiers.isEmpty()) {
                            System.out.println("   🔍 Création des pièces jointes...");
                            for (KdnFile fichier : finalFichiers) {
                                MJoint mJoint = creerPieceJointe(fichier, daoEnt, currentUsername, jointNumber);
                                mJoint = mJointRepository.save(mJoint);
                                System.out.println("   ✅ Pièce jointe SAUVEGARDÉE - JOINT: " + jointNumber + ", NOM: " + mJoint.getLib80());
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

            System.out.println("=== FIN INTÉGRATION DIVALTO - SUCCÈS ===");

        } catch (Exception e) {
            System.err.println("❌ ERREUR GLOBALE lors de l'intégration Divalto: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur Divalto: " + e.getMessage(), e);
        }
    }

    /**
     * Créer l'entête DAO pour Divalto
     */
    @Override
    public DaoEnt creerEnteteDao(DemandeAchat demande, String currentUsername, List<KdnFile> fichiers, BigDecimal jointNumber) {
        DaoEnt daoEnt = new DaoEnt();

        // Génération du DAONO
        BigDecimal nextDaoNo = getNextDaoNo();
        daoEnt.setDaoNo(nextDaoNo);

        // ✅ CORRECTION : DAOREF doit être une chaîne vide, jamais null
        String daoRef = "";
        if (demande.getNumDa() != null && !demande.getNumDa().trim().isEmpty()) {
            daoRef = demande.getNumDa().trim();
        }
        daoEnt.setDaoRef(daoRef);
        daoEnt.setDaoRefExt("");

        // Référence et projet
        daoEnt.setProjet(demande.getChantier() != null ? demande.getChantier() : "");

        // Codes et identifiants
        daoEnt.setAdrcod("");
        daoEnt.setAdrtiers("");
        daoEnt.setCatcod("");
        daoEnt.setServcod("");
        daoEnt.setConf("");
        daoEnt.setEtb("   ");
        daoEnt.setAfrindice("");

        daoEnt.setDos("1");
        daoEnt.setDepo("1");

        // Champs CE
        daoEnt.setCe1("1");
        daoEnt.setCe2("1");
        daoEnt.setCe3(" ");
        daoEnt.setCe4(" ");
        daoEnt.setCe5(" ");
        daoEnt.setCe6(" ");
        daoEnt.setCe7(" ");
        daoEnt.setCe8(" ");
        daoEnt.setCe9(" ");
        daoEnt.setCea(" ");

        // Champs numériques
        daoEnt.setDaoTyp(BigDecimal.ONE);
        daoEnt.setDaoOrig(BigDecimal.ONE);
        daoEnt.setTxtcodd(BigDecimal.ONE);
        daoEnt.setTxtcodf(BigDecimal.ONE);
        daoEnt.setTxtnoted(BigDecimal.ZERO);
        daoEnt.setTxtnotef(BigDecimal.ZERO);
        daoEnt.setCenote(BigDecimal.ONE);
        daoEnt.setNote(BigDecimal.ZERO);

        // ✅ CORRECTION : CEJOINT = 2 si pièces jointes, sinon 1
        if (fichiers != null && !fichiers.isEmpty()) {
            daoEnt.setCejoint(BigDecimal.valueOf(2));
            daoEnt.setJoint(jointNumber);
        } else {
            daoEnt.setCejoint(BigDecimal.ONE);
            daoEnt.setJoint(BigDecimal.ZERO);
        }

        daoEnt.setElemno(BigDecimal.ZERO);

        // Dates
        daoEnt.setDaodt(LocalDate.now());

        if (demande.getDelaiSouhaite() != null) {
            daoEnt.setDeldemdt(demande.getDelaiSouhaite().toLocalDate());
        } else {
            daoEnt.setDeldemdt(LocalDate.now());
        }

        daoEnt.setTransmisdt(null);

        if (demande.getDelaiSouhaite() != null) {
            daoEnt.setDelrepsdt(demande.getDelaiSouhaite().toLocalDate().plusDays(4));
        }

        daoEnt.setUsercrdh(LocalDateTime.now());
        daoEnt.setUsermodh(null);

        // Utilisateurs
        String paddedUsername = String.format("%-20s", currentUsername.toUpperCase());
        daoEnt.setUsercr(paddedUsername);
        daoEnt.setUserdao(paddedUsername);
        daoEnt.setSalcod(paddedUsername);
        daoEnt.setUsermo("");

        daoEnt.setStatus(BigDecimal.valueOf(1));

        if (demande.getId() != null) {
            daoEnt.setUpIdAgileo(BigDecimal.valueOf(demande.getId()));
        }
        daoEnt.setUpIdWeavy(BigDecimal.ZERO);

        return daoEnt;
    }

    /**
     * Créer une ligne DAO dans Divalto (DAOLIG)
     */
    @Override
    public DaoLig creerLigneDaoLig(LigneDemandeAchat ligne, DaoEnt daoEnt, String currentUsername, int ligneNumber) {
        DaoLig daoLig = new DaoLig();

        daoLig.setDaoNo(daoEnt.getDaoNo());
        daoLig.setProjet(daoEnt.getProjet());

        // ✅ Générer le DAOLGNO
        BigDecimal nextDaoLgNo = getNextDaoLgNo();
        daoLig.setDaolgno(nextDaoLgNo);

        daoLig.setDos("1");
        daoLig.setEtb("   ");

        // ✅ CORRECTION : DEPO = les 3 derniers chiffres du chantier
        String depot = "1";
        if (daoEnt.getProjet() != null && !daoEnt.getProjet().isEmpty()) {
            String chantier = daoEnt.getProjet().trim();
            String numeros = chantier.replaceAll("[^0-9]", "");
            if (numeros.length() >= 3) {
                depot = numeros.substring(numeros.length() - 3);
            } else if (!numeros.isEmpty()) {
                depot = numeros;
            }
        }
        daoLig.setDepo(depot);

        daoLig.setAfrindice("");
        daoLig.setArtind("");
        daoLig.setUpMateriel("");

        // Champs CE
        daoLig.setCe1("2");
        daoLig.setCe2(" ");
        daoLig.setCe3("1");
        daoLig.setCe4(" ");
        daoLig.setCe5(" ");
        daoLig.setCe6(" ");
        daoLig.setCe7(" ");
        daoLig.setCe8(" ");
        daoLig.setCe9(" ");
        daoLig.setCea(" ");

        daoLig.setDaoTyp(BigDecimal.ONE);

        // ✅ CORRECTION : LILG incrémenté (1, 2, 3...)
        daoLig.setLilg(BigDecimal.valueOf(ligneNumber));

        daoLig.setDaolgnoao(BigDecimal.ZERO);
        daoLig.setTxtcod(BigDecimal.ONE);
        daoLig.setTxtnote(BigDecimal.ZERO);
        daoLig.setCenote(BigDecimal.ONE);
        daoLig.setNote(BigDecimal.ZERO);
        daoLig.setCejoint(BigDecimal.ONE);
        daoLig.setJoint(BigDecimal.ZERO);
        daoLig.setElemno(BigDecimal.ZERO);
        daoLig.setBesoinno(BigDecimal.ZERO);

        // Article
        daoLig.setRef(ligne.getRef() != null ? ligne.getRef() : "");
        daoLig.setSref1(ligne.getSref1() != null ? ligne.getSref1() : "");
        daoLig.setSref2(ligne.getSref2() != null ? ligne.getSref2() : "");
        daoLig.setDes(ligne.getDesignation() != null ? ligne.getDesignation() : "");
        daoLig.setQteini(ligne.getQte() != null ? ligne.getQte() : BigDecimal.ZERO);
        daoLig.setRefqte(ligne.getQte() != null ? ligne.getQte() : BigDecimal.ZERO);
        daoLig.setAchun(ligne.getUnite() != null ? ligne.getUnite() : "");
        daoLig.setRefun(ligne.getUnite() != null ? ligne.getUnite() : "");

        // Date de livraison
        if (daoEnt.getDeldemdt() != null) {
            daoLig.setDeldemdt(daoEnt.getDeldemdt());
        } else {
            daoLig.setDeldemdt(LocalDate.now());
        }

        daoLig.setUsercrdh(LocalDateTime.now());
        daoLig.setUsermodh(LocalDateTime.now());

        String paddedUsername = String.format("%-20s", currentUsername.toUpperCase());
        daoLig.setUsercr(paddedUsername);
        daoLig.setUsermo(paddedUsername);

        daoLig.setStatus(BigDecimal.valueOf(2));

        return daoLig;
    }

    /**
     * Créer une pièce jointe dans MJOINT
     */
    @Override
    public MJoint creerPieceJointe(KdnFile fichier, DaoEnt daoEnt, String currentUsername, BigDecimal jointNumber) {
        MJoint mJoint = new MJoint();

        // ✅ Utiliser le numéro JOINT passé en paramètre
        mJoint.setJoint(jointNumber);

        mJoint.setApplic("DAV");
        mJoint.setJointobj("DAOENT");

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
        mJoint.setNaturejointcod("DAO_F_T");
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
     * Convertir LocalDateTime en entier YYYYMMDD
     */
    public Integer convertirDateVersEntier(LocalDateTime date) {
        if (date == null) {
            date = LocalDateTime.now();
        }
        String dateStr = date.format(DATE_FORMATTER);
        return Integer.parseInt(dateStr);
    }

    /**
     * Tronquer une chaîne à la longueur maximale
     */
    public String truncateString(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    /**
     * Obtenir le prochain DAONO
     */
    private BigDecimal getNextDaoNo() {
        try {
            String sql = "SELECT MAX(DAONO) FROM DAOENT";
            Query query = divaltoEntityManager.createNativeQuery(sql);
            Object result = query.getSingleResult();

            if (result != null) {
                BigDecimal maxDaoNo = new BigDecimal(result.toString());
                return maxDaoNo.add(BigDecimal.ONE);
            } else {
                return new BigDecimal("20000");
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du dernier DAONO: {}", e.getMessage());
            return new BigDecimal("20000");
        }
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
     * Obtenir le prochain DAOLGNO
     */
    private BigDecimal getNextDaoLgNo() {
        try {
            String sql = "SELECT MAX(DAOLGNO) FROM DAOLIG";
            Query query = divaltoEntityManager.createNativeQuery(sql);
            Object result = query.getSingleResult();

            if (result != null) {
                BigDecimal maxDaoLgNo = new BigDecimal(result.toString());
                return maxDaoLgNo.add(BigDecimal.ONE);
            } else {
                return new BigDecimal("60000");
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du dernier DAOLGNO: {}", e.getMessage());
            return new BigDecimal("60000");
        }
    }
}