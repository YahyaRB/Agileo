package com.agileo.AGILEO.service.Impl;

import com.agileo.AGILEO.Dtos.request.*;
import com.agileo.AGILEO.Dtos.response.*;
import com.agileo.AGILEO.entity.primary.*;
import com.agileo.AGILEO.exception.BadRequestException;
import com.agileo.AGILEO.exception.ResourceNotFoundException;
import com.agileo.AGILEO.message.ResponseMessage;
import com.agileo.AGILEO.repository.primary.*;
import com.agileo.AGILEO.service.ConsommationService;
import com.agileo.AGILEO.service.FileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class ConsommationServiceImpl implements ConsommationService {

    private final ConsommationRepository consommationRepository;
    private final LigneConsommationRepository ligneConsommationRepository;
    private final AffaireRepository affaireRepository;
    private final KdnsAccessorRepository kdnsAccessorRepository;
    private final ArticleErpRepository articleErpRepository;
    private final LigneReceptionRepository ligneReceptionRepository;
    private final UserServiceImpl userService;
    private final FileService fileService;
    private final ArticleEnStockRepository articleEnStockRepository;

    public ConsommationServiceImpl(
            ConsommationRepository consommationRepository,
            LigneConsommationRepository ligneConsommationRepository,
            AffaireRepository affaireRepository,
            KdnsAccessorRepository kdnsAccessorRepository,
            ArticleErpRepository articleErpRepository,
            LigneReceptionRepository ligneReceptionRepository,
            UserServiceImpl userService,
            FileService fileService,
            ArticleEnStockRepository articleEnStockRepository) {
        this.consommationRepository = consommationRepository;
        this.ligneConsommationRepository = ligneConsommationRepository;
        this.affaireRepository = affaireRepository;
        this.kdnsAccessorRepository = kdnsAccessorRepository;
        this.articleErpRepository = articleErpRepository;
        this.ligneReceptionRepository = ligneReceptionRepository;
        this.userService = userService;
        this.fileService = fileService;
        this.articleEnStockRepository = articleEnStockRepository;
    }

    @Override
    public ConsommationResponseDTO createConsommation(ConsommationRequestDTO consommationDto, String currentUsername) {
        String affaireCode = consommationDto.getAffaireId();

        if (affaireCode == null || affaireCode.trim().isEmpty()) {
            throw new BadRequestException("Code d'affaire requis");
        }

        Affaire affaire = affaireRepository.findById(affaireCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Affaire introuvable: " + affaireCode));

        UserResponseDTO currentUser;
        try {
            currentUser = userService.findUserByLogin(currentUsername);
        } catch (Exception e) {
            throw new BadRequestException("Utilisateur introuvable: " + currentUsername);
        }

        Integer accessorId = null;
        if (currentUser.getIdAgelio() != null && !currentUser.getIdAgelio().trim().isEmpty()) {
            try {
                accessorId = Integer.parseInt(currentUser.getIdAgelio());
            } catch (NumberFormatException e) {
                throw new BadRequestException("ID Agileo invalide pour l'utilisateur: " + currentUsername);
            }
        }

        if (accessorId == null) {
            throw new BadRequestException("ID Agileo requis pour créer une consommation");
        }

        LocalDateTime now = LocalDateTime.now();

        Consommation consommation = new Consommation();
        consommation.setChantier(affaire.getAffaire());
        consommation.setDateC(consommationDto.getDateConsommation());
        consommation.setComm(consommationDto.getCommentaire());
        consommation.setRefInterne(consommationDto.getRefInterne());
        consommation.setLogin(accessorId);
        consommation.setStatut(0);

        consommation.setSysCreationDate(now);
        consommation.setSysCreatorId(accessorId);
        consommation.setSysModificationDate(now);
        consommation.setSysUserId(accessorId);
        consommation.setSysState(1);

        try {
            Consommation savedConsommation = consommationRepository.save(consommation);
            return mapToResponseDTO(savedConsommation);
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde de la consommation: " + e.getMessage());
            throw new BadRequestException("Erreur lors de la création de la consommation: " + e.getMessage());
        }
    }

    @Override
    public List<ConsommationResponseDTO> getAllConsommations() {
        List<Consommation> consommations = consommationRepository.findAll();
        return mapToResponseDTOs(consommations);
    }

    @Override
    public ConsommationResponseDTO getConsommationById(Integer id) {
        Consommation consommation = getConsommationEntityById(id);
        return mapToResponseDTO(consommation);
    }

    @Override
    public List<ConsommationResponseDTO> getConsommationsByAffaire(Integer affaireId) {
        String affaireCode = String.valueOf(affaireId);
        Affaire affaire = affaireRepository.findById(affaireCode)
                .orElseThrow(() -> new ResourceNotFoundException("Affaire introuvable: " + affaireCode));

        List<Consommation> consommations = consommationRepository.findByChantier(affaire.getAffaire());
        return mapToResponseDTOs(consommations);
    }

    @Override
    public List<ConsommationResponseDTO> getCurrentUserConsommations(String currentUsername) {
        try {
            UserResponseDTO user = userService.findUserByLogin(currentUsername);
            Integer accessorId = Integer.parseInt(user.getIdAgelio());
            List<Consommation> consommations = consommationRepository.findByLogin(accessorId);
            return mapToResponseDTOs(consommations);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<ConsommationResponseDTO> getConsommationsByUser(Integer userId) {
        List<Consommation> consommations = consommationRepository.findByLogin(userId);
        return mapToResponseDTOs(consommations);
    }

    @Override
    public ResponseMessage updateConsommation(Integer id, ConsommationRequestDTO consommationDto, String currentUsername) {
        Consommation consommation = getConsommationEntityById(id);

        if (consommation.isEnvoye()) {
            throw new BadRequestException("Une consommation envoyée ne peut pas être modifiée");
        }

        if (consommationDto.getAffaireId() != null && !consommationDto.getAffaireId().trim().isEmpty()) {
            String affaireCode = consommationDto.getAffaireId().trim();
            Affaire affaire = affaireRepository.findById(affaireCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Affaire introuvable: " + affaireCode));
            consommation.setChantier(affaire.getAffaire());
        }

        consommation.setDateC(consommationDto.getDateConsommation());
        consommation.setComm(consommationDto.getCommentaire());
        consommation.setRefInterne(consommationDto.getRefInterne());
        consommation.setSysModificationDate(LocalDateTime.now());

        consommationRepository.save(consommation);
        return new ResponseMessage("Consommation mise à jour avec succès");
    }

    @Override
    public List<ConsommationResponseDTO> getConsommationsByAffaireCode(String affaireCode) {
        Affaire affaire = affaireRepository.findById(affaireCode)
                .orElseThrow(() -> new ResourceNotFoundException("Affaire introuvable: " + affaireCode));

        List<Consommation> consommations = consommationRepository.findByChantier(affaire.getAffaire());
        return mapToResponseDTOs(consommations);
    }

    // ==================== MÉTHODES SIMPLIFIÉES POUR LES ARTICLES ====================
    @Override
    public List<ArticleStockDTO> getArticlesDisponiblesForAffaire(String affaireCode) {
        System.out.println("🚀 Début chargement articles pour affaire: " + affaireCode);

        try {
            // Vérifier que l'affaire existe
            Affaire affaire = affaireRepository.findById(affaireCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Affaire introuvable: " + affaireCode));

            String depot = extraireDepotDuCodeAffaire(affaireCode);
            System.out.println("📍 Dépôt extrait: " + depot);

            // Récupérer les articles en stock
            List<ArticleEnStock> articlesEnStock = articleEnStockRepository.findByDepotWithStockNative(depot);

            if (articlesEnStock.isEmpty()) {
                articlesEnStock = articleEnStockRepository.findByDepo(depot);
            }

            System.out.println("📦 Articles en stock: " + articlesEnStock.size());

            // ⭐⭐⭐ CALCUL EN UNE SEULE FOIS - BROUILLON + ENVOYÉ ⭐⭐⭐
            Map<String, Double> quantitesReservees = getQuantitesReserveesParDepot(depot);

            // Convertir en DTO
            List<ArticleStockDTO> result = articlesEnStock.stream()
                    .filter(article -> article.getSumStQte() != null && article.getSumStQte().doubleValue() > 0)
                    .map(article -> {
                        String refArticle = article.getRef().trim();
                        Double stockPhysique = article.getSumStQte().doubleValue();

                        // ⭐ Stock Disponible = Stock Vue - (Brouillon + Envoyé)
                        Double quantiteReservee = quantitesReservees.getOrDefault(refArticle, 0.0);
                        Double stockDisponible = stockPhysique - quantiteReservee;

                        System.out.println("📊 " + refArticle +
                                " | Stock: " + stockPhysique +
                                " | Réservé: " + quantiteReservee +
                                " | Disponible: " + Math.max(0, stockDisponible));

                        return new ArticleStockDTO(
                                refArticle,
                                article.getDescription() != null ? article.getDescription().trim() : "Désignation non disponible",
                                article.getAchun() != null ? article.getAchun().trim() : "PCS",
                                Math.max(0, stockDisponible), // Stock disponible réel
                                stockPhysique,                // Stock physique total
                                quantiteReservee              // Quantité réservée (brouillon + envoyée)
                        );
                    })
                    .filter(dto -> dto.getStockDisponible() > 0)
                    .sorted(Comparator.comparing(ArticleStockDTO::getReferenceArticle))
                    .collect(Collectors.toList());

            System.out.println("✅ Articles disponibles: " + result.size());
            return result;

        } catch (Exception e) {
            System.err.println("❌ ERREUR récupération articles: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    @Override
    public ResponseMessage envoyerConsommation(Integer id, String currentUsername) {
        Consommation consommation = getConsommationEntityById(id);

        if (consommation.isEnvoye()) {
            throw new BadRequestException("Cette consommation est déjà envoyée");
        }

        long nbLignes = ligneConsommationRepository.countByNumCons(id);
        if (nbLignes == 0) {
            throw new BadRequestException("Impossible d'envoyer une consommation vide");
        }

        consommation.marquerCommeEnvoye();
        consommation.setSysModificationDate(LocalDateTime.now());
        consommationRepository.save(consommation);

        return new ResponseMessage("Consommation envoyée avec succès");
    }

    @Override
    public ResponseMessage deleteConsommation(Integer id) {
        Consommation consommation = getConsommationEntityById(id);

        if (consommation.isEnvoye()) {
            throw new BadRequestException("Une consommation envoyée ne peut pas être supprimée");
        }

        ligneConsommationRepository.deleteByNumCons(id);
        consommationRepository.delete(consommation);

        return new ResponseMessage("Consommation supprimée avec succès");
    }

    @Override
    public List<ArticleStockDTO> getArticlesDisponibles(Integer affaireId) {
        String affaireCode = String.valueOf(affaireId);
        return getArticlesDisponiblesForAffaire(affaireCode);
    }

    // ==================== GESTION DES LIGNES - SIMPLIFIÉE ====================

    @Override
    public ResponseMessage addLignesConsommation(Integer consommationId, List<LigneConsommationRequestDTO> lignesDto, String currentUsername) {
        Consommation consommation = getConsommationEntityById(consommationId);

        if (consommation.isEnvoye()) {
            throw new BadRequestException("Impossible d'ajouter des lignes à une consommation envoyée");
        }

        UserResponseDTO currentUser;
        try {
            currentUser = userService.findUserByLogin(currentUsername);
            Integer accessorId = Integer.parseInt(currentUser.getIdAgelio());

            for (LigneConsommationRequestDTO ligneDto : lignesDto) {
                // VALIDATIONS OBLIGATOIRES
                if (ligneDto.getReferenceArticle() == null || ligneDto.getReferenceArticle().trim().isEmpty()) {
                    throw new BadRequestException("Référence article obligatoire");
                }

                if (ligneDto.getQuantite() == null || ligneDto.getQuantite() <= 0) {
                    throw new BadRequestException("Quantité doit être positive");
                }

                if (accessorId == null) {
                    throw new BadRequestException("Impossible de déterminer l'utilisateur pour la ligne");
                }

                // VALIDATION SIMPLIFIÉE : Vérifier seulement le stock physique
                Double stockDisponible = getStockDisponible(ligneDto.getReferenceArticle(), consommation.getChantier());

                if (stockDisponible < ligneDto.getQuantite()) {
                    throw new BadRequestException("Stock insuffisant pour l'article " + ligneDto.getReferenceArticle().trim() +
                            ". Stock disponible: " + stockDisponible + ", Quantité demandée: " + ligneDto.getQuantite());
                }

                try {
                    LigneConsommation ligne = createLigneFromDTO(ligneDto, consommation, accessorId);
                    LigneConsommation savedLigne = ligneConsommationRepository.save(ligne);

                } catch (Exception e) {
                    System.err.println("Erreur sauvegarde ligne pour article " + ligneDto.getReferenceArticle() + ": " + e.getMessage());
                    throw new BadRequestException("Erreur lors de la sauvegarde de la ligne pour l'article " + ligneDto.getReferenceArticle() + ": " + e.getMessage());
                }
            }

            return new ResponseMessage("Lignes ajoutées avec succès");

        } catch (BadRequestException e) {
            throw e; // Re-lancer les BadRequestException
        } catch (Exception e) {
            System.err.println("Erreur générale ajout lignes: " + e.getMessage());
            e.printStackTrace();
            throw new BadRequestException("Erreur lors de l'ajout des lignes: " + e.getMessage());
        }
    }

    @Override
    public List<LigneConsommationResponseDTO> getLignesConsommationByConsommationId(Integer consommationId) {
        getConsommationEntityById(consommationId);
        List<LigneConsommation> lignes = ligneConsommationRepository.findByNumCons(consommationId);
        return lignes.stream()
                .map(this::mapLigneToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ResponseMessage updateLigneConsommation(Integer ligneId, LigneConsommationRequestDTO ligneDto, String currentUsername) {
        LigneConsommation ligne = ligneConsommationRepository.findById(ligneId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable: " + ligneId));

        Consommation consommation = getConsommationEntityById(ligne.getNumCons());
        if (consommation.isEnvoye()) {
            throw new BadRequestException("Impossible de modifier une ligne d'une consommation envoyée");
        }

        // Vérifier le stock (validation simplifiée)
        Double stockDisponible = getStockDisponible(ligneDto.getReferenceArticle(), consommation.getChantier());

        if (stockDisponible < ligneDto.getQuantite()) {
            throw new BadRequestException("Stock insuffisant pour l'article " + ligneDto.getReferenceArticle().trim());
        }

        updateLigneFromDTO(ligne, ligneDto);
        ligne.setSysModificationDate(LocalDateTime.now());
        ligneConsommationRepository.save(ligne);

        return new ResponseMessage("Ligne mise à jour avec succès");
    }

    @Override
    public ResponseMessage deleteLigneConsommation(Integer ligneId, String currentUsername) {
        LigneConsommation ligne = ligneConsommationRepository.findById(ligneId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable: " + ligneId));

        Consommation consommation = getConsommationEntityById(ligne.getNumCons());
        if (consommation.isEnvoye()) {
            throw new BadRequestException("Impossible de supprimer une ligne d'une consommation envoyée");
        }

        ligneConsommationRepository.delete(ligne);
        return new ResponseMessage("Ligne supprimée avec succès");
    }

    // ==================== MÉTHODES PRIVÉES ====================

    private Consommation getConsommationEntityById(Integer id) {
        return consommationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consommation introuvable: " + id));
    }

    // SIMPLIFIÉE : Utilise uniquement SumStQte
    private Double getStockDisponible(String referenceArticle, String affaire) {
        try {
            String depot = extraireDepotDuCodeAffaire(affaire);

            // Chercher l'article dans le stock
            List<ArticleEnStock> articles = articleEnStockRepository.findByDepo(depot);

            Optional<ArticleEnStock> articleOpt = articles.stream()
                    .filter(a -> referenceArticle.trim().equals(a.getRef().trim()))
                    .findFirst();

            if (articleOpt.isPresent()) {
                BigDecimal stockPhysique = articleOpt.get().getSumStQte();
                Double stockPhysiqueDouble = stockPhysique != null ? stockPhysique.doubleValue() : 0.0;

                // ⭐ Stock Disponible = Stock Vue - (Brouillon + Envoyé)
                Map<String, Double> quantitesReservees = getQuantitesReserveesParDepot(depot);
                Double quantiteReservee = quantitesReservees.getOrDefault(referenceArticle.trim(), 0.0);

                Double stockDisponibleReel = stockPhysiqueDouble - quantiteReservee;

                return Math.max(0, stockDisponibleReel);
            }

            return 0.0;
        } catch (Exception e) {
            System.err.println("Erreur calcul stock pour " + referenceArticle + ": " + e.getMessage());
            return 0.0;
        }
    }
    /**
     * Calcule TOUTES les quantités en brouillon ET envoyées pour un dépôt EN UNE SEULE FOIS
     * Retourne une Map : référenceArticle -> quantité réservée (brouillon + envoyée)
     */
    private Map<String, Double> getQuantitesReserveesParDepot(String depot) {
        try {
            System.out.println("🔍 Calcul des quantités réservées (brouillon + envoyées) pour le dépôt: " + depot);

            // 1️⃣ Récupérer toutes les consommations en brouillon (statut = 0) ET envoyées (statut = 1)
            List<Consommation> consommationsBrouillon = consommationRepository.findByStatut(0);
            List<Consommation> consommationsEnvoyees = consommationRepository.findByStatut(1);

            // Combiner les deux listes
            List<Consommation> toutesConsommations = new ArrayList<>();
            toutesConsommations.addAll(consommationsBrouillon);
            toutesConsommations.addAll(consommationsEnvoyees);

            System.out.println("📦 Consommations brouillon: " + consommationsBrouillon.size());
            System.out.println("📤 Consommations envoyées: " + consommationsEnvoyees.size());

            if (toutesConsommations.isEmpty()) {
                System.out.println("✅ Aucune consommation non synchronisée");
                return Collections.emptyMap();
            }

            // 2️⃣ Filtrer par dépôt
            List<Integer> idsConsommationsDepot = toutesConsommations.stream()
                    .filter(c -> c.getChantier() != null)
                    .filter(c -> {
                        try {
                            String depotConsommation = extraireDepotDuCodeAffaireSafe(c.getChantier());
                            return depot.equals(depotConsommation);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .map(Consommation::getIdBc)
                    .collect(Collectors.toList());

            if (idsConsommationsDepot.isEmpty()) {
                System.out.println("✅ Aucune consommation réservée pour ce dépôt");
                return Collections.emptyMap();
            }

            System.out.println("🎯 Consommations réservées pour ce dépôt: " + idsConsommationsDepot.size());

            // 3️⃣ Récupérer TOUTES les lignes (UNE SEULE requête)
            List<LigneConsommation> lignesReservees = ligneConsommationRepository.findByNumConsIn(idsConsommationsDepot);

            System.out.println("📝 Lignes réservées trouvées: " + lignesReservees.size());

            // 4️⃣ Grouper par référence article et sommer les quantités
            Map<String, Double> quantitesParArticle = lignesReservees.stream()
                    .collect(Collectors.groupingBy(
                            ligne -> ligne.getRef().trim(),
                            Collectors.summingDouble(LigneConsommation::getQte)
                    ));

            System.out.println("✅ Articles avec quantités réservées: " + quantitesParArticle.size());
            quantitesParArticle.forEach((ref, qte) ->
                    System.out.println("   📌 " + ref + " : " + qte + " réservée(s)")
            );

            return quantitesParArticle;

        } catch (Exception e) {
            System.err.println("❌ ERREUR calcul quantités réservées: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    /**
     * Version SAFE de l'extraction du dépôt - ne lance JAMAIS d'exception
     */
    private String extraireDepotDuCodeAffaireSafe(String codeAffaire) {
        try {
            if (codeAffaire == null || codeAffaire.trim().isEmpty()) {
                return null;
            }

            // Méthode simple : prendre les 3 derniers chiffres
            String chiffres = codeAffaire.replaceAll("[^0-9]", "");

            if (chiffres.length() >= 3) {
                return chiffres.substring(chiffres.length() - 3);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }
    private LigneConsommation createLigneFromDTO(LigneConsommationRequestDTO dto, Consommation consommation, Integer userId) {
        LocalDateTime now = LocalDateTime.now();

        LigneConsommation ligne = new LigneConsommation();

        // Champs métier
        ligne.setRef(dto.getReferenceArticle());
        ligne.setDes(dto.getDesignationArticle());
        ligne.setQte(dto.getQuantite());
        ligne.setUnite(dto.getUnite() != null ? dto.getUnite() : "PCS"); // Valeur par défaut si null
        ligne.setSref1(dto.getFamilleStatistique1());
        ligne.setSref2(dto.getFamilleStatistique2());
        ligne.setNumCons(consommation.getIdBc());

        // Champs système OBLIGATOIRES (not null dans la base)
        ligne.setSysCreationDate(now);
        ligne.setSysCreatorId(userId);
        ligne.setSysModificationDate(now); // IMPORTANT: Champ obligatoire souvent oublié
        ligne.setSysUserId(userId);
        ligne.setSysState(1);

        // Champ optionnel
        ligne.setSysSynchronizationDate(null); // Peut être null
        return ligne;
    }

    private void updateLigneFromDTO(LigneConsommation ligne, LigneConsommationRequestDTO dto) {
        ligne.setRef(dto.getReferenceArticle());
        ligne.setDes(dto.getDesignationArticle());
        ligne.setQte(dto.getQuantite());
        ligne.setUnite(dto.getUnite());
        ligne.setSref1(dto.getFamilleStatistique1());
        ligne.setSref2(dto.getFamilleStatistique2());
    }

    private ConsommationResponseDTO mapToResponseDTO(Consommation consommation) {
        return mapToResponseDTOs(Collections.singletonList(consommation)).get(0);
    }


    private List<ConsommationResponseDTO> mapToResponseDTOs(List<Consommation> consommations) {
        if (consommations.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> affaireCodes = consommations.stream()
                .map(Consommation::getChantier)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> userIds = consommations.stream()
                .flatMap(c -> Stream.of(c.getLogin(), c.getSysCreatorId(), c.getSysUserId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, Affaire> affaires = affaireRepository.findAllById(affaireCodes)
                .stream()
                .collect(Collectors.toMap(Affaire::getAffaire, a -> a));

        Map<Integer, KdnsAccessor> users = kdnsAccessorRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(KdnsAccessor::getAccessorId, u -> u));

        return consommations.stream()
                .map(consommation -> {
                    ConsommationResponseDTO dto = new ConsommationResponseDTO();
                    dto.setId(consommation.getIdBc());
                    dto.setDateConsommation(consommation.getDateC());
                    dto.setCommentaire(consommation.getComm());
                    dto.setRefInterne(consommation.getRefInterne());
                    dto.setUserId(consommation.getLogin());
                    dto.setStatut(getStatutLabel(consommation.getStatut()));
                    dto.setCreatedDate(consommation.getSysCreationDate());

                    if (consommation.getChantier() != null) {
                        Affaire affaire = affaires.get(consommation.getChantier());
                        if (affaire != null) {
                            dto.setAffaireCode(affaire.getAffaire());
                            dto.setAffaireLibelle(affaire.getLibelle());

                            try {
                                String numericPart = affaire.getAffaire().replaceAll("[^0-9]", "");
                                if (!numericPart.isEmpty()) {
                                    dto.setAffaireId(Integer.valueOf(numericPart));
                                }
                            } catch (NumberFormatException e) {
                                dto.setAffaireId(null);
                            }
                        }
                    }

                    if (consommation.getSysCreatorId() != null) {
                        KdnsAccessor creator = users.get(consommation.getSysCreatorId());
                        if (creator != null) {
                            dto.setUserLogin(creator.getLogin());
                            dto.setCreatedBy(creator.getLogin());
                            dto.setCreateurNom(formatUserName(creator));
                        }
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    private LigneConsommationResponseDTO mapLigneToResponseDTO(LigneConsommation ligne) {
        LigneConsommationResponseDTO dto = new LigneConsommationResponseDTO();
        dto.setId(ligne.getIdLigne());
        dto.setReferenceArticle(ligne.getRef());
        dto.setDesignationArticle(ligne.getDes());
        dto.setQuantite(ligne.getQte());
        dto.setUnite(ligne.getUnite());
        dto.setFamilleStatistique1(ligne.getSref1());
        dto.setFamilleStatistique2(ligne.getSref2());
        dto.setSysCreationDate(ligne.getSysCreationDate());
        dto.setSysModificationDate(ligne.getSysModificationDate());

        return dto;
    }

    private String getStatutLabel(Integer statut) {
        if (statut == null || statut == 0) return "Brouillon";
        if (statut == 1) return "Envoyé";
        if (statut == 2) return "Reçu";
        return "Inconnu";
    }

    private String formatUserName(KdnsAccessor accessor) {
        if (accessor.getFullName() != null && !accessor.getFullName().trim().isEmpty()) {
            return accessor.getFullName();
        }
        String firstName = accessor.getFirstName() != null ? accessor.getFirstName() : "";
        String lastName = accessor.getLastName() != null ? accessor.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }

    // Méthode d'extraction du dépôt depuis le code affaire
    private String extraireDepotDuCodeAffaire(String codeAffaire) {


        if (codeAffaire == null || codeAffaire.trim().isEmpty()) {
            throw new BadRequestException("Code affaire invalide: " + codeAffaire);
        }

        try {
            // Méthode 1: Pour les codes qui commencent par "CH"
            if (codeAffaire.startsWith("CH")) {
                // Enlever "CH" et garder les chiffres
                String chiffres = codeAffaire.substring(2); // Exemple: CH003721 -> 003721


                if (chiffres.length() >= 3) {
                    // Prendre les 3 derniers chiffres
                    String depot = chiffres.substring(chiffres.length() - 3); // 003721 -> 721

                    // Vérifier si ce dépôt existe dans la base
                    try {
                        Long count = articleEnStockRepository.countByDepo(depot);
                        if (count > 0) {
                            return depot;
                        } else {
                        }
                    } catch (Exception e) {
                        System.out.println("Erreur vérification dépôt " + depot + ": " + e.getMessage());
                    }
                }
            }

            // Méthode 2: Extraction générale - tous les chiffres
            String tousLesChiffres = codeAffaire.replaceAll("[^0-9]", "");
            System.out.println("Tous les chiffres du code: " + tousLesChiffres);

            if (tousLesChiffres.length() >= 3) {
                String depot = tousLesChiffres.substring(tousLesChiffres.length() - 3);

                try {
                    Long count = articleEnStockRepository.countByDepo(depot);
                    if (count > 0) {
                        return depot;
                    }
                } catch (Exception e) {
                    System.out.println("Erreur vérification dépôt alternatif " + depot + ": " + e.getMessage());
                }
            }

            // Méthode 3: Essayer d'autres positions dans les chiffres
            if (tousLesChiffres.length() >= 6) {
                // Essayer les 3 chiffres du milieu
                int start = (tousLesChiffres.length() - 3) / 2;
                String depotMilieu = tousLesChiffres.substring(start, start + 3);


                try {
                    Long count = articleEnStockRepository.countByDepo(depotMilieu);
                    if (count > 0) {

                        return depotMilieu;
                    }
                } catch (Exception e) {
                    System.out.println("Erreur vérification dépôt milieu " + depotMilieu + ": " + e.getMessage());
                }

                // Essayer les 3 premiers chiffres
                String depotDebut = tousLesChiffres.substring(0, 3);


                try {
                    Long count = articleEnStockRepository.countByDepo(depotDebut);
                    if (count > 0) {
                        return depotDebut;
                    }
                } catch (Exception e) {
                    System.out.println("Erreur vérification dépôt début " + depotDebut + ": " + e.getMessage());
                }
            }
            try {
                List<String> depotsDisponibles = articleEnStockRepository.findAllDepots();
                System.out.println("Dépôts disponibles dans la base: " + depotsDisponibles);

                for (String depot : depotsDisponibles) {
                    try {
                        Long count = articleEnStockRepository.countByDepo(depot);
                        if (count > 0) {
                            return depot;
                        }
                    } catch (Exception e) {
                        System.out.println("Erreur test dépôt " + depot + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println("Erreur récupération des dépôts: " + e.getMessage());
            }

            throw new BadRequestException("Aucun dépôt contenant des articles trouvé pour le code affaire: " + codeAffaire);

        } catch (BadRequestException e) {
            throw e; // Re-lancer les BadRequestException
        } catch (Exception e) {
            System.err.println("Erreur lors de l'extraction du dépôt: " + e.getMessage());

            // Dernier recours: essayer de trouver n'importe quel dépôt avec des articles
            try {
                List<String> depotsDisponibles = articleEnStockRepository.findAllDepots();
                if (!depotsDisponibles.isEmpty()) {
                    String premierDepot = depotsDisponibles.get(0);
                    return premierDepot;
                }
            } catch (Exception ex) {
                System.err.println("Erreur fallback d'urgence: " + ex.getMessage());
            }

            throw new BadRequestException("Impossible de déterminer un dépôt pour le code affaire: " + codeAffaire);
        }
    }
    /**
     * Calcule la quantité consommée en brouillon pour un article dans un dépôt
     * VERSION OPTIMISÉE avec gestion d'erreurs robuste
     */
    private Double getQuantiteEnBrouillon(String referenceArticle, String depot) {
        try {
            System.out.println("🔍 Calcul quantité brouillon pour article: " + referenceArticle + " dans dépôt: " + depot);

            // Récupérer toutes les consommations en brouillon (statut = 0)
            List<Consommation> consommationsBrouillon = consommationRepository.findByStatut(0);
            System.out.println("📦 Nombre total de consommations en brouillon: " + consommationsBrouillon.size());

            if (consommationsBrouillon.isEmpty()) {
                System.out.println("✅ Aucune consommation en brouillon trouvée");
                return 0.0;
            }

            // Filtrer par dépôt de manière sécurisée
            List<Integer> idsConsommationsDepot = new ArrayList<>();

            for (Consommation c : consommationsBrouillon) {
                try {
                    String chantier = c.getChantier();
                    if (chantier == null || chantier.trim().isEmpty()) {
                        continue; // Ignorer les consommations sans chantier
                    }

                    // Extraire le dépôt du chantier de manière sécurisée
                    String depotConsommation = extraireDepotDuCodeAffaireSafe(chantier);

                    if (depotConsommation != null && depot.equals(depotConsommation)) {
                        idsConsommationsDepot.add(c.getIdBc());
                    }
                } catch (Exception e) {
                    // Ignorer silencieusement les erreurs d'extraction de dépôt pour une consommation
                    System.out.println("⚠️ Erreur extraction dépôt pour consommation " + c.getIdBc() + ": " + e.getMessage());
                }
            }

            System.out.println("🎯 Nombre de consommations brouillon dans ce dépôt: " + idsConsommationsDepot.size());

            if (idsConsommationsDepot.isEmpty()) {
                System.out.println("✅ Aucune consommation en brouillon pour ce dépôt");
                return 0.0;
            }

            // Calculer la somme des quantités pour cet article
            List<LigneConsommation> lignesBrouillon = ligneConsommationRepository
                    .findByNumConsInAndRef(idsConsommationsDepot, referenceArticle.trim());

            Double totalBrouillon = lignesBrouillon.stream()
                    .mapToDouble(LigneConsommation::getQte)
                    .sum();

            System.out.println("💰 Quantité totale en brouillon pour " + referenceArticle + ": " + totalBrouillon);

            return totalBrouillon;

        } catch (Exception e) {
            // NE PAS lancer d'exception, retourner 0.0 par sécurité
            System.err.println("❌ ERREUR calcul quantité brouillon pour " + referenceArticle + ": " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }

    /**
     * Version SAFE de l'extraction du dépôt - ne lance JAMAIS d'exception
     */

    private Map<String, Double> getQuantitesEnBrouillonParDepot(String depot) {
        try {
            System.out.println("🔍 Calcul des quantités en brouillon pour le dépôt: " + depot);

            // 1️⃣ Récupérer TOUTES les consommations en brouillon (UNE SEULE requête)
            List<Consommation> consommationsBrouillon = consommationRepository.findByStatut(0);

            if (consommationsBrouillon.isEmpty()) {
                System.out.println("✅ Aucune consommation en brouillon");
                return Collections.emptyMap();
            }

            // 2️⃣ Filtrer par dépôt
            List<Integer> idsConsommationsDepot = consommationsBrouillon.stream()
                    .filter(c -> c.getChantier() != null)
                    .filter(c -> {
                        try {
                            String depotConsommation = extraireDepotDuCodeAffaireSafe(c.getChantier());
                            return depot.equals(depotConsommation);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .map(Consommation::getIdBc)
                    .collect(Collectors.toList());

            if (idsConsommationsDepot.isEmpty()) {
                System.out.println("✅ Aucune consommation en brouillon pour ce dépôt");
                return Collections.emptyMap();
            }

            System.out.println("📦 Consommations brouillon trouvées: " + idsConsommationsDepot.size());

            // 3️⃣ Récupérer TOUTES les lignes en brouillon (UNE SEULE requête)
            List<LigneConsommation> lignesBrouillon = ligneConsommationRepository.findByNumConsIn(idsConsommationsDepot);

            System.out.println("📝 Lignes en brouillon trouvées: " + lignesBrouillon.size());

            // 4️⃣ Grouper par référence article et sommer les quantités
            Map<String, Double> quantitesParArticle = lignesBrouillon.stream()
                    .collect(Collectors.groupingBy(
                            ligne -> ligne.getRef().trim(),
                            Collectors.summingDouble(LigneConsommation::getQte)
                    ));

            System.out.println("✅ Articles avec quantités en brouillon: " + quantitesParArticle.size());
            quantitesParArticle.forEach((ref, qte) ->
                    System.out.println("   📌 " + ref + " : " + qte)
            );

            return quantitesParArticle;

        } catch (Exception e) {
            System.err.println("❌ ERREUR calcul quantités brouillon: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }
}