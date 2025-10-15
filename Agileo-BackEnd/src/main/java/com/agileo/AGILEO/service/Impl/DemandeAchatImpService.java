package com.agileo.AGILEO.service.Impl;

import com.agileo.AGILEO.Dtos.request.DemandeAchatRequestDTO;
import com.agileo.AGILEO.Dtos.request.LigneDemandeAchatRequestDTO;
import com.agileo.AGILEO.Dtos.response.*;
import com.agileo.AGILEO.entity.primary.*;
import com.agileo.AGILEO.exception.BadRequestException;
import com.agileo.AGILEO.exception.ResourceNotFoundException;
import com.agileo.AGILEO.message.ResponseMessage;
import com.agileo.AGILEO.repository.primary.*;
import com.agileo.AGILEO.service.DemandeAchatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
public class DemandeAchatImpService implements DemandeAchatService {
    private final DemandeAchatRepository demandeAchatRepository;
    private final LigneDemandeAchatRepository ligneDemandeRepository;
    private final AffaireRepository affaireRepository;
    private final KdnsAccessorRepository kdnsAccessorRepository;
    private final  DemandeAchatRepository userRepository;
    private final UserServiceImpl userService;
    private final KdnFileRepository kdnFileRepository;

    public DemandeAchatImpService(DemandeAchatRepository demandeAchatRepository,
                                  LigneDemandeAchatRepository ligneDemandeRepository,
                                  AffaireRepository affaireRepository,
                                  KdnsAccessorRepository kdnsAccessorRepository, DemandeAchatRepository userRepository, UserServiceImpl userService, KdnFileRepository kdnFileRepository) {
        this.demandeAchatRepository = demandeAchatRepository;
        this.ligneDemandeRepository = ligneDemandeRepository;
        this.affaireRepository = affaireRepository;
        this.kdnsAccessorRepository = kdnsAccessorRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.kdnFileRepository = kdnFileRepository;
    }
    @Override
    public List<DemandeAchatFileResponseDTO> getDemandeFiles(Integer demandeId) {
        try {

            DemandeAchat demande = demandeAchatRepository.findById(demandeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Demande d'achat non trouvée: " + demandeId));


            if (demande.getPjDa() == null) {
                return new ArrayList<>();
            }

            // Récupérer les fichiers du groupe depuis KDN_FILE
            List<KdnFile> files = kdnFileRepository.findActiveFilesByGroupId(demande.getPjDa());

            return files.stream().map(file -> {
                DemandeAchatFileResponseDTO dto = new DemandeAchatFileResponseDTO();
                dto.setFileId(file.getFileId());

                // Le nom à afficher = NAME.EXTENSION (pas le FTPPATH)
                dto.setName(file.getName() != null ? file.getName() : "fichier");
                dto.setExtension(file.getExtension() != null ? file.getExtension() : "");

                // Le nom complet pour l'affichage
                String displayName = dto.getName();
                if (!dto.getExtension().isEmpty()) {
                    displayName += "." + dto.getExtension();
                }
                dto.setFullFileName(displayName);

                // Le reste des propriétés...
                dto.setSize(file.getSize() != null ? file.getSize() : 0);
                dto.setSizeFormatted(file.getSizeFormatted());
                dto.setUploadDate(file.getSysCreationDate());
                dto.setDownloadUrl("/api/demandes-achat/files/" + file.getFileId() + "/download");
                dto.setCanDelete(file.getSysState() != null && file.getSysState() == 1);
                dto.setCanDownload(file.getSysState() != null && file.getSysState() == 1);

                return dto;
            }).collect(Collectors.toList());

        } catch (ResourceNotFoundException e) {
            System.err.println("Demande non trouvée: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des fichiers de demande: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    @Override
    public DemandeAchatResponseDTO createDemandeAchat(DemandeAchatRequestDTO demandeDto, String currentUsername) {
        DemandeAchat demande = new DemandeAchat();
        demande.setNumDa(demandeDto.getNumDa());
        demande.setChantier(demandeDto.getChantier());
        demande.setCommentaire(demandeDto.getCommentaire());
        demande.setLogin(demandeDto.getLogin());
        demande.setStatut(0);
        demande.setDateDa(LocalDateTime.now());
        demande.setSysCreationDate(LocalDateTime.now());
        demande.setSysCreatorId(demande.getLogin());
        demande.setSysState(1);
        demande.setDelaiSouhaite(demandeDto.getDelaiSouhaite());
        demande.setSysUserId(demande.getLogin());
        DemandeAchat saved = demandeAchatRepository.save(demande);
        return mapEntityToDto(saved);
    }

    @Override
    public ResponseMessage updateDemandeAchat(Integer id, DemandeAchatRequestDTO demandeDto, String currentUsername) {
        DemandeAchat demande = getDemandeById(id);
        if (!demande.isPending()) {
            throw new BadRequestException("Seules les demandes en attente peuvent être modifiées");
        }
        demande.setChantier(demandeDto.getChantier());
        demande.setDelaiSouhaite(demandeDto.getDelaiSouhaite());
        demande.setCommentaire(demandeDto.getCommentaire());
        demande.setSysModificationDate(LocalDateTime.now());
        demandeAchatRepository.save(demande);
        return new ResponseMessage("Demande mise à jour avec succès");
    }

    @Override
    public ResponseMessage updateDemandeAchatStatut(Integer demandeID, String currentUsername) {
        DemandeAchat demande = getDemandeById(demandeID);
        demande.setStatut(1);
        demande.setSysModificationDate(LocalDateTime.now());
        demandeAchatRepository.save(demande);
        return new ResponseMessage("Statut de la demande mis à jour avec succès");
    }
    @Override
    public ResponseMessage deleteDemandeAchat(Integer id) {
        DemandeAchat demande = getDemandeById(id);
        if (!demande.isPending()) {
            throw new BadRequestException("Seules les demandes en attente peuvent être supprimées");
        }
        demandeAchatRepository.delete(demande);
        return new ResponseMessage("Demande marquée comme supprimée");
    }
    @Override
    public List<DemandeAchatResponseDTO> findAllDemandes() {
        List<DemandeAchat> demandes = demandeAchatRepository.findAll();
        return mapEntitiesToDtos(demandes);
    }
    //Demande d'achate liste avec pagination
    @Override
    public PagedResponse<DemandeAchatResponseDTO> findAllDemandesPaginated(
            int page, int size, String sortBy, String sortDirection) {
        if (page < 0) page = 0;
        if (size <= 0 || size > 20) size = 20;
        if (sortBy == null || sortBy.isEmpty()) sortBy = "id";
        Sort.Direction direction = sortDirection != null && sortDirection.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<DemandeAchat> demandesPage = demandeAchatRepository.findAll(pageable);
        List<DemandeAchatResponseDTO> demandeDtos = mapEntitiesToDtos(demandesPage.getContent());
        PagedResponse<DemandeAchatResponseDTO> response = new PagedResponse<>();
        response.setContent(demandeDtos);
        response.setPageNumber(demandesPage.getNumber());
        response.setPageSize(demandesPage.getSize());
        response.setTotalElements(demandesPage.getTotalElements());
        response.setTotalPages(demandesPage.getTotalPages());
        response.setFirst(demandesPage.isFirst());
        response.setLast(demandesPage.isLast());
        response.setHasNext(demandesPage.hasNext());
        response.setHasPrevious(demandesPage.hasPrevious());
        return response;
    }
    @Override
    public DemandeAchatResponseDTO findDemandeById(Integer id) {
        return mapEntityToDto(getDemandeById(id));
    }
    @Override
    public List<DemandeAchatResponseDTO> findDemandesByLogin(String login) {
        try {
            UserResponseDTO userDto = userService.findUserByLogin(login);
            String idAgelio = userDto.getIdAgelio();

            if (idAgelio == null || idAgelio.trim().isEmpty()) {
                return Collections.emptyList();
            }
            Integer accessorId;
            try {
                accessorId = Integer.parseInt(idAgelio);

            } catch (NumberFormatException e) {
                throw new BadRequestException("idAgelio invalide pour l'utilisateur: " + login + " (valeur: " + idAgelio + ")");
            }
            // CORRECTION PRINCIPALE: vérifiez que votre repository a la bonne méthode
            // Si votre méthode repository s'appelle findByLogin et attend un Integer, c'est correct
            // Sinon, il faut ajuster selon votre entity DemandeAchat
            List<DemandeAchat> demandes = demandeAchatRepository.findByLogin(accessorId);
            return mapEntitiesToDtos(demandes);
        } catch (ResourceNotFoundException e) {
            System.err.println("ResourceNotFoundException: " + e.getMessage());
            throw e;
        } catch (BadRequestException e) {
            System.err.println("BadRequestException: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des demandes pour login: " + login + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des demandes d'achat", e);
        }
    }
    //Les demandes d'achat d'un utilisateurs avec pagination
    @Override
    public PagedResponse<DemandeAchatResponseDTO> findAllDemandesByLoginPaginated(
            int page, int size, String sortBy, String sortDirection, String login) {
        try {
            UserResponseDTO userDto = userService.findUserByLogin(login);
            String idAgelio = userDto.getIdAgelio();
            if (idAgelio == null || idAgelio.trim().isEmpty()) {
                return (PagedResponse<DemandeAchatResponseDTO>) Collections.emptyList();
            }
            Integer accessorId;
            try {
                accessorId = Integer.parseInt(idAgelio);
            } catch (NumberFormatException e) {
                throw new BadRequestException("idAgelio invalide pour l'utilisateur: " + login + " (valeur: " + idAgelio + ")");
            }
            // CORRECTION PRINCIPALE: vérifiez que votre repository a la bonne méthode
            // Si votre méthode repository s'appelle findByLogin et attend un Integer, c'est correct
            // Sinon, il faut ajuster selon votre entity DemandeAchat
            if (page < 0) page = 0;
            if (size <= 0 || size > 20) size = 20;
            if (sortBy == null || sortBy.isEmpty()) sortBy = "id";
            Sort.Direction direction = sortDirection != null && sortDirection.equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            Page<DemandeAchat> demandesPage = demandeAchatRepository.findByLogin(accessorId,pageable);
            List<DemandeAchatResponseDTO> demandeDtos = mapEntitiesToDtos(demandesPage.getContent());
            PagedResponse<DemandeAchatResponseDTO> response = new PagedResponse<>();
            response.setContent(demandeDtos);
            response.setPageNumber(demandesPage.getNumber());
            response.setPageSize(demandesPage.getSize());
            response.setTotalElements(demandesPage.getTotalElements());
            response.setTotalPages(demandesPage.getTotalPages());
            response.setFirst(demandesPage.isFirst());
            response.setLast(demandesPage.isLast());
            response.setHasNext(demandesPage.hasNext());
            response.setHasPrevious(demandesPage.hasPrevious());
            return response;
        } catch (ResourceNotFoundException e) {
            System.err.println("ResourceNotFoundException: " + e.getMessage());
            throw e;
        } catch (BadRequestException e) {
            System.err.println("BadRequestException: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des demandes pour login: " + login + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des demandes d'achat", e);
        }
    }
    @Override
    public List<DemandeAchatResponseDTO> findDemandesByChantier(String chantier) {
        List<DemandeAchat> demandes = demandeAchatRepository.findByChantier(chantier);
        return mapEntitiesToDtos(demandes);
    }

    @Override
    public DemandeAchatResponseDTO findDemandeByNumDa(String numDa) {
        return demandeAchatRepository.findByNumDa(numDa).stream()
                .findFirst()
                .map(this::mapEntityToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable avec numDa: " + numDa));
    }

    @Override
    public ResponseMessage changeDemandeStatus(Integer id, Integer newStatus, String currentUsername) {
        DemandeAchat demande = getDemandeById(id);
        demande.setStatut(newStatus);
        demande.setSysModificationDate(LocalDateTime.now());
        demandeAchatRepository.save(demande);
        return new ResponseMessage("Statut mis à jour avec succès");
    }

    @Override
    public ResponseMessage removeFileFromDemande(Integer demandeId, Integer fileId, String currentUsername) {
        // TODO: Implémenter la logique de suppression de fichier
        return new ResponseMessage("Fichier supprimé avec succès");
    }



    @Override
    public List<LigneDemandeAchatResponseDTO> findLignesDemandeByDemandeId(Integer demandeId) {
        getDemandeById(demandeId); // vérifie existence
        return ligneDemandeRepository.findByDa(demandeId).stream()
                .map(this::mapLigneToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ResponseMessage addLignesDemande(Integer demandeId, LigneDemandeAchatRequestDTO lignesDto, String currentUsername) {
        DemandeAchat demande = getDemandeById(demandeId);
        if (!demande.isPending()) {
            throw new BadRequestException("Seules les demandes en attente peuvent être modifiées");
        }

        // CORRECTION: Passer le currentUsername pour définir sysUserId
        LigneDemandeAchat ligne = createLigneDemande(lignesDto, demande, currentUsername);
        ligneDemandeRepository.save(ligne);
        return new ResponseMessage("Ligne ajoutée avec succès");
    }

    @Override
    public ResponseMessage updateLigneDemande(Integer ligneId, LigneDemandeAchatRequestDTO ligneDto, String currentUsername) {
        LigneDemandeAchat ligne = ligneDemandeRepository.findById(ligneId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable: " + ligneId));

        DemandeAchat demande = ligne.getDemandeAchat();
        if (!demande.isPending()) {
            throw new BadRequestException("Seules les demandes en attente peuvent être modifiées");
        }

        // CORRECTION: Mise à jour complète avec tous les champs
        updateLigneFields(ligne, ligneDto, currentUsername);
        ligneDemandeRepository.save(ligne);
        return new ResponseMessage("Ligne mise à jour avec succès");
    }

    @Override
    public ResponseMessage deleteLigneDemande(Integer ligneId, String currentUsername) {
        LigneDemandeAchat ligne = ligneDemandeRepository.findById(ligneId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable: " + ligneId));
        DemandeAchat demande = ligne.getDemandeAchat();
        if (!demande.isPending()) {
            throw new BadRequestException("Seules les demandes en attente peuvent être modifiées");
        }
        ligneDemandeRepository.delete(ligne);
        return new ResponseMessage("Ligne supprimée avec succès");
    }

    // ==================== MÉTHODES PRIVÉES OPTIMISÉES ====================

    private DemandeAchat getDemandeById(Integer id) {
        return demandeAchatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable: " + id));
    }

    // ==================== CORRECTIONS DANS DemandeAchatImpService.java ====================


    @Override
    public ResponseMessage updateDemandeAchatStatut(Integer demandeID, Integer newStatut, String currentUsername) {
        try {
            DemandeAchat demande = getDemandeById(demandeID);

            // Validation du changement de statut
            if (demande.getStatut() != null && demande.getStatut() != 0 && newStatut == 0) {
                throw new BadRequestException("Impossible de remettre une demande envoyée en brouillon");
            }

            // Mise à jour du statut
            demande.setStatut(newStatut);
            demande.setSysModificationDate(LocalDateTime.now());

            // Si on passe en "Envoyé", ajouter la date d'envoi
            if (newStatut == 1 && (demande.getStatut() == null || demande.getStatut() == 0)) {
                demande.setDateDa(LocalDateTime.now());
            }

            demandeAchatRepository.save(demande);

            String statusLabel = getStatusLabel(newStatut);
            return new ResponseMessage("Statut de la demande mis à jour: " + statusLabel);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour du statut: " + e.getMessage());
        }
    }

    // Méthode helper pour obtenir le label du statut
    private String getStatusLabel(Integer statut) {
        if (statut == null || statut == 0) return "Brouillon";
        switch (statut) {
            case 1: return "Envoyé";
            case 2: return "Reçu";
            case 3: return "Approuvé";
            case -1: return "Rejeté";
            default: return "Inconnu";
        }
    }



// ==================== MÉTHODES PRIVÉES CORRIGÉES ====================

    /**
     * CORRECTION PRINCIPALE: Méthode mise à jour pour gérer tous les champs obligatoires
     */
    private LigneDemandeAchat createLigneDemande(LigneDemandeAchatRequestDTO dto, DemandeAchat demande, String currentUsername) {
        LigneDemandeAchat ligne = new LigneDemandeAchat();

        ligne.setRef(truncateString(dto.getRef(), 25));
        ligne.setDesignation(truncateString(dto.getDesignation(), 80));
        ligne.setUnite(truncateString(dto.getUnite(), 4));

        ligne.setFam0001(null);
        ligne.setFam0002(null);
        ligne.setFam0003(null);
        ligne.setSref1(truncateString(dto.getSref1(), 8));
        ligne.setSref2(truncateString(dto.getSref2(), 8));

        ligne.setQte(dto.getQte());

        ligne.setDa(demande.getId());
        ligne.setDemandeAchat(demande);

        Integer userId = getUserIdFromUsername(currentUsername);
        ligne.setSysUserId(userId);
        ligne.setSysCreatorId(userId);

        LocalDateTime now = LocalDateTime.now();
        ligne.setSysCreationDate(now);
        ligne.setSysModificationDate(now);

        ligne.setSysState(1);

        return ligne;
    }

    private String truncateString(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
    /**
     * NOUVELLE MÉTHODE: Mise à jour des champs d'une ligne existante
     */
    private void updateLigneFields(LigneDemandeAchat ligne, LigneDemandeAchatRequestDTO dto, String currentUsername) {
        // Mise à jour des champs modifiables
        ligne.setRef(dto.getRef());
        ligne.setDesignation(dto.getDesignation());
        ligne.setQte(dto.getQte());
        ligne.setUnite(dto.getUnite());

        // Mise à jour des familles si fournies
        if (dto.getFam0001() != null) {
            ligne.setFam0001(dto.getFam0001());
        }
        if (dto.getFam0002() != null) {
            ligne.setFam0002(dto.getFam0002());
        }
        if (dto.getFam0003() != null) {
            ligne.setFam0003(dto.getFam0003());
        }

        // CORRECTION: Mise à jour des champs système
        Integer userId = getUserIdFromUsername(currentUsername);
        ligne.setSysUserId(userId);
        ligne.setSysModificationDate(LocalDateTime.now());
    }

    /**
     * NOUVELLE MÉTHODE: Conversion username vers userId
     * Cette méthode doit être adaptée selon votre logique métier
     */
    private Integer getUserIdFromUsername(String username) {
        try {
            // Option 1: Si le username est directement l'ID
            if (username != null && username.matches("\\d+")) {
                return Integer.parseInt(username);
            }

            // Option 2: Récupérer via le service utilisateur
            UserResponseDTO user = userService.findUserByLogin(username);
            if (user != null && user.getIdAgelio() != null) {
                return Integer.parseInt(user.getIdAgelio());
            }

            return 1; // ID par défaut - à adapter selon votre système

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la conversion username vers userId: " + e.getMessage());
            return 1; // ID par défaut en cas d'erreur
        }
    }

// ==================== MÉTHODE DE VALIDATION OPTIONNELLE ====================

    /**
     * NOUVELLE MÉTHODE: Validation des données avant sauvegarde
     */
    private void validateLigneData(LigneDemandeAchatRequestDTO dto) {
        if (dto.getRef() == null || dto.getRef().trim().isEmpty()) {
            throw new BadRequestException("La référence est obligatoire");
        }

        if (dto.getQte() == null || dto.getQte().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("La quantité doit être positive");
        }

        if (dto.getUnite() == null || dto.getUnite().trim().isEmpty()) {
            throw new BadRequestException("L'unité est obligatoire");
        }

        // Validation de la longueur des champs
        if (dto.getRef().length() > 25) {
            throw new BadRequestException("La référence ne peut pas dépasser 25 caractères");
        }

        if (dto.getDesignation() != null && dto.getDesignation().length() > 80) {
            throw new BadRequestException("La désignation ne peut pas dépasser 80 caractères");
        }
    }

    /**
     * Mapper une seule entité (pour les cas individuels)
     */
    private DemandeAchatResponseDTO mapEntityToDto(DemandeAchat entity) {
        return mapEntitiesToDtos(Collections.singletonList(entity)).get(0);
    }


    /**
     * Mapping avec données pré-chargées pour éviter les requêtes multiples
     */
    private DemandeAchatResponseDTO mapEntityToDtoWithPreloadedData(
            DemandeAchat entity,
            Map<String, String> affaireLibelles,
            Map<Integer, KdnsAccessor> accessors) {

        DemandeAchatResponseDTO dto = new DemandeAchatResponseDTO();
        dto.setId(entity.getId());
        dto.setChantier(entity.getChantier());
        dto.setDelaiSouhaite(entity.getDelaiSouhaite());
        dto.setCommentaire(entity.getCommentaire());
        dto.setLogin(entity.getLogin());
        dto.setDateDa(entity.getDateDa());
        dto.setStatut(entity.getStatut());
        dto.setStatutLabel(entity.getStatutLabel());
        dto.setNumDa(entity.getNumDa());
        dto.setSysCreationDate(entity.getSysCreationDate());
        dto.setSysCreatorId(entity.getSysCreatorId());
        dto.setSysModificationDate(entity.getSysModificationDate());

        // Libellé affaire depuis les données pré-chargées
        if (entity.getChantier() != null) {
            dto.setChantierLibelle(affaireLibelles.get(entity.getChantier()));
        }

        // Informations demandeur depuis les données pré-chargées
        if (entity.getLogin() != null && accessors.containsKey(entity.getLogin())) {
            KdnsAccessor demandeur = accessors.get(entity.getLogin());
            dto.setDemandeurNom(formatUserName(demandeur));
            dto.setDemandeurLogin(demandeur.getLogin());
        }

        // Informations créateur depuis les données pré-chargées
        if (entity.getSysCreatorId() != null && accessors.containsKey(entity.getSysCreatorId())) {
            KdnsAccessor createur = accessors.get(entity.getSysCreatorId());
            dto.setCreateurNom(formatUserName(createur));
        }

        return dto;
    }

    private String formatUserName(KdnsAccessor accessor) {
        if (accessor.getFullName() != null && !accessor.getFullName().trim().isEmpty()) {
            return accessor.getFullName();
        }
        String firstName = accessor.getFirstName() != null ? accessor.getFirstName() : "";
        String lastName = accessor.getLastName() != null ? accessor.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }

    private LigneDemandeAchatResponseDTO mapLigneToDto(LigneDemandeAchat ligne) {
        LigneDemandeAchatResponseDTO dto = new LigneDemandeAchatResponseDTO();
        dto.setIdArt(ligne.getIdArt());
        dto.setRef(ligne.getRef());
        dto.setDesignation(ligne.getDesignation());
        dto.setQte(ligne.getQte());
        dto.setUnite(ligne.getUnite());
        dto.setSysModificationDate(ligne.getSysModificationDate());
        dto.setSysUserId(ligne.getSysUserId());
        dto.setSysCreationDate(ligne.getSysCreationDate());
        return dto;
    }


    private List<DemandeAchatResponseDTO> mapEntitiesToDtos(List<DemandeAchat> demandes) {
        if (demandes.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Extraire tous les codes chantier uniques
        Set<String> chantierCodes = demandes.stream()
                .map(DemandeAchat::getChantier)
                .filter(chantier -> chantier != null && !chantier.trim().isEmpty())
                .collect(Collectors.toSet());

        // 2. Extraire tous les IDs utilisateur uniques
        Set<Integer> userIds = demandes.stream()
                .flatMap(demande -> Stream.of(
                        demande.getLogin(),
                        demande.getSysCreatorId(),
                        demande.getSysUserId()
                ))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. Charger toutes les affaires en une seule requête
        Map<String, String> affaireLibelles = new HashMap<>();
        if (!chantierCodes.isEmpty()) {
            List<String> chantierList = new ArrayList<>(chantierCodes);
            List<Affaire> affaires = affaireRepository.findByAffaireIn(chantierList);
            affaireLibelles = affaires.stream()
                    .collect(Collectors.toMap(
                            Affaire::getAffaire,
                            Affaire::getLibelle,
                            (existing, replacement) -> existing
                    ));
        }

        // 4. Charger tous les utilisateurs en une seule requête
        Map<Integer, KdnsAccessor> accessors = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<KdnsAccessor> accessorList = kdnsAccessorRepository.findAllById(userIds);
            accessors = accessorList.stream()
                    .collect(Collectors.toMap(
                            KdnsAccessor::getAccessorId,
                            Function.identity(),
                            (existing, replacement) -> existing
                    ));
        }

        // 5. Mapper toutes les demandes avec les données pré-chargées
        final Map<String, String> finalAffaireLibelles = affaireLibelles;
        final Map<Integer, KdnsAccessor> finalAccessors = accessors;

        return demandes.stream()
                .map(demande -> mapEntityToDtoWithPreloadedData(demande, finalAffaireLibelles, finalAccessors))
                .collect(Collectors.toList());
    }
}