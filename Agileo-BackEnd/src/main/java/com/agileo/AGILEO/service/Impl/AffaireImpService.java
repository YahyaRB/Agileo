package com.agileo.AGILEO.service.Impl;

import com.agileo.AGILEO.Dtos.response.AffaireDetailsDTO;
import com.agileo.AGILEO.Dtos.response.AffaireResponseDTO;
import com.agileo.AGILEO.Dtos.response.AffaireStatsDTO;
import com.agileo.AGILEO.Dtos.response.AffaireUserAssignmentDTO;
import com.agileo.AGILEO.entity.primary.*;
import com.agileo.AGILEO.exception.ResourceNotFoundException;
import com.agileo.AGILEO.message.ResponseMessage;
import com.agileo.AGILEO.repository.primary.*;
import com.agileo.AGILEO.service.AffaireService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AffaireImpService implements AffaireService {

    private final M6003AffaireUtilisateurRepository m6003Repository;
    private final AffaireLieUserRepository affaireLieUserRepository;
    private final AffaireRepository affaireRepository;
    private final KdnsAccessorRepository kdnsAccessorRepository;

    public AffaireImpService(M6003AffaireUtilisateurRepository m6003Repository,
                             AffaireLieUserRepository affaireLieUserRepository,
                             AffaireRepository affaireRepository,
                             KdnsAccessorRepository kdnsAccessorRepository) {
        this.m6003Repository = m6003Repository;
        this.affaireLieUserRepository = affaireLieUserRepository;
        this.affaireRepository = affaireRepository;
        this.kdnsAccessorRepository = kdnsAccessorRepository;
    }

    /**
     * Récupérer toutes les affaires - Lecture depuis la table Affaire
     */
    @Transactional(readOnly = true)
    public List<AffaireResponseDTO> findAllAffaires() {
        try {
            List<Affaire> affaires = affaireRepository.findAll();

            if (affaires == null || affaires.isEmpty()) {
                return new ArrayList<>();
            }

            // Conversion SIMPLE sans requêtes supplémentaires
            return affaires.stream()
                    .map(this::convertFromAffaireToResponseDTOSimple)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des affaires: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des affaires", e);
        }
    }

    /**
     * Récupérer une affaire par ID - Lecture depuis M6003 pour les détails complets
     */
    @Transactional(readOnly = true)
    public AffaireResponseDTO findAffaireById(Long id) {
        M6003AffaireUtilisateur entity = m6003Repository.findById(id.intValue())
                .orElseThrow(() -> new ResourceNotFoundException("Affaire non trouvée avec l'ID: " + id));

        return convertToResponseDTO(entity);
    }

    /**
     * Récupérer affaires par code - Lecture depuis la vue
     */
    @Transactional(readOnly = true)
    public List<AffaireResponseDTO> findAffairesByCode(String code) {
        Optional<Affaire> affaires = affaireRepository.findByAffaire(code);

        if (!affaires.isPresent()) {
            return Collections.emptyList();
        }

        return Collections.singletonList(
                convertFromAffaireToResponseDTOSimple(affaires.get())
        );
    }


    /**
     * Récupérer affaires par statut - Lecture depuis M6003
     */
    @Transactional(readOnly = true)
    public List<AffaireResponseDTO> findAffairesByStatut(int statut) {
        List<M6003AffaireUtilisateur> entities = m6003Repository.findBySysState(statut);

        return entities.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * NOUVELLE MÉTHODE : Récupérer les affaires d'un KdnsAccessor par ID
     */
    @Transactional(readOnly = true)
    public List<AffaireResponseDTO> findAffairesByAccessorId(Integer accessorId) {
        if (!kdnsAccessorRepository.existsById(accessorId)) {
            throw new ResourceNotFoundException("Accessor non trouvé avec l'ID: " + accessorId);
        }

        List<M6003AffaireUtilisateur> userAffaires = m6003Repository.findByAccessoirId(accessorId);

        if (userAffaires.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> affaireCodes = userAffaires.stream()
                .map(M6003AffaireUtilisateur::getAffaire)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (affaireCodes.isEmpty()) {
            return new ArrayList<>();
        }

        List<Affaire> affaires = affaireRepository.findByAffaireIn(new ArrayList<>(affaireCodes));

        return affaires.stream()
                .map(this::convertFromAffaireToResponseDTOSimple)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AffaireResponseDTO> findAffairesByAccessorLogin(String login) {
        // Trouver l'accessor par login
        KdnsAccessor accessor = kdnsAccessorRepository.findByLogin(login)
                .orElseThrow(() -> new ResourceNotFoundException("Accessor non trouvé avec le login: " + login));

        return findAffairesByAccessorId(accessor.getAccessorId());
    }

    /**
     * NOUVELLE MÉTHODE : Compter les affaires d'un accessor
     */
    @Transactional(readOnly = true)
    public Long countAffairesByAccessorId(Integer accessorId) {
        // Vérifier que l'accessor existe
        kdnsAccessorRepository.findById(accessorId)
                .orElseThrow(() -> new ResourceNotFoundException("Accessor non trouvé avec l'ID: " + accessorId));

        return (long) affaireLieUserRepository.findByAccessoirId(accessorId).size();
    }

    /**
     * NOUVELLE MÉTHODE : Récupérer les affaires actives d'un accessor
     */
    @Transactional(readOnly = true)
    public List<AffaireResponseDTO> findActiveAffairesByAccessorId(Integer accessorId) {
        // Vérifier que l'accessor existe
        kdnsAccessorRepository.findById(accessorId)
                .orElseThrow(() -> new ResourceNotFoundException("Accessor non trouvé avec l'ID: " + accessorId));

        // Récupérer toutes les affaires de l'accessor et filtrer par statut actif
        List<AffaireLieUser> accessorAffaires = affaireLieUserRepository.findByAccessoirId(accessorId);

        return accessorAffaires.stream()
                .map(this::convertFromViewToResponseDTOSafe)
                .filter(Objects::nonNull)
                .filter(dto -> {
                    // Vérifier le statut actif dans M6003
                    Optional<M6003AffaireUtilisateur> m6003 = m6003Repository
                            .findByAffaireAndAccessoirId(dto.getAccessoirId());
                    return m6003.isPresent() && Integer.valueOf(1).equals(m6003.get().getSysState());
                })
                .collect(Collectors.toList());
    }

    public ResponseMessage addAccessorToAffaire(String affaireCode, Integer accessorId, String adminLogin) {
        // Vérifier que l'affaire existe
        affaireRepository.findByAffaire(affaireCode)
                .orElseThrow(() -> new ResourceNotFoundException("Affaire non trouvée: " + affaireCode));

        // Vérifier que l'accessor existe
        kdnsAccessorRepository.findById(accessorId)
                .orElseThrow(() -> new ResourceNotFoundException("Accessor non trouvé: " + accessorId));

        // Récupérer l'admin
        KdnsAccessor admin = kdnsAccessorRepository.findByLogin(adminLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Admin non trouvé: " + adminLogin));

        // Vérifier si l'association existe déjà
        Optional<M6003AffaireUtilisateur> existing =
                m6003Repository.findByAffaireAndAccessoirId(affaireCode, accessorId);

        if (existing.isPresent()) {
            throw new IllegalArgumentException("L'accessor est déjà assigné à cette affaire");
        }

        // Créer la nouvelle association
        M6003AffaireUtilisateur entity = new M6003AffaireUtilisateur();
        entity.setAffaire(affaireCode);
        entity.setAccessoirId(accessorId);
        entity.setSysCreatorId(admin.getAccessorId());
        entity.setSysUserId(admin.getAccessorId());
        entity.setSysCreationDate(LocalDateTime.now());
        entity.setSysModificationDate(LocalDateTime.now());
        entity.setSysState(1);
        m6003Repository.save(entity);

        return new ResponseMessage("Accessor ajouté à l'affaire avec succès");
    }


    /**
     * MÉTHODE MODIFIÉE : Retirer un accessor d'une affaire
     */
    public ResponseMessage removeAccessorFromAffaire(String affaireCode, Integer accessorId, String adminLogin) {
        m6003Repository.findByAffaireAndAccessoirId(affaireCode, accessorId)
                .ifPresent(m6003Repository::delete);
        return new ResponseMessage("Accessor retiré de l'affaire avec succès");
    }


    /**
     * Rechercher des affaires par mot-clé
     */
    @Transactional(readOnly = true)
    public List<AffaireResponseDTO> searchAffaires(String keyword) {
        List<AffaireLieUser> results = affaireLieUserRepository.searchByKeyword(keyword);

        return results.stream()
                .map(this::convertFromViewToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les utilisateurs d'une affaire
     */
    @Transactional(readOnly = true)
    public Set<String> getAffaireUsers(String affaireCode) {
        List<AffaireLieUser> affaires = affaireLieUserRepository.findByAffaire(affaireCode);

        return affaires.stream()
                .map(a -> kdnsAccessorRepository.findById(a.getAccessoirId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(KdnsAccessor::getFullName)
                .collect(Collectors.toSet());
    }

    /**
     * Changer le statut d'une affaire
     */
    public ResponseMessage changeAffaireStatus(Long id, int newStatus, String adminLogin) {
        M6003AffaireUtilisateur entity = m6003Repository.findById(id.intValue())
                .orElseThrow(() -> new ResourceNotFoundException("Affaire non trouvée: " + id));

        KdnsAccessor admin = kdnsAccessorRepository.findByLogin(adminLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Admin non trouvé: " + adminLogin));

        entity.setSysState(newStatus);
        entity.setSysUserId(admin.getAccessorId());
        entity.setSysModificationDate(LocalDateTime.now());

        m6003Repository.save(entity);

        return new ResponseMessage("Statut de l'affaire modifié avec succès");
    }

    /**
     * MÉTHODE MODIFIÉE : Récupérer les assignations d'un accessor spécifique
     */
    @Transactional(readOnly = true)
    public List<AffaireUserAssignmentDTO> getAccessorAssignments(Integer accessorId) {
        // Vérifier que l'accessor existe
        kdnsAccessorRepository.findById(accessorId)
                .orElseThrow(() -> new ResourceNotFoundException("Accessor non trouvé: " + accessorId));

        List<AffaireLieUser> assignments = affaireLieUserRepository.findByAccessoirId(accessorId);

        return assignments.stream()
                .map(this::convertToUserAssignmentDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer toutes les assignations utilisateur-affaire
     */
    @Transactional(readOnly = true)
    public List<AffaireUserAssignmentDTO> getAllUserAssignments() {
        List<AffaireLieUser> assignments = affaireLieUserRepository.findAll();

        return assignments.stream()
                .map(this::convertToUserAssignmentDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les détails complets d'une affaire avec ses utilisateurs
     */
    @Transactional(readOnly = true)
    public AffaireDetailsDTO getAffaireDetails(String affaireCode) {
        // Vérifier que l'affaire existe
        Affaire affaire = affaireRepository.findByAffaire(affaireCode)
                .orElseThrow(() -> new ResourceNotFoundException("Affaire non trouvée: " + affaireCode));

        AffaireDetailsDTO details = new AffaireDetailsDTO();
        details.setAffaire(affaire.getAffaire());
        details.setLibelle(affaire.getLibelle());

        // Récupérer toutes les assignations depuis la vue
        List<AffaireLieUser> assignments = affaireLieUserRepository.findByAffaire(affaireCode);

        List<AffaireDetailsDTO.UserAssignmentDTO> userAssignments = assignments.stream()
                .map(assignment -> {
                    AffaireDetailsDTO.UserAssignmentDTO userDto = new AffaireDetailsDTO.UserAssignmentDTO();
                    userDto.setAccessoirId(assignment.getAccessoirId());

                    // Récupérer les détails accessor
                    kdnsAccessorRepository.findById(assignment.getAccessoirId())
                            .ifPresent(accessor -> {
                                userDto.setFullName(accessor.getFullName());
                                userDto.setLogin(accessor.getLogin());
                                userDto.setEmail(accessor.getEmail());
                            });

                    // Récupérer les détails M6003 pour les infos d'assignation
                    m6003Repository.findByAffaireAndAccessoirId(assignment.getAccessoirId())
                            .ifPresent(m6003 -> {
                                userDto.setCommentaire(m6003.getCommentaire());
                                userDto.setAssignmentDate(m6003.getSysCreationDate());
                                userDto.setAssignedBy(m6003.getSysCreatorId());

                                // Récupérer le nom de l'assignateur
                                kdnsAccessorRepository.findById(m6003.getSysCreatorId())
                                        .ifPresent(creator -> userDto.setAssignedByName(creator.getFullName()));
                            });

                    return userDto;
                })
                .collect(Collectors.toList());

        details.setAssignedUsers(userAssignments);
        details.setTotalUsers(userAssignments.size());

        return details;
    }

    /**
     * Récupérer les statistiques des affaires
     */
    @Transactional(readOnly = true)
    public AffaireStatsDTO getAffaireStats() {
        AffaireStatsDTO stats = new AffaireStatsDTO();

        // Statistiques globales
        stats.setTotalAffaires((long) affaireRepository.findAll().size());
        stats.setTotalAssignments((long) affaireLieUserRepository.findAll().size());

        // Compter les accessors actifs
        List<Integer> activeAccessorIds = affaireLieUserRepository.findAll().stream()
                .map(AffaireLieUser::getAccessoirId)
                .distinct()
                .collect(Collectors.toList());
        stats.setTotalActiveUsers((long) activeAccessorIds.size());

        // Assignations inactives (état != 1 dans M6003)
        long inactiveCount = m6003Repository.findAll().stream()
                .filter(m6003 -> m6003.getSysState() == null || !m6003.getSysState().equals(1))
                .count();
        stats.setTotalInactiveAssignments(inactiveCount);

        // Affaire la plus utilisée
        Map<String, Long> affaireUsageCount = affaireLieUserRepository.findAll().stream()
                .collect(Collectors.groupingBy(AffaireLieUser::getAffaire, Collectors.counting()));

        affaireUsageCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> {
                    AffaireStatsDTO.AffaireMostUsed mostUsed = new AffaireStatsDTO.AffaireMostUsed();
                    mostUsed.setAffaire(entry.getKey());
                    mostUsed.setAssignmentCount(entry.getValue());

                    // Récupérer le libellé
                    affaireRepository.findByAffaire(entry.getKey())
                            .ifPresent(affaire -> mostUsed.setLibelle(affaire.getLibelle()));

                    stats.setAffaireMostUsed(mostUsed);
                });

        // Accessor le plus assigné
        Map<Integer, Long> accessorAssignmentCount = affaireLieUserRepository.findAll().stream()
                .collect(Collectors.groupingBy(AffaireLieUser::getAccessoirId, Collectors.counting()));

        accessorAssignmentCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> {
                    AffaireStatsDTO.UserMostAssigned mostAssigned = new AffaireStatsDTO.UserMostAssigned();
                    mostAssigned.setAccessoirId(entry.getKey());
                    mostAssigned.setAssignmentCount(entry.getValue());

                    // Récupérer le nom
                    kdnsAccessorRepository.findById(entry.getKey())
                            .ifPresent(accessor -> mostAssigned.setFullName(accessor.getFullName()));

                    stats.setUserMostAssigned(mostAssigned);
                });

        return stats;
    }

    /**
     * Valider qu'un code d'affaire existe
     */
    @Transactional(readOnly = true)
    public Boolean isValidAffaireCode(String code) {
        return affaireRepository.findByAffaire(code).isPresent();
    }

    /**
     * MÉTHODE MODIFIÉE : Vérifier si un accessor peut être assigné à une affaire
     */
    @Transactional(readOnly = true)
    public Boolean canAssignAccessorToAffaire(String affaireCode, Integer accessorId) {
        // Vérifier que l'affaire existe
        if (!affaireRepository.findByAffaire(affaireCode).isPresent()) {
            return false;
        }

        // Vérifier que l'accessor existe et est actif
        Optional<KdnsAccessor> accessor = kdnsAccessorRepository.findById(accessorId);
        if (!accessor.isPresent()) {
            return false;
        }

        // Vérifier si l'accessor a une date de fin de validité
        KdnsAccessor kdnsAccessor = accessor.get();
        if (kdnsAccessor.getEndValid() != null && kdnsAccessor.getEndValid().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Vérifier si l'assignation n'existe pas déjà
        return !m6003Repository.findByAffaireAndAccessoirId(accessorId).isPresent();
    }

    // ================ MÉTHODES HELPER PRIVÉES ================

    /**
     * NOUVELLE MÉTHODE : Conversion depuis Affaire (table simple) vers DTO
     * Pour gérer les cas où on lit directement depuis la table Affaires
     */
    private AffaireResponseDTO convertFromAffaireToResponseDTO(Affaire affaire) {
        AffaireResponseDTO dto = new AffaireResponseDTO();
        dto.setAffaire(affaire.getAffaire());
        dto.setLibelle(affaire.getLibelle());

        // Pour la compatibilité avec l'interface frontend
        dto.setAccessoirId(null);
        dto.setNumero(null);
        dto.setCommentaire("Affaire disponible");
        dto.setSysCreationDate(null);
        dto.setSysCreatorId(null);
        dto.setSysModificationDate(null);
        dto.setSysUserId(null);
        dto.setSysSynchronizationDate(null);
        dto.setSysState(1); // Par défaut actif
        dto.setCreatorFullName(null);
        dto.setUserFullName(null);

        // Optionnel : Enrichir avec le nombre d'assignations existantes
        try {
            List<AffaireLieUser> assignments = affaireLieUserRepository.findByAffaire(affaire.getAffaire());
            if (!assignments.isEmpty()) {
                dto.setCommentaire("Affaire avec " + assignments.size() + " assignation(s)");
            }
        } catch (Exception e) {
            // En cas d'erreur, on continue avec les valeurs par défaut
            System.err.println("Erreur lors du calcul des assignations pour l'affaire " + affaire.getAffaire() + ": " + e.getMessage());
        }

        return dto;
    }

    /**
     * Version sécurisée de la conversion depuis AffaireLieUser vers DTO
     */
    private AffaireResponseDTO convertFromViewToResponseDTOSafe(AffaireLieUser viewEntity) {
        try {
            if (viewEntity == null) {
                return null;
            }

            AffaireResponseDTO dto = new AffaireResponseDTO();
            dto.setAccessoirId(viewEntity.getAccessoirId());
            dto.setAffaire(viewEntity.getAffaire());
            dto.setLibelle(viewEntity.getLibelle());

            // Récupérer les détails complets depuis M6003 si nécessaire - avec gestion d'erreur
            try {
                if (viewEntity.getAffaire() != null && viewEntity.getAccessoirId() != null) {
                    Optional<M6003AffaireUtilisateur> m6003Opt = m6003Repository
                            .findByAffaireAndAccessoirId(viewEntity.getAccessoirId());

                    if (m6003Opt.isPresent()) {
                        M6003AffaireUtilisateur m6003 = m6003Opt.get();
                        dto.setNumero(m6003.getNumero());
                        dto.setCommentaire(m6003.getCommentaire());
                        dto.setSysCreationDate(m6003.getSysCreationDate());
                        dto.setSysCreatorId(m6003.getSysCreatorId());
                        dto.setSysModificationDate(m6003.getSysModificationDate());
                        dto.setSysUserId(m6003.getSysUserId());
                        dto.setSysSynchronizationDate(m6003.getSysSynchronizationDate());
                        dto.setSysState(m6003.getSysState());
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la récupération des détails M6003 pour l'affaire "
                        + viewEntity.getAffaire() + ": " + e.getMessage());
                // Continue sans les détails M6003
            }

            // Récupérer les noms d'utilisateur - avec gestion d'erreur
            try {
                if (viewEntity.getAccessoirId() != null) {
                    kdnsAccessorRepository.findById(viewEntity.getAccessoirId())
                            .ifPresent(accessor -> dto.setUserFullName(accessor.getFullName()));
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la récupération du nom accessor "
                        + viewEntity.getAccessoirId() + ": " + e.getMessage());
                // Continue sans le nom d'utilisateur
            }

            return dto;

        } catch (Exception e) {
            System.err.println("Erreur lors de la conversion DTO pour: " +
                    (viewEntity != null ? viewEntity.getAffaire() : "null") + " - " + e.getMessage());
            return null; // Sera filtré par filter(Objects::nonNull)
        }
    }

    /**
     * Conversion depuis M6003AffaireUtilisateur vers DTO
     */
    private AffaireResponseDTO convertToResponseDTO(M6003AffaireUtilisateur entity) {
        AffaireResponseDTO dto = new AffaireResponseDTO();
        dto.setNumero(entity.getNumero());
        dto.setAccessoirId(entity.getAccessoirId());
        dto.setAffaire(entity.getAffaire());
        dto.setCommentaire(entity.getCommentaire());
        dto.setSysCreationDate(entity.getSysCreationDate());
        dto.setSysCreatorId(entity.getSysCreatorId());
        dto.setSysModificationDate(entity.getSysModificationDate());
        dto.setSysUserId(entity.getSysUserId());
        dto.setSysSynchronizationDate(entity.getSysSynchronizationDate());
        dto.setSysState(entity.getSysState());

        // Récupérer le libellé depuis la vue Affaires
        if (entity.getAffaire() != null) {
            affaireRepository.findByAffaire(entity.getAffaire())
                    .ifPresent(affaire -> dto.setLibelle(affaire.getLibelle()));
        }

        // Récupérer les noms des accessors
        if (entity.getSysCreatorId() != null) {
            kdnsAccessorRepository.findById(entity.getSysCreatorId())
                    .ifPresent(creator -> dto.setCreatorFullName(creator.getFullName()));
        }

        if (entity.getSysUserId() != null) {
            kdnsAccessorRepository.findById(entity.getSysUserId())
                    .ifPresent(accessor -> dto.setUserFullName(accessor.getFullName()));
        }

        return dto;
    }

    /**
     * Conversion depuis AffaireLieUser vers DTO
     */
    private AffaireResponseDTO convertFromViewToResponseDTO(AffaireLieUser viewEntity) {
        AffaireResponseDTO dto = new AffaireResponseDTO();
        dto.setAccessoirId(viewEntity.getAccessoirId());
        dto.setAffaire(viewEntity.getAffaire());
        dto.setLibelle(viewEntity.getLibelle());

        // Récupérer les détails complets depuis M6003 si nécessaire
        m6003Repository.findByAffaireAndAccessoirId(viewEntity.getAccessoirId())
                .ifPresent(m6003 -> {
                    dto.setNumero(m6003.getNumero());
                    dto.setCommentaire(m6003.getCommentaire());
                    dto.setSysCreationDate(m6003.getSysCreationDate());
                    dto.setSysCreatorId(m6003.getSysCreatorId());
                    dto.setSysModificationDate(m6003.getSysModificationDate());
                    dto.setSysUserId(m6003.getSysUserId());
                    dto.setSysSynchronizationDate(m6003.getSysSynchronizationDate());
                    dto.setSysState(m6003.getSysState());
                });

        // Récupérer les noms d'accessor
        if (viewEntity.getAccessoirId() != null) {
            kdnsAccessorRepository.findById(viewEntity.getAccessoirId())
                    .ifPresent(accessor -> dto.setUserFullName(accessor.getFullName()));
        }

        return dto;
    }

    /**
     * Conversion vers AffaireUserAssignmentDTO
     */
    private AffaireUserAssignmentDTO convertToUserAssignmentDTO(AffaireLieUser assignment) {
        AffaireUserAssignmentDTO dto = new AffaireUserAssignmentDTO();
        dto.setAccessoirId(assignment.getAccessoirId());
        dto.setAffaire(assignment.getAffaire());
        dto.setLibelle(assignment.getLibelle());

        // Récupérer les détails accessor
        kdnsAccessorRepository.findById(assignment.getAccessoirId())
                .ifPresent(accessor -> {
                    dto.setUserFullName(accessor.getFullName());
                    dto.setUserLogin(accessor.getLogin());
                    dto.setUserEmail(accessor.getEmail());
                });

        return dto;
    }
    private AffaireResponseDTO convertFromAffaireToResponseDTOSimple(Affaire affaire) {
        AffaireResponseDTO dto = new AffaireResponseDTO();
        dto.setAffaire(affaire.getAffaire());
        dto.setLibelle(affaire.getLibelle());

        dto.setAccessoirId(null);
        dto.setNumero(null);
        dto.setCommentaire("Affaire disponible");
        dto.setSysCreationDate(null);
        dto.setSysCreatorId(null);
        dto.setSysModificationDate(null);
        dto.setSysUserId(null);
        dto.setSysSynchronizationDate(null);
        dto.setSysState(1); // Par défaut actif
        dto.setCreatorFullName(null);
        dto.setUserFullName(null);

        return dto;
}
}