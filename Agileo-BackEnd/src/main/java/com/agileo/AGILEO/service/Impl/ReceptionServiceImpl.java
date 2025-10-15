package com.agileo.AGILEO.service.Impl;

import com.agileo.AGILEO.Dtos.request.*;
import com.agileo.AGILEO.Dtos.response.*;
import com.agileo.AGILEO.entity.primary.KdnFile;
import com.agileo.AGILEO.entity.primary.KdnFileGroup;
import com.agileo.AGILEO.repository.primary.KdnFileRepository;
import com.agileo.AGILEO.repository.primary.KdnFileGroupRepository;
import com.agileo.AGILEO.entity.primary.*;
import com.agileo.AGILEO.exception.BadRequestException;
import com.agileo.AGILEO.exception.ResourceNotFoundException;
import com.agileo.AGILEO.message.ResponseMessage;
import com.agileo.AGILEO.repository.primary.*;
import com.agileo.AGILEO.service.ReceptionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class ReceptionServiceImpl implements ReceptionService {

    private final ReceptionRepository receptionRepository;
    private final LigneReceptionRepository ligneReceptionRepository;
    private final AffaireRepository affaireRepository;
    private final KdnsAccessorRepository kdnsAccessorRepository;
    private final KdnFileRepository kdnFileRepository;
    private final ArticleReceptionRepository articleReceptionRepository;
    private final UserServiceImpl userService;
    private final CommandeRepository commandeRepository;
    private final VentilationArticleRepository ventilationArticleRepository;
    private final KdnFileGroupRepository kdnFileGroupRepository;
    public ReceptionServiceImpl(
            ReceptionRepository receptionRepository,
            LigneReceptionRepository ligneReceptionRepository,
            AffaireRepository affaireRepository,
            KdnsAccessorRepository kdnsAccessorRepository,
            ArticleReceptionRepository articleReceptionRepository,
            UserServiceImpl userService,
            CommandeRepository commandeRepository,
            VentilationArticleRepository ventilationArticleRepository,
            KdnFileRepository kdnFileRepository,
            KdnFileGroupRepository kdnFileGroupRepository) {

        this.receptionRepository = receptionRepository;
        this.ligneReceptionRepository = ligneReceptionRepository;
        this.affaireRepository = affaireRepository;
        this.kdnsAccessorRepository = kdnsAccessorRepository;
        this.articleReceptionRepository = articleReceptionRepository;
        this.userService = userService;
        this.commandeRepository = commandeRepository;
        this.ventilationArticleRepository = ventilationArticleRepository;
        this.kdnFileRepository = kdnFileRepository;
        this.kdnFileGroupRepository = kdnFileGroupRepository; // CORRECTION: Cette ligne était manquante !
    }
    // ==================== CRÉATION ET GESTION DES RÉCEPTIONS ====================

    @Override
    public ReceptionResponseDTO createReception(ReceptionRequestDTO receptionDto, String currentUsername) {
        try {
            // Validation de l'affaire
            Affaire affaire = null;
            // Essayer d'abord de récupérer par ID
            try {
                Integer affaireId = Integer.parseInt(String.valueOf(receptionDto.getAffaireId()));
                affaire = affaireRepository.findById(String.valueOf(affaireId)).orElse(null);
            } catch (NumberFormatException e) {
                System.out.println("AffaireId n'est pas un nombre, recherche par code: " + receptionDto.getAffaireId());
            }
            // Si pas trouvé par ID, chercher par code d'affaire
            if (affaire == null) {
                String affaireCode = String.valueOf(receptionDto.getAffaireId());
                Optional<Affaire> affaireOptional = affaireRepository.findByAffaire(affaireCode);
                if (affaireOptional.isPresent()) {
                    affaire = affaireOptional.get();
                    System.out.println("Affaire trouvée par code: " + affaire.getAffaire() + " - " + affaire.getLibelle());
                } else {
                    throw new ResourceNotFoundException("Affaire introuvable: " + receptionDto.getAffaireId());
                }
            }
            // Récupération de l'utilisateur courant
            UserResponseDTO currentUser;
            try {
                currentUser = userService.findUserByLogin(currentUsername);

            } catch (Exception e) {
                throw new BadRequestException("Utilisateur introuvable: " + currentUsername);
            }
            // Gestion sécurisée de l'accessorId
            Integer accessorId = null;
            if (currentUser.getIdAgelio() != null && !currentUser.getIdAgelio().trim().isEmpty()) {
                try {
                    accessorId = Integer.parseInt(currentUser.getIdAgelio());
                } catch (NumberFormatException e) {
                    System.out.println("ID Agileo invalide pour l'utilisateur: " + currentUsername);
                }
            }

            Reception reception = new Reception();
            // Gestion du commandeCode
            Integer commandeValue = null;
            if (receptionDto.getCommandeCode() != null) {
                try {
                    if (receptionDto.getCommandeCode() instanceof Integer) {
                        commandeValue = (Integer) receptionDto.getCommandeCode();
                    } else {
                        String commandeStr = String.valueOf(receptionDto.getCommandeCode()).trim();
                        if (!commandeStr.isEmpty() && !commandeStr.equals("null")) {
                            commandeValue = Integer.parseInt(commandeStr);
                        }
                    }
                } catch (NumberFormatException e) {
                    throw new BadRequestException("Le code de commande doit être numérique: " + receptionDto.getCommandeCode());
                }
            }
            // Si commandeCode n'est pas fourni, utiliser le code d'affaire
            if (commandeValue == null) {
                try {
                    commandeValue = Integer.parseInt(affaire.getAffaire());
                } catch (NumberFormatException e) {
                    throw new BadRequestException("Le code d'affaire doit être numérique: " + affaire.getAffaire());
                }
            }
            reception.setCommande(commandeValue);
            reception.setPjBc(null);
//            reception.setPinotiers(String.valueOf(receptionDto.getIdAgelio()));
            reception.setPinotiers(receptionDto.getIdAgelio());
            reception.setSysCreationDate(receptionDto.getDateReception());
            reception.setSysModificationDate(receptionDto.getDateBl());
            reception.setSysCreatorId(accessorId);
            reception.setSysUserId(accessorId);
            reception.setSysState(1);
            // Gestion du statut
            int statutValue = 0;
            if (receptionDto.getStatut() != null && receptionDto.getStatut().equals("Envoyé")) {
                statutValue = 1;
            }

            reception.setSysState(statutValue);
            Reception savedReception = receptionRepository.save(reception);
            return mapToResponseDTO(savedReception);
        } catch (BadRequestException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Erreur lors de la création de la réception: " + e.getMessage());
            e.printStackTrace();
            throw new BadRequestException("Erreur interne lors de la création de la réception: " + e.getMessage());
        }
    }

    @Override
    public List<ReceptionResponseDTO> getAllReceptions() {
        List<Reception> receptions = receptionRepository.findAll();
        return mapToResponseDTOs(receptions);
    }
    //Liste des receptions avec pagination
    @Override
    public PagedResponse<ReceptionResponseDTO> getAllReceptionsPaginated(
            int page, int size, String sortBy, String sortDirection, String search) {

        if (page < 0) page = 0;
        if (size <= 0 || size > 20) size = 20;
        if (sortBy == null || sortBy.isEmpty()) sortBy = "numero";

        // ✅ AJOUT : Mapper les noms de colonnes frontend vers les propriétés de l'entité
        sortBy = mapSortFieldToEntityProperty(sortBy);

        Sort.Direction direction = sortDirection != null && sortDirection.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Reception> receptionsPage;

        if (search != null && !search.trim().isEmpty()) {
            receptionsPage = receptionRepository.searchReceptions(search.trim(), pageable);
        } else {
            receptionsPage = receptionRepository.findAll(pageable);
        }

        List<ReceptionResponseDTO> receptionsDtos = mapToResponseDTOs(receptionsPage.getContent());

        PagedResponse<ReceptionResponseDTO> response = new PagedResponse<>();
        response.setContent(receptionsDtos);
        response.setPageNumber(receptionsPage.getNumber());
        response.setPageSize(receptionsPage.getSize());
        response.setTotalElements(receptionsPage.getTotalElements());
        response.setTotalPages(receptionsPage.getTotalPages());
        response.setFirst(receptionsPage.isFirst());
        response.setLast(receptionsPage.isLast());
        response.setHasNext(receptionsPage.hasNext());
        response.setHasPrevious(receptionsPage.hasPrevious());

        return response;
    }


    @Override
    public ReceptionResponseDTO getReceptionById(Integer id) {
        Reception reception = getReceptionEntityById(id);
        return mapToResponseDTO(reception);
    }

    @Override
    public List<ReceptionResponseDTO> getReceptionsByAffaire(Integer affaireId) {
        List<Reception> receptions = receptionRepository.findByCommande(affaireId);
        return mapToResponseDTOs(receptions);
    }

    @Override
    public List<ReceptionResponseDTO> getCurrentUserReceptions(String currentUsername) {
        try {
            UserResponseDTO user = userService.findUserByLogin(currentUsername);
            if (user.getIdAgelio() != null && !user.getIdAgelio().trim().isEmpty()) {
                Integer accessorId = Integer.parseInt(user.getIdAgelio());
                List<Reception> receptions = receptionRepository.findBySysCreatorId(accessorId);
                return mapToResponseDTOs(receptions);
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    @Override
    public PagedResponse<ReceptionResponseDTO> getCurrentUserReceptionsPaginated(
            int page, int size, String sortBy, String sortDirection, String currentUsername, String search) {
        try {
            UserResponseDTO user = userService.findUserByLogin(currentUsername);
            String idAgelio = user.getIdAgelio();

            if (idAgelio == null || idAgelio.trim().isEmpty()) {
                return buildEmptyPagedResponse(page, size);
            }

            Integer accessorId = Integer.parseInt(idAgelio);

            if (page < 0) page = 0;
            if (size <= 0 || size > 20) size = 20;
            if (sortBy == null || sortBy.isEmpty()) sortBy = "numero";

            // ✅ AJOUT : Mapper les noms de colonnes
            sortBy = mapSortFieldToEntityProperty(sortBy);

            Sort.Direction direction = (sortDirection != null && sortDirection.equalsIgnoreCase("desc"))
                    ? Sort.Direction.DESC : Sort.Direction.ASC;

            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<Reception> receptionsPage;

            if (search != null && !search.trim().isEmpty()) {
                receptionsPage = receptionRepository.searchReceptionsByCreator(accessorId, search.trim(), pageable);
            } else {
                receptionsPage = receptionRepository.findBySysCreatorId(accessorId, pageable);
            }

            List<ReceptionResponseDTO> receptionsDtos = mapToResponseDTOs(receptionsPage.getContent());

            PagedResponse<ReceptionResponseDTO> response = new PagedResponse<>();
            response.setContent(receptionsDtos);
            response.setPageNumber(receptionsPage.getNumber());
            response.setPageSize(receptionsPage.getSize());
            response.setTotalElements(receptionsPage.getTotalElements());
            response.setTotalPages(receptionsPage.getTotalPages());
            response.setFirst(receptionsPage.isFirst());
            response.setLast(receptionsPage.isLast());
            response.setHasNext(receptionsPage.hasNext());
            response.setHasPrevious(receptionsPage.hasPrevious());

            return response;

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des réceptions: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des réceptions", e);
        }
    }

    @Override
    public ResponseMessage updateReception(Integer id, ReceptionRequestDTO receptionDto, String currentUsername) {
        Reception reception = getReceptionEntityById(id);

        if (reception.getSysState() != null && reception.getSysState() == 1) {
            throw new BadRequestException("Une réception envoyée ne peut pas être modifiée");
        }

        if (receptionDto.getCommandeCode() != null) {
            try {
                Integer commandeValue;
                if (receptionDto.getCommandeCode() instanceof Integer) {
                    commandeValue = (Integer) receptionDto.getCommandeCode();
                } else {
                    commandeValue = Integer.parseInt(String.valueOf(receptionDto.getCommandeCode()));
                }
                reception.setCommande(commandeValue);
            } catch (NumberFormatException e) {
                throw new BadRequestException("Le code de commande doit être numérique: " + receptionDto.getCommandeCode());
            }
        }

        
        reception.setPinotiers(receptionDto.getReferenceBl());
        reception.setSysModificationDate(receptionDto.getDateBl());
        reception.setSysCreationDate(receptionDto.getDateReception());
        if (receptionDto.getStatut() != null && receptionDto.getStatut().equals("Envoyé")) {
            reception.setSysState(1);
        }

        receptionRepository.save(reception);
        return new ResponseMessage("Réception mise à jour avec succès");
    }

    @Override
    public ResponseMessage deleteReception(Integer id) {
        Reception reception = getReceptionEntityById(id);

        if (reception.getSysState() != null && reception.getSysState() == 1) {
            throw new BadRequestException("Une réception envoyée ne peut pas être supprimée");
        }

        // Supprimer d'abord les lignes
        Integer commandeId = reception.getCommande();
        if (commandeId != null) {
            ligneReceptionRepository.deleteByCommande(commandeId);
        }

        // Supprimer la réception
        receptionRepository.delete(reception);
        return new ResponseMessage("Réception supprimée avec succès");
    }

    // ==================== GESTION DES ARTICLES DISPONIBLES (DEPUIS ArticleReception) ====================
    @Override
    public List<ArticleDisponibleDTO> getArticlesDisponibles(Integer receptionId) {
        try {
            Reception reception = getReceptionEntityById(receptionId);
            Integer commandeNumber = reception.getCommande();
            if (commandeNumber == null) {
                return Collections.emptyList();
            }

            // Récupérer les articles depuis ArticleReception (Livraison_ERP)
            List<ArticleReception> articlesReception = articleReceptionRepository
                    .findArticleReceptionsByCommande(commandeNumber.longValue());

            // ✅ CORRECTION: Récupérer TOUTES les lignes de la commande (toutes réceptions confondues)
            // pour calculer la quantité totale déjà reçue
            List<LigneReception> toutesLesLignes = ligneReceptionRepository.findByCommande(commandeNumber);

            // ✅ Calculer les quantités RÉELLEMENT reçues depuis LigneReception
            Map<String, BigDecimal> quantitesDejaRecues = toutesLesLignes.stream()
                    .filter(ligne -> ligne.getArticle() != null && ligne.getQte() != null)
                    .collect(Collectors.groupingBy(
                            LigneReception::getArticle,
                            Collectors.reducing(BigDecimal.ZERO, LigneReception::getQte, BigDecimal::add)
                    ));

            List<ArticleDisponibleDTO> articlesDisponibles = new ArrayList<>();

            for (ArticleReception articleReception : articlesReception) {
                ArticleDisponibleDTO dto = new ArticleDisponibleDTO();

                // Informations de base
                dto.setReference(articleReception.getArticleId());
                dto.setDesignation(articleReception.getDesignation());
                dto.setUnite(articleReception.getUnite());

                // Quantités
                BigDecimal qteCommandee = articleReception.getQteCommandee() != null ?
                        articleReception.getQteCommandee() : BigDecimal.ZERO;
                BigDecimal qteRest = articleReception.getQteRest() != null ?
                        articleReception.getQteRest() : BigDecimal.ZERO;

                // ✅ CORRECTION PRINCIPALE: Utiliser la valeur CALCULÉE depuis LigneReception
                BigDecimal qteDejaRecue = quantitesDejaRecues.getOrDefault(
                        articleReception.getArticleId(),
                        BigDecimal.ZERO
                );

                dto.setQuantiteCommandee(qteCommandee);
                dto.setQuantiteDejaRecue(qteDejaRecue);  // ✅ Maintenant utilise la bonne valeur !
                dto.setQuantiteDisponible(qteRest);

                // ✅ FILTRE: N'afficher QUE les articles avec qteRest > 0
                if (qteRest.compareTo(BigDecimal.ZERO) > 0) {
                    articlesDisponibles.add(dto);
                }
            }

            return articlesDisponibles;

        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des articles disponibles: " + e.getMessage());
            e.printStackTrace();
            throw new BadRequestException("Erreur lors de la récupération des articles: " + e.getMessage());
        }
    }
    // ==================== GESTION DES LIGNES DE RÉCEPTION ====================

    @Override

    public ResponseMessage addLignesReception(Integer receptionId, List<LigneReceptionRequestDTO> lignesDto,
                                              String currentUsername) {
        VentilationArticle ventilation;
        try {
            Reception reception = getReceptionEntityById(receptionId);
            if (reception.getSysState() != null && reception.getSysState() == 1) {
                throw new BadRequestException("Impossible d'ajouter des lignes à une réception envoyée");
            }

            // Récupérer l'utilisateur
            Integer accessorId = null;
            try {
                UserResponseDTO currentUser = userService.findUserByLogin(currentUsername);
                if (currentUser.getIdAgelio() != null && !currentUser.getIdAgelio().trim().isEmpty()) {
                    accessorId = Integer.parseInt(currentUser.getIdAgelio());
                }
            } catch (Exception e) {
                System.out.println("Impossible de récupérer l'ID utilisateur: " + e.getMessage());
            }

            // Vérifier que le numéro de commande n'est pas null
            if (reception.getCommande() == null) {
                throw new BadRequestException("Numéro de commande manquant pour la réception " + receptionId);
            }

            // Récupérer les articles depuis ArticleReception
            List<ArticleReception> articlesReception;
            try {
                articlesReception = articleReceptionRepository
                        .findArticleReceptionsByCommande(reception.getCommande().longValue());
            } catch (Exception e) {
                System.err.println("Erreur lors de la récupération des articles de commande " + reception.getCommande() + ": " + e.getMessage());
                throw new BadRequestException("Erreur lors de la récupération des articles de la commande");
            }

            if (articlesReception.isEmpty()) {
                throw new BadRequestException("Aucun article trouvé pour la commande " + reception.getCommande());
            }

            Map<String, ArticleReception> articlesMap = articlesReception.stream()
                    .collect(Collectors.toMap(ArticleReception::getArticleId, a -> a));

            // Validation des données d'entrée
            if (lignesDto == null || lignesDto.isEmpty()) {
                throw new BadRequestException("Aucune ligne à ajouter");
            }

            for (LigneReceptionRequestDTO ligneDto : lignesDto) {
                // Validation complète des données
                if (ligneDto.getReferenceArticle() == null || ligneDto.getReferenceArticle().trim().isEmpty()) {
                    throw new BadRequestException("Référence article manquante");
                }
                if (ligneDto.getQuantite() == null || ligneDto.getQuantite().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BadRequestException("Quantité invalide pour l'article " + ligneDto.getReferenceArticle());
                }

                // Vérifier que l'article existe dans le bon de commande
                ArticleReception articleReception = articlesMap.get(ligneDto.getReferenceArticle());
                if (articleReception == null) {
                    throw new BadRequestException("Article " + ligneDto.getReferenceArticle() +
                            " non trouvé dans le bon de commande " + reception.getCommande());
                }

                // Validation de la quantité
                ValidationQuantiteDTO validation;
                try {
                    validation = validerQuantiteArticle(
                            receptionId, ligneDto.getReferenceArticle(), ligneDto.getQuantite(), null);
                } catch (Exception e) {
                    System.err.println("Erreur lors de la validation de quantité: " + e.getMessage());
                    throw new BadRequestException("Erreur lors de la validation de la quantité pour l'article " + ligneDto.getReferenceArticle());
                }
                if (!validation.isValide()) {
                    throw new BadRequestException("Article " + ligneDto.getReferenceArticle() + ": " + validation.getMessage());
                }

                // Vérifier si l'article existe déjà dans cette réception spécifique
                try {
                    List<LigneReception> lignesExistantes = ligneReceptionRepository.findByEntId(reception.getNumero());
                    boolean existe = lignesExistantes.stream()
                            .anyMatch(l -> l.getArticle() != null && l.getArticle().equals(ligneDto.getReferenceArticle()));
                    if (existe) {
                        throw new BadRequestException("L'article " + ligneDto.getReferenceArticle() +
                                " existe déjà dans cette réception");
                    }
                } catch (BadRequestException e) {
                    throw e;
                } catch (Exception e) {
                    System.err.println("Erreur lors de la vérification d'existence: " + e.getMessage());
                }

                // Création de la ligne
                try {
                    LigneReception ligne = new LigneReception();
                    ventilation = new VentilationArticle();
                    Commande commande = commandeRepository.findByCommande(Long.valueOf(reception.getCommande()));
                    String depot = extraireDepotDuCodeAffaire(commande.getAffaireCode());
                    ventilation=ventilationArticleRepository.findByReferenceAndDepot(ligneDto.getReferenceArticle(),depot);
                    ligne.setEntId(reception.getNumero());
                    ligne.setCommande(reception.getCommande());
                    ligne.setArticle(ligneDto.getReferenceArticle());
                    // Information ENRNO
                    ligne.setEnrno(ventilation.getEnrno());
                    // Information VTLNO
                    ligne.setVtlno(ventilation.getVtlno());

                    // Désignation
                    String designation = articleReception.getDesignation();
                    if (ligneDto.getDesignationArticle() != null && !ligneDto.getDesignationArticle().trim().isEmpty()) {
                        designation = ligneDto.getDesignationArticle();
                    }
                    ligne.setDeseignation(designation);
                    ligne.setQte(ligneDto.getQuantite());
                    ligne.setQteCmd(articleReception.getQteCommandee() != null ? articleReception.getQteCommandee() : BigDecimal.ZERO);

                    BigDecimal qteLivree = articleReception.getQteLivree() != null ? articleReception.getQteLivree() : BigDecimal.ZERO;
                    BigDecimal qteRest = articleReception.getQteRest() != null ? articleReception.getQteRest() : BigDecimal.ZERO;

                    ligne.setQteLivre(qteLivree.add(ligneDto.getQuantite()));
                    ligne.setReste(qteRest.subtract(ligneDto.getQuantite()));

                    // Autres infos
                    String unite = articleReception.getUnite();
                    if (ligneDto.getUnite() != null && !ligneDto.getUnite().trim().isEmpty()) {
                        unite = ligneDto.getUnite();
                    }
                    ligne.setUnite(unite);
                    ligne.setAffaire(articleReception.getAffaireCode());
                    ligne.setTiers(null);

                    // Métadonnées
                    ligne.setIntegre(2);
                    ligne.setSysCreationDate(LocalDateTime.now());
                    ligne.setSysModificationDate(LocalDateTime.now());
                    ligne.setSysCreatorId(accessorId);
                    ligne.setSysUserId(accessorId);
                    ligne.setPldt(reception.getSysModificationDate());
                    ligne.setSysState(1);

                    // Sauvegarde
                    ligneReceptionRepository.save(ligne);
                    System.out.println("Ligne ajoutée avec succès pour l'article: " + ligneDto.getReferenceArticle());

                } catch (Exception e) {
                    System.err.println("Erreur lors de la création de la ligne pour l'article " +
                            ligneDto.getReferenceArticle() + ": " + e.getMessage());
                    e.printStackTrace();
                    throw new BadRequestException("Erreur lors de la création de la ligne pour l'article " +
                            ligneDto.getReferenceArticle() + ": " + e.getMessage());
                }
            }

            return new ResponseMessage("Lignes ajoutées avec succès");

        } catch (BadRequestException | ResourceNotFoundException e) {
            System.err.println("Erreur métier lors de l'ajout des lignes: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Erreur technique lors de l'ajout des lignes de réception: " + e.getMessage());
            e.printStackTrace();
            throw new BadRequestException("Erreur interne lors de l'ajout des lignes: " + e.getMessage());
        }
    }

    @Override
    public ValidationQuantiteDTO validerQuantiteArticle(Integer receptionId, String referenceArticle,
                                                        BigDecimal quantiteDemandee, Integer ligneReceptionId) {
        try {
            Reception reception = getReceptionEntityById(receptionId);
            // Vérifier le statut de la réception
            if (reception.getSysState() != null && reception.getSysState() == 1) {
                return new ValidationQuantiteDTO(false, "Réception déjà envoyée", BigDecimal.ZERO, BigDecimal.ZERO);
            }
            // Vérification du numéro de commande
            if (reception.getCommande() == null) {
                return new ValidationQuantiteDTO(false, "Numéro de commande manquant", BigDecimal.ZERO, quantiteDemandee);
            }
            // Récupérer l'article depuis ArticleReception
            List<ArticleReception> articlesReception;
            try {
                articlesReception = articleReceptionRepository
                        .findArticleReceptionsByCommande(reception.getCommande().longValue());
            } catch (Exception e) {
                System.err.println("Erreur lors de la récupération des articles: " + e.getMessage());
                return new ValidationQuantiteDTO(false, "Erreur lors de la vérification de l'article",
                        BigDecimal.ZERO, quantiteDemandee);
            }
            ArticleReception articleReception = articlesReception.stream()
                    .filter(a -> a.getArticleId() != null && a.getArticleId().equals(referenceArticle))
                    .findFirst()
                    .orElse(null);
            if (articleReception == null) {
                return new ValidationQuantiteDTO(false, "Article non trouvé dans le bon de commande",
                        BigDecimal.ZERO, quantiteDemandee);
            }
            // Calculer les quantités
            BigDecimal qteCommandee = articleReception.getQteCommandee() != null ?
                    articleReception.getQteCommandee() : BigDecimal.ZERO;
            // Gestion sécurisée de la récupération des lignes existantes
            BigDecimal qteDejaRecue = BigDecimal.ZERO;
            try {
                List<LigneReception> lignesExistantes = ligneReceptionRepository.findByEntId(reception.getNumero());
                qteDejaRecue = lignesExistantes.stream()
                        .filter(l -> l.getArticle() != null && l.getArticle().equals(referenceArticle))
                        .filter(l -> ligneReceptionId == null || !l.getNumero().equals(ligneReceptionId))
                        .map(l -> l.getQte() != null ? l.getQte() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } catch (Exception e) {
                System.err.println("Erreur lors du calcul des quantités déjà reçues: " + e.getMessage());
                // Continuer avec qteDejaRecue = 0
            }
            BigDecimal qteDisponible = qteCommandee.subtract(qteDejaRecue);
            if (qteDisponible.compareTo(BigDecimal.ZERO) < 0) {
                qteDisponible = BigDecimal.ZERO;
            }
            // Validation
            if (quantiteDemandee == null || quantiteDemandee.compareTo(BigDecimal.ZERO) <= 0) {
                return new ValidationQuantiteDTO(false, "La quantité doit être positive",
                        qteDisponible, quantiteDemandee);
            }
            if (quantiteDemandee.compareTo(qteDisponible) > 0) {
                return new ValidationQuantiteDTO(false,
                        String.format("Quantité demandée (%s) dépasse la quantité disponible (%s)",
                                quantiteDemandee, qteDisponible),
                        qteDisponible, quantiteDemandee);
            }
            return new ValidationQuantiteDTO(true, "Quantité valide", qteDisponible, quantiteDemandee);
        } catch (Exception e) {
            System.err.println("Erreur lors de la validation de quantité: " + e.getMessage());
            return new ValidationQuantiteDTO(false, "Erreur lors de la validation: " + e.getMessage(),
                    BigDecimal.ZERO, quantiteDemandee);
        }
    }
    @Override
    public List<LigneReceptionResponseDTO> getLignesReceptionByReceptionId(Integer receptionId) {
        try {
            Reception reception = getReceptionEntityById(receptionId);
            List<LigneReception> lignes = ligneReceptionRepository.findByEntId(reception.getNumero());
            // Si aucune ligne trouvée avec Ent_ID, essayer avec Commande (pour compatibilité)
            if (lignes.isEmpty() && reception.getCommande() != null) {
                lignes = ligneReceptionRepository.findByCommande(reception.getCommande());
                // Filtrer uniquement les lignes de cette réception spécifique
                lignes = lignes.stream()
                        .filter(l -> l.getEntId() != null && l.getEntId().equals(reception.getNumero()))
                        .collect(Collectors.toList());
            }
            return lignes.stream().map(this::mapLigneToResponseDTO).collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des lignes de réception: " + e.getMessage());
            e.printStackTrace();
            throw new BadRequestException("Erreur lors de la récupération des lignes: " + e.getMessage());
        }
    }

    //@Override
//    public ResponseMessage updateLigneReception(Integer ligneId, LigneReceptionRequestDTO ligneDto,
//                                                String currentUsername) {
//        LigneReception ligne = ligneReceptionRepository.findById(ligneId)
//                .orElseThrow(() -> new ResourceNotFoundException("Ligne de réception non trouvée"));
//        // Vérifier le statut de la réception via Ent_ID
//        Reception reception = null;
//        if (ligne.getEntId() != null) {
//            reception = receptionRepository.findById(ligne.getEntId()).orElse(null);
//        }
//        // Si pas trouvé par Ent_ID, chercher par Commande
//        if (reception == null && ligne.getCommande() != null) {
//            List<Reception> receptions = receptionRepository.findByCommande(ligne.getCommande());
//            if (!receptions.isEmpty()) {
//                reception = receptions.get(0);
//            }
//        }
//        if (reception == null) {
//            throw new ResourceNotFoundException("Réception non trouvée pour cette ligne");
//        }
//        if (reception.getSysState() != null && reception.getSysState() == 1) {
//            throw new BadRequestException("Impossible de modifier une ligne d'une réception envoyée");
//        }
//        // Valider la nouvelle quantité
//        ValidationQuantiteDTO validation = validerQuantiteArticle(
//                reception.getNumero(), ligne.getArticle(), ligneDto.getQuantite(), ligneId);
//        if (!validation.isValide()) {
//            throw new BadRequestException(validation.getMessage());
//        }
//        // Mettre à jour la ligne
//        ligne.setQte(ligneDto.getQuantite());
//        ligne.setQteLivre(ligneDto.getQuantite());
//        if (ligneDto.getDesignationArticle() != null) {
//            ligne.setDeseignation(ligneDto.getDesignationArticle());
//        }
//        if (ligneDto.getUnite() != null) {
//            ligne.setUnite(ligneDto.getUnite());
//        }
//        ligne.setSysModificationDate(LocalDateTime.now());
//        ligneReceptionRepository.save(ligne);
//        return new ResponseMessage("Ligne mise à jour avec succès");
//    }
    // Méthode pour récupérer les fichiers d'une réception
// Remplacer cette méthode dans ReceptionServiceImpl.java :

// Méthode pour récupérer les fichiers d'une réception
public List<DemandeAchatFileResponseDTO> getReceptionFiles(Integer receptionId) {
    try {
        System.out.println("=== RÉCUPÉRATION FICHIERS RÉCEPTION ===");
        System.out.println("Réception ID: " + receptionId);

        Reception reception = receptionRepository.findById(receptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Réception non trouvée: " + receptionId));

        System.out.println("Réception trouvée, pj_bc: " + reception.getPjBc());

        if (reception.getPjBc() == null) {
            System.out.println("Aucun groupe de fichiers associé à cette réception");
            return new ArrayList<>();
        }

        // Récupérer les fichiers du groupe depuis KDN_FILE
        List<KdnFile> files = kdnFileRepository.findByGroupIdOrderByUploadDateDesc(reception.getPjBc());
        System.out.println("Fichiers trouvés: " + files.size());

        return files.stream().map(file -> {
            System.out.println("Traitement fichier: " + file.getFullFileName() + " (ID: " + file.getFileId() + ")");

            DemandeAchatFileResponseDTO dto = new DemandeAchatFileResponseDTO();
            dto.setFileId(file.getFileId());
            dto.setName(file.getName() != null ? file.getName() : "");
            dto.setExtension(file.getExtension() != null ? file.getExtension() : "");
            dto.setFullFileName(file.getFullFileName());
            dto.setSize(file.getSize() != null ? file.getSize() : 0);
            dto.setSizeFormatted(file.getSizeFormatted());
            dto.setUploadDate(file.getSysCreationDate());
            dto.setNbOpen(file.getNbOpen() != null ? file.getNbOpen() : 0);
            dto.setDownloadUrl("/api/receptions/files/" + file.getFileId() + "/download");
            dto.setCanDelete(file.getSysState() != null && file.getSysState() == 1);
            dto.setCanDownload(file.getSysState() != null && file.getSysState() == 1);
            dto.setCategory("Pièce jointe réception");
            dto.setDocumentType("Fichier réception");
            dto.setAlt(file.getAlt() != null ? file.getAlt() : "");

            // Informations utilisateur si disponibles
            if (file.getSysCreatorId() != null) {
                try {
                    Optional<KdnsAccessor> creator = kdnsAccessorRepository.findById(file.getSysCreatorId());
                    if (creator.isPresent()) {
                        dto.setUploadedByNom(formatUserName(creator.get()));
                        dto.setUploadedBy(creator.get().getLogin());
                    } else {
                        dto.setUploadedByNom("Utilisateur inconnu");
                        dto.setUploadedBy("unknown");
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de la récupération de l'utilisateur créateur: " + e.getMessage());
                    dto.setUploadedByNom("Erreur utilisateur");
                    dto.setUploadedBy("error");
                }
            } else {
                dto.setUploadedByNom("Système");
                dto.setUploadedBy("system");
            }

            return dto;
        }).collect(Collectors.toList());

    } catch (ResourceNotFoundException e) {
        System.err.println("Réception non trouvée: " + e.getMessage());
        throw e;
    } catch (Exception e) {
        System.err.println("Erreur lors de la récupération des fichiers de réception: " + e.getMessage());
        e.printStackTrace();
        return new ArrayList<>();
    }
}


    public ResponseMessage updateLigneReception(Integer ligneId, LigneReceptionRequestDTO ligneDto,
                                                String currentUsername) {
        LigneReception ligne = ligneReceptionRepository.findById(ligneId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne de réception non trouvée"));

        // Vérifier le statut de la réception via Ent_ID
        Reception reception = null;
        if (ligne.getEntId() != null) {
            reception = receptionRepository.findById(ligne.getEntId()).orElse(null);
        }

        // Si pas trouvé par Ent_ID, chercher par Commande
        if (reception == null && ligne.getCommande() != null) {
            List<Reception> receptions = receptionRepository.findByCommande(ligne.getCommande());
            if (!receptions.isEmpty()) {
                reception = receptions.get(0);
            }
        }

        if (reception == null) {
            throw new ResourceNotFoundException("Réception non trouvée pour cette ligne");
        }

        // Vérifier si réception déjà envoyée
        if (reception.getSysState() != null && reception.getSysState() == 1) {
            throw new BadRequestException("Impossible de modifier une ligne d'une réception envoyée");
        }

        // Ancienne et nouvelle quantité
        BigDecimal ancienneQte = ligne.getQte() != null ? ligne.getQte() : BigDecimal.ZERO;
        BigDecimal nouvelleQte = ligneDto.getQuantite() != null ? ligneDto.getQuantite() : BigDecimal.ZERO;

        // Différence
        BigDecimal difference = nouvelleQte.subtract(ancienneQte);

        // Mettre à jour qte
        ligne.setQte(nouvelleQte);

        // Mettre à jour qte_livre et reste selon la différence
        if (difference.compareTo(BigDecimal.ZERO) > 0) {
            // Nouvelle quantite > ancienne → on augmente qte_livre et on diminue reste
            ligne.setQteLivre(ligne.getQteLivre().add(difference));
            ligne.setReste(ligne.getReste().subtract(difference));
        } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
            // Nouvelle quantite < ancienne → on diminue qte_livre et on augmente reste
            BigDecimal absDiff = difference.abs();
            ligne.setQteLivre(ligne.getQteLivre().subtract(absDiff));
            ligne.setReste(ligne.getReste().add(absDiff));
        }

        // Mettre à jour autres infos
        if (ligneDto.getDesignationArticle() != null) {
            ligne.setDeseignation(ligneDto.getDesignationArticle());
        }
        if (ligneDto.getUnite() != null) {
            ligne.setUnite(ligneDto.getUnite());
        }

        ligne.setSysModificationDate(LocalDateTime.now());
        ligneReceptionRepository.save(ligne);

        return new ResponseMessage("Ligne mise à jour avec succès");
    }

    @Override
    public ResponseMessage deleteLigneReception(Integer ligneId, String currentUsername) {
        LigneReception ligne = ligneReceptionRepository.findById(ligneId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne de réception non trouvée"));

        // Vérifier le statut de la réception via Ent_ID
        Reception reception = null;
        if (ligne.getEntId() != null) {
            reception = receptionRepository.findById(ligne.getEntId()).orElse(null);
        }

        // Si pas trouvé par Ent_ID, chercher par Commande
        if (reception == null && ligne.getCommande() != null) {
            List<Reception> receptions = receptionRepository.findByCommande(ligne.getCommande());
            if (!receptions.isEmpty()) {
                reception = receptions.get(0);
            }
        }

        if (reception == null) {
            throw new ResourceNotFoundException("Réception non trouvée pour cette ligne");
        }

        if (reception.getSysState() != null && reception.getSysState() == 1) {
            throw new BadRequestException("Impossible de supprimer une ligne d'une réception envoyée");
        }

        ligneReceptionRepository.delete(ligne);

        return new ResponseMessage("Ligne supprimée avec succès");
    }

    // ==================== MÉTHODES PRIVÉES ====================

    private Reception getReceptionEntityById(Integer id) {
        return receptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réception non trouvée avec l'ID: " + id));
    }

    private ReceptionResponseDTO mapToResponseDTO(Reception reception) {
        return mapToResponseDTOs(Collections.singletonList(reception)).get(0);
    }

    private List<ReceptionResponseDTO> mapToResponseDTOs(List<Reception> receptions) {
        if (receptions.isEmpty()) {
            return Collections.emptyList();
        }
        // CORRECTION: Traiter les réceptions par lots pour éviter les requêtes trop grandes
        final int BATCH_SIZE = 500; // Limite pour éviter les erreurs SQL
        // Diviser les réceptions en lots
        List<List<Reception>> batches = new ArrayList<>();
        for (int i = 0; i < receptions.size(); i += BATCH_SIZE) {
            batches.add(receptions.subList(i, Math.min(i + BATCH_SIZE, receptions.size())));
        }
        // Traiter chaque lot et combiner les résultats
        List<ReceptionResponseDTO> allResults = new ArrayList<>();
        for (List<Reception> batch : batches) {
            // Pré-charger les données pour ce lot
            Set<Long> commandeIds = batch.stream()
                    .map(r -> Long.valueOf(r.getCommande()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Set<Integer> userIds = batch.stream()
                    .flatMap(r -> Stream.of(r.getSysCreatorId(), r.getSysUserId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, Commande> commandes = new HashMap<>();
            Map<Integer, KdnsAccessor> users = new HashMap<>();
            // Charger les commandes uniquement si nécessaire
            if (!commandeIds.isEmpty() && commandeIds.size() <= BATCH_SIZE) {
                try {
                    commandes = commandeRepository.findAllById(commandeIds)
                            .stream()
                            .collect(Collectors.toMap(Commande::getCommande, c -> c));
                } catch (Exception e) {
                    System.err.println("Erreur lors du chargement des commandes: " + e.getMessage());
                    // Continuer sans les données de commande
                }
            }
            // Charger les utilisateurs uniquement si nécessaire
            if (!userIds.isEmpty() && userIds.size() <= BATCH_SIZE) {
                try {
                    users = kdnsAccessorRepository.findAllById(userIds)
                            .stream()
                            .collect(Collectors.toMap(KdnsAccessor::getAccessorId, u -> u));
                } catch (Exception e) {
                    System.err.println("Erreur lors du chargement des utilisateurs: " + e.getMessage());
                    // Continuer sans les données utilisateur
                }
            }
            final Map<Long, Commande> finalCommandes = commandes;
            final Map<Integer, KdnsAccessor> finalUsers = users;
            // Mapper les réceptions de ce lot
            List<ReceptionResponseDTO> batchResults = batch.stream()
                    .map(reception -> {
                        ReceptionResponseDTO dto = new ReceptionResponseDTO();
                        dto.setId(reception.getNumero());
                        dto.setCommandeCode(reception.getCommande());
                        dto.setAffaireId(reception.getCommande());
                        dto.setUserId(reception.getSysCreatorId());
                        dto.setReferenceBl(reception.getPinotiers());
                        dto.setIdAgelio(reception.getPjBc());
                        dto.setBlDivalto(reception.getBlDivalto() != null ? reception.getBlDivalto() : -1);
                        dto.setStatut(reception.getSysState() != null && reception.getSysState() == 1 ? "Envoyé" : "Brouillon");
                        dto.setCreatedDate(reception.getSysCreationDate());
                        dto.setDateReception(reception.getSysCreationDate());

                        // Enrichissement avec les données pré-chargées (si disponibles)
                        if (reception.getCommande() != null) {
                            Commande commande = finalCommandes.get(Long.valueOf(reception.getCommande()));
                            if (commande != null) {
                                dto.setAffaireCode(commande.getAffaireCode());
                                dto.setAffaireLibelle(commande.getAffaireName());
                                dto.setRefFournisseur(commande.getFournisseurId());
                                dto.setNomFournisseur(commande.getFournisseur());
                                if (commande.getDateCommande() != null) {
                                    dto.setDateBl(reception.getSysModificationDate());
                                }
                            }
                        }
                        if (reception.getSysCreatorId() != null) {
                            KdnsAccessor creator = finalUsers.get(reception.getSysCreatorId());

                            if (creator != null) {
                                dto.setUserLogin(creator.getLogin());
                                dto.setCreatedBy(creator.getLogin());
                                dto.setCreateurNom(formatUserName(creator));
                            }
                        }
                        return dto;
                    })
                    .collect(Collectors.toList());

            allResults.addAll(batchResults);
        }

        return allResults;
    }

    private LigneReceptionResponseDTO mapLigneToResponseDTO(LigneReception ligne) {
        LigneReceptionResponseDTO dto = new LigneReceptionResponseDTO();
        dto.setId(ligne.getNumero());
        dto.setReferenceArticle(ligne.getArticle());
        dto.setDesignationArticle(ligne.getDeseignation());
        dto.setQuantite(ligne.getQte());
        dto.setQteCmd(ligne.getQteCmd());
        dto.setQteLivre(ligne.getQteLivre());
        dto.setReste(ligne.getReste());
        dto.setUnite(ligne.getUnite());
        dto.setFamilleStatistique1(ligne.getSref1());
        dto.setFamilleStatistique2(ligne.getSref2());
        dto.setSysCreationDate(ligne.getSysCreationDate());
        dto.setSysModificationDate(ligne.getSysModificationDate());
        dto.setStatut(ligne.getIntegre() != null && ligne.getIntegre() == 1 ? "Intégré" : "Non intégré");
        return dto;
    }

    // MÉTHODE UTILITAIRE (ajoutez-la si elle n'existe pas déjà)
    private String formatUserName(KdnsAccessor accessor) {
        if (accessor.getFullName() != null && !accessor.getFullName().trim().isEmpty()) {
            return accessor.getFullName();
        }
        String firstName = accessor.getFirstName() != null ? accessor.getFirstName() : "";
        String lastName = accessor.getLastName() != null ? accessor.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? accessor.getLogin() : fullName;
    }
    private PagedResponse<ReceptionResponseDTO> buildEmptyPagedResponse(int page, int size) {
        PagedResponse<ReceptionResponseDTO> response = new PagedResponse<>();
        response.setContent(Collections.emptyList());
        response.setPageNumber(page);
        response.setPageSize(size);
        response.setTotalElements(0);
        response.setTotalPages(0);
        response.setFirst(true);
        response.setLast(true);
        response.setHasNext(false);
        response.setHasPrevious(false);
        return response;
    }
    private String extraireDepotDuCodeAffaire(String codeAffaire) {
        if (codeAffaire == null || (codeAffaire = codeAffaire.trim()).isEmpty()) {
            throw new BadRequestException("Code affaire invalide: " + codeAffaire);
        }

        // Si ça commence par CH, on retire le préfixe
        String base = codeAffaire.startsWith("CH") ? codeAffaire.substring(2) : codeAffaire;

        if (base.length() < 3) {
            throw new BadRequestException(
                    "Code affaire trop court pour extraire un dépôt (3 caractères requis): " + codeAffaire
            );
        }

        String depot = base.substring(base.length() - 3);
        System.out.println("Extraction dépôt depuis: " + codeAffaire + " -> " + depot);
        return depot;
    }
    private String mapSortFieldToEntityProperty(String frontendField) {
        if (frontendField == null) {
            return "numero";
        }

        switch (frontendField.toLowerCase()) {
            case "referenceBl":
                return "pinotiers";
            case "datebl":
                return "sysModificationDate";
            case "datereception":
            case "syscreationdate":
                return "sysCreationDate";
            case "commande":
                return "commande";
            case "sysstate":
            case "statut":
                return "sysState";
            case "numero":
            case "id":
                return "numero";
            default:
                // Si le champ n'est pas reconnu, utiliser "numero" par défaut
                System.out.println("⚠️ Champ de tri non reconnu: " + frontendField + ", utilisation de 'numero'");
                return "numero";
        }
    }
}


