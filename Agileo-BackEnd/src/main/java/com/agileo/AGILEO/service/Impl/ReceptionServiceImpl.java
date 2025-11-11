package com.agileo.AGILEO.service.Impl;

import com.agileo.AGILEO.Dtos.request.*;
import com.agileo.AGILEO.Dtos.response.*;
import com.agileo.AGILEO.entity.divalto.MOUV;
import com.agileo.AGILEO.entity.divalto.Mvtl;
import com.agileo.AGILEO.entity.primary.KdnFile;
import com.agileo.AGILEO.repository.divalto.MouvRepository;
import com.agileo.AGILEO.repository.divalto.MvtlRepository;
import com.agileo.AGILEO.repository.primary.KdnFileRepository;
import com.agileo.AGILEO.repository.primary.KdnFileGroupRepository;
import com.agileo.AGILEO.entity.primary.*;
import com.agileo.AGILEO.exception.BadRequestException;
import com.agileo.AGILEO.exception.ResourceNotFoundException;
import com.agileo.AGILEO.message.ResponseMessage;
import com.agileo.AGILEO.repository.primary.*;
import com.agileo.AGILEO.service.DivaltoIntegrationReceptionService;
import com.agileo.AGILEO.service.ReceptionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final AffaireDisplayRepository affaireDisplayRepository; // ✅ NOUVEAU
    private final KdnsAccessorRepository kdnsAccessorRepository;
    private final KdnFileRepository kdnFileRepository;
    private final ArticleReceptionRepository articleReceptionRepository;
    private final UserServiceImpl userService;
    private final CommandeRepository commandeRepository;
    private final VentilationArticleRepository ventilationArticleRepository;
    private final KdnFileGroupRepository kdnFileGroupRepository;
    private final DivaltoIntegrationReceptionService divaltoIntegrationReceptionService;
    private final MvtlRepository mvtlRepository;
    private final MouvRepository mouvRepository;

    public ReceptionServiceImpl(
            ReceptionRepository receptionRepository,
            LigneReceptionRepository ligneReceptionRepository,
            AffaireRepository affaireRepository,
            AffaireDisplayRepository affaireDisplayRepository, // ✅ NOUVEAU
            KdnsAccessorRepository kdnsAccessorRepository,
            ArticleReceptionRepository articleReceptionRepository,
            UserServiceImpl userService,
            CommandeRepository commandeRepository,
            VentilationArticleRepository ventilationArticleRepository,
            KdnFileRepository kdnFileRepository,
            KdnFileGroupRepository kdnFileGroupRepository,
            DivaltoIntegrationReceptionService divaltoIntegrationReceptionService,
            MvtlRepository mvtlRepository,
            MouvRepository mouvRepository) {

        this.receptionRepository = receptionRepository;
        this.ligneReceptionRepository = ligneReceptionRepository;
        this.affaireRepository = affaireRepository;
        this.affaireDisplayRepository = affaireDisplayRepository; // ✅ NOUVEAU
        this.kdnsAccessorRepository = kdnsAccessorRepository;
        this.articleReceptionRepository = articleReceptionRepository;
        this.userService = userService;
        this.commandeRepository = commandeRepository;
        this.ventilationArticleRepository = ventilationArticleRepository;
        this.kdnFileRepository = kdnFileRepository;
        this.kdnFileGroupRepository = kdnFileGroupRepository;
        this.divaltoIntegrationReceptionService = divaltoIntegrationReceptionService;
        this.mvtlRepository = mvtlRepository;
        this.mouvRepository = mouvRepository;
    }

    // ==================== CRÉATION ET GESTION DES RÉCEPTIONS ====================

    @Override
    public ReceptionResponseDTO createReception(ReceptionRequestDTO receptionDto, String currentUsername) {
        try {
            // ✅ GARDE affaireRepository pour la SÉCURITÉ (affaires autorisées uniquement)
            Affaire affaire = null;
            try {
                Integer affaireId = Integer.parseInt(String.valueOf(receptionDto.getAffaireId()));
                affaire = affaireRepository.findById(String.valueOf(affaireId)).orElse(null);
            } catch (NumberFormatException e) {
                System.out.println("AffaireId n'est pas un nombre, recherche par code: " + receptionDto.getAffaireId());
            }

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
            reception.setPinotiers(receptionDto.getIdAgelio());
            reception.setSysCreationDate(receptionDto.getDateReception());
            reception.setSysModificationDate(receptionDto.getDateBl());
            reception.setSysCreatorId(accessorId);
            reception.setSysUserId(accessorId);

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

    @Override
    public PagedResponse<ReceptionResponseDTO> getAllReceptionsPaginated(
            int page, int size, String sortBy, String sortDirection, String search) {

        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
        if (sortBy == null || sortBy.isEmpty()) sortBy = "numero";

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
            if (size <= 0 || size > 100) size = 20;
            if (sortBy == null || sortBy.isEmpty()) sortBy = "numero";

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

        Integer commandeId = reception.getCommande();
        if (commandeId != null) {
            ligneReceptionRepository.deleteByCommande(commandeId);
        }

        receptionRepository.delete(reception);
        return new ResponseMessage("Réception supprimée avec succès");
    }

    // ==================== GESTION DES ARTICLES DISPONIBLES ====================

    @Override
    public List<ArticleDisponibleDTO> getArticlesDisponibles(Integer receptionId) {
        try {
            Reception reception = getReceptionEntityById(receptionId);
            Integer commandeNumber = reception.getCommande();
            if (commandeNumber == null) {
                return Collections.emptyList();
            }

            List<ArticleReception> articlesReception = articleReceptionRepository
                    .findArticleReceptionsByCommande(commandeNumber.longValue());

            List<LigneReception> toutesLesLignes = ligneReceptionRepository.findByCommande(commandeNumber);

            Map<String, BigDecimal> quantitesDejaRecues = toutesLesLignes.stream()
                    .filter(ligne -> ligne.getArticle() != null && ligne.getQte() != null)
                    .collect(Collectors.groupingBy(
                            LigneReception::getArticle,
                            Collectors.reducing(BigDecimal.ZERO, LigneReception::getQte, BigDecimal::add)
                    ));

            List<ArticleDisponibleDTO> articlesDisponibles = new ArrayList<>();

            for (ArticleReception articleReception : articlesReception) {
                ArticleDisponibleDTO dto = new ArticleDisponibleDTO();

                dto.setReference(articleReception.getArticleId());
                dto.setDesignation(articleReception.getDesignation());
                dto.setUnite(articleReception.getUnite());

                BigDecimal qteCommandee = articleReception.getQteCommandee() != null ?
                        articleReception.getQteCommandee() : BigDecimal.ZERO;
                BigDecimal qteRest = articleReception.getQteRest() != null ?
                        articleReception.getQteRest() : BigDecimal.ZERO;

                BigDecimal qteDejaRecue = quantitesDejaRecues.getOrDefault(
                        articleReception.getArticleId(),
                        BigDecimal.ZERO
                );

                dto.setQuantiteCommandee(qteCommandee);
                dto.setQuantiteDejaRecue(qteDejaRecue);
                dto.setQuantiteDisponible(qteRest);

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
        try {
            Reception reception = getReceptionEntityById(receptionId);
            if (reception.getSysState() != null && reception.getSysState() == 1) {
                throw new BadRequestException("Impossible d'ajouter des lignes à une réception envoyée");
            }

            Integer accessorId = null;
            try {
                UserResponseDTO currentUser = userService.findUserByLogin(currentUsername);
                if (currentUser.getIdAgelio() != null && !currentUser.getIdAgelio().trim().isEmpty()) {
                    accessorId = Integer.parseInt(currentUser.getIdAgelio());
                }
            } catch (Exception e) {
                System.out.println("Impossible de récupérer l'ID utilisateur: " + e.getMessage());
            }

            if (reception.getCommande() == null) {
                throw new BadRequestException("Numéro de commande manquant pour la réception " + receptionId);
            }

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

            if (lignesDto == null || lignesDto.isEmpty()) {
                throw new BadRequestException("Aucune ligne à ajouter");
            }

            for (LigneReceptionRequestDTO ligneDto : lignesDto) {
                if (ligneDto.getReferenceArticle() == null || ligneDto.getReferenceArticle().trim().isEmpty()) {
                    throw new BadRequestException("Référence article manquante");
                }
                if (ligneDto.getQuantite() == null || ligneDto.getQuantite().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BadRequestException("Quantité invalide pour l'article " + ligneDto.getReferenceArticle());
                }

                ArticleReception articleReception = articlesMap.get(ligneDto.getReferenceArticle());
                if (articleReception == null) {
                    throw new BadRequestException("Article " + ligneDto.getReferenceArticle() +
                            " non trouvé dans le bon de commande " + reception.getCommande());
                }

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

                try {
                    LigneReception ligne = new LigneReception();

                    // Récupération MOUV et MVTL
                    Optional<MOUV> mouvOpt = mouvRepository.findLigneCommandeByPinoAndRef(
                            BigDecimal.valueOf(reception.getCommande()),
                            ligneDto.getReferenceArticle().trim()
                    );

                    if (!mouvOpt.isPresent()) {
                        throw new BadRequestException("Ligne de commande non trouvée pour l'article: " + ligneDto.getReferenceArticle());
                    }

                    MOUV mouv = mouvOpt.get();

                    Optional<Mvtl> mvtlOpt = mvtlRepository.findMouvementByEnrno(
                            mouv.getRef(),
                            mouv.getCdno()
                    );

                    if (!mvtlOpt.isPresent()) {
                        throw new BadRequestException("Mouvement de stock non trouvé pour l'article: " + ligneDto.getReferenceArticle());
                    }

                    Mvtl mvtl = mvtlOpt.get();

                    ligne.setEntId(reception.getNumero());
                    ligne.setCommande(reception.getCommande());
                    ligne.setArticle(ligneDto.getReferenceArticle());
                    ligne.setEnrno(mvtl.getEnrno().intValue());
                    ligne.setVtlno(mvtl.getVtlno().intValue());

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

                    String unite = articleReception.getUnite();
                    if (ligneDto.getUnite() != null && !ligneDto.getUnite().trim().isEmpty()) {
                        unite = ligneDto.getUnite();
                    }
                    ligne.setUnite(unite);
                    ligne.setAffaire(articleReception.getAffaireCode());
                    ligne.setTiers(null);

                    ligne.setIntegre(2);
                    ligne.setSysCreationDate(LocalDateTime.now());
                    ligne.setSysModificationDate(LocalDateTime.now());
                    ligne.setSysCreatorId(accessorId);
                    ligne.setSysUserId(accessorId);
                    ligne.setPldt(reception.getSysModificationDate());
                    ligne.setSysState(1);

                    ligneReceptionRepository.save(ligne);
                    System.out.println("Ligne ajoutée avec succès pour l'article: " + ligneDto.getReferenceArticle());

                } catch (BadRequestException e) {
                    throw e;
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

            if (reception.getSysState() != null && reception.getSysState() == 1) {
                return new ValidationQuantiteDTO(false, "Réception déjà envoyée", BigDecimal.ZERO, BigDecimal.ZERO);
            }

            if (reception.getCommande() == null) {
                return new ValidationQuantiteDTO(false, "Numéro de commande manquant", BigDecimal.ZERO, quantiteDemandee);
            }

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

            BigDecimal qteCommandee = articleReception.getQteCommandee() != null ?
                    articleReception.getQteCommandee() : BigDecimal.ZERO;

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
            }

            BigDecimal qteDisponible = qteCommandee.subtract(qteDejaRecue);
            if (qteDisponible.compareTo(BigDecimal.ZERO) < 0) {
                qteDisponible = BigDecimal.ZERO;
            }

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

            if (lignes.isEmpty() && reception.getCommande() != null) {
                lignes = ligneReceptionRepository.findByCommande(reception.getCommande());
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

        Reception reception = null;
        if (ligne.getEntId() != null) {
            reception = receptionRepository.findById(ligne.getEntId()).orElse(null);
        }

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
            throw new BadRequestException("Impossible de modifier une ligne d'une réception envoyée");
        }

        BigDecimal ancienneQte = ligne.getQte() != null ? ligne.getQte() : BigDecimal.ZERO;
        BigDecimal nouvelleQte = ligneDto.getQuantite() != null ? ligneDto.getQuantite() : BigDecimal.ZERO;
        BigDecimal difference = nouvelleQte.subtract(ancienneQte);

        ligne.setQte(nouvelleQte);

        if (difference.compareTo(BigDecimal.ZERO) > 0) {
            ligne.setQteLivre(ligne.getQteLivre().add(difference));
            ligne.setReste(ligne.getReste().subtract(difference));
        } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal absDiff = difference.abs();
            ligne.setQteLivre(ligne.getQteLivre().subtract(absDiff));
            ligne.setReste(ligne.getReste().add(absDiff));
        }

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

        Reception reception = null;
        if (ligne.getEntId() != null) {
            reception = receptionRepository.findById(ligne.getEntId()).orElse(null);
        }

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

    /**
     * ✅ MÉTHODE MODIFIÉE - Utilise AffaireDisplayRepository pour l'affichage
     */
    private List<ReceptionResponseDTO> mapToResponseDTOs(List<Reception> receptions) {
        if (receptions.isEmpty()) {
            return Collections.emptyList();
        }

        final int BATCH_SIZE = 500;
        List<List<Reception>> batches = new ArrayList<>();
        for (int i = 0; i < receptions.size(); i += BATCH_SIZE) {
            batches.add(receptions.subList(i, Math.min(i + BATCH_SIZE, receptions.size())));
        }

        List<ReceptionResponseDTO> allResults = new ArrayList<>();

        for (List<Reception> batch : batches) {
            // Les codes d'affaires seront résolus via les commandes

            Set<Long> commandeIds = batch.stream()
                    .map(r -> r.getCommande() != null ? Long.valueOf(r.getCommande()) : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Set<Integer> userIds = batch.stream()
                    .flatMap(r -> Stream.of(r.getSysCreatorId(), r.getSysUserId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // ✅ NOUVEAU : Charger les affaires depuis AffaireDisplayRepository
            Map<String, AffaireDisplay> affaires = new HashMap<>();

            Map<Long, Commande> commandes = new HashMap<>();
            if (!commandeIds.isEmpty() && commandeIds.size() <= BATCH_SIZE) {
                try {
                    commandes = commandeRepository.findAllById(commandeIds)
                            .stream()
                            .collect(Collectors.toMap(Commande::getCommande, c -> c));
                } catch (Exception e) {
                    System.err.println("Erreur lors du chargement des commandes: " + e.getMessage());
                }
            }

            // ✅ Charger les AffairesDisplay à partir des codes d'affaires des commandes
            Set<String> affaireCodesFromCommandes = commandes.values().stream()
                    .map(Commande::getAffaireCode)
                    .filter(code -> code != null && !code.trim().isEmpty())
                    .collect(Collectors.toSet());
            if (!affaireCodesFromCommandes.isEmpty()) {
                try {
                    affaires = affaireDisplayRepository.findAllById(affaireCodesFromCommandes)
                            .stream()
                            .collect(Collectors.toMap(AffaireDisplay::getAffaire, a -> a));
                } catch (Exception e) {
                    System.err.println("Erreur lors du chargement des AffairesDisplay: " + e.getMessage());
                }
            }

            // ✅ CORRECTION : Utiliser getAccessorId au lieu de getId
            Map<Integer, KdnsAccessor> users = new HashMap<>();
            if (!userIds.isEmpty() && userIds.size() <= BATCH_SIZE) {
                try {
                    users = kdnsAccessorRepository.findAllById(userIds)
                            .stream()
                            .collect(Collectors.toMap(KdnsAccessor::getAccessorId, u -> u));
                } catch (Exception e) {
                    System.err.println("Erreur lors du chargement des utilisateurs: " + e.getMessage());
                }
            }

            final Map<String, AffaireDisplay> finalAffaires = affaires;
            final Map<Long, Commande> finalCommandes = commandes;
            final Map<Integer, KdnsAccessor> finalUsers = users;

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

                        // ✅ Mapper l'affaire à partir de la commande -> affaireCode -> AffaireDisplay
                        if (reception.getCommande() != null) {
                            Commande cmd = finalCommandes.get(Long.valueOf(reception.getCommande()));
                            if (cmd != null && cmd.getAffaireCode() != null) {
                                AffaireDisplay affaire = finalAffaires.get(cmd.getAffaireCode());
                                if (affaire != null) {
                                    dto.setAffaireCode(affaire.getAffaire());
                                    dto.setAffaireLibelle(affaire.getLibelle());
                                } else {
                                    // fallback minimal: afficher le code s'il existe
                                    dto.setAffaireCode(cmd.getAffaireCode());
                                }
                            }
                        }

                        // Mapper l'utilisateur créateur
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
            case "referencebl":
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
                System.out.println("⚠️ Champ de tri non reconnu: " + frontendField + ", utilisation de 'numero'");
                return "numero";
        }
    }

    @Override
    public ResponseMessage updateReceptionStatut(Integer receptionId, Integer newStatut, String currentUsername) {
        try {
            System.out.println("=== DÉBUT updateReceptionStatut ===");
            System.out.println("Réception ID: " + receptionId);
            System.out.println("Nouveau statut: " + newStatut);
            System.out.println("Utilisateur: " + currentUsername);

            Reception reception = getReceptionEntityById(receptionId);
            Integer oldStatut = reception.getSysState();

            if (oldStatut != null && oldStatut == 1 && newStatut == 0) {
                throw new BadRequestException("Impossible de remettre une réception envoyée en brouillon");
            }

            reception.setSysState(newStatut);

            if (newStatut == 1 && (oldStatut == null || oldStatut == 0)) {
                reception.setSysCreationDate(LocalDateTime.now());
                System.out.println("✅ Date de réception mise à jour");
            }

            receptionRepository.save(reception);
            System.out.println("✅ Réception sauvegardée avec le statut : " + newStatut);

            if (newStatut == 1) {
                System.out.println("🚀 Statut = 1 détecté - Déclenchement intégration Divalto...");

                try {
                    List<LigneReception> lignes = ligneReceptionRepository.findByEntId(reception.getNumero());
                    System.out.println("📋 Nombre de lignes : " + lignes.size());

                    if (lignes.isEmpty()) {
                        System.err.println("⚠️ ATTENTION : Aucune ligne de réception !");
                        throw new BadRequestException("Impossible d'envoyer une réception sans ligne d'article");
                    }

                    System.out.println("🔄 Appel de divaltoIntegrationReceptionService.integrerReceptionDansDivalto()...");
                    divaltoIntegrationReceptionService.integrerReceptionDansDivalto(receptionId, currentUsername);
                    System.out.println("✅ Intégration Divalto TERMINÉE avec succès !");

                } catch (Exception e) {
                    System.err.println("❌ ERREUR Divalto: " + e.getMessage());
                    e.printStackTrace();

                    reception.setSysState(oldStatut != null ? oldStatut : 0);
                    receptionRepository.save(reception);
                    System.out.println("🔄 Rollback effectué - statut restauré à : " + (oldStatut != null ? oldStatut : 0));

                    throw new RuntimeException("Échec intégration Divalto : " + e.getMessage());
                }
            } else {
                System.out.println("ℹ️ Statut différent de 1 - Pas d'intégration Divalto");
            }

            System.out.println("=== FIN updateReceptionStatut ===");

            String statusLabel = newStatut == 1 ? "Envoyé" : "Brouillon";
            return new ResponseMessage("Statut mis à jour : " + statusLabel +
                    (newStatut == 1 ? " et intégré dans Divalto" : ""));

        } catch (BadRequestException | ResourceNotFoundException e) {
            System.err.println("❌ Erreur métier: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Exception technique: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors du changement de statut : " + e.getMessage(), e);
        }
    }

}