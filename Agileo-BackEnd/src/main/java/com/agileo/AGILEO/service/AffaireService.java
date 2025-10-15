package com.agileo.AGILEO.service;

import com.agileo.AGILEO.Dtos.request.AffaireRequestDTO;
import com.agileo.AGILEO.Dtos.response.AffaireDetailsDTO;
import com.agileo.AGILEO.Dtos.response.AffaireResponseDTO;
import com.agileo.AGILEO.Dtos.response.AffaireStatsDTO;
import com.agileo.AGILEO.Dtos.response.AffaireUserAssignmentDTO;
import com.agileo.AGILEO.message.ResponseMessage;

import java.util.List;
import java.util.Set;

public interface AffaireService {

    // ================ MÉTHODES DE LECTURE ================

    /**
     * Récupérer toutes les affaires
     */
    List<AffaireResponseDTO> findAllAffaires();

    /**
     * Récupérer une affaire par ID
     */
    AffaireResponseDTO findAffaireById(Long id);

    /**
     * Récupérer les affaires par code
     */
    List<AffaireResponseDTO> findAffairesByCode(String code);

    /**
     * Récupérer les affaires par statut
     */
    List<AffaireResponseDTO> findAffairesByStatut(int statut);

    /**
     * Rechercher des affaires par mot-clé
     */
    List<AffaireResponseDTO> searchAffaires(String keyword);

    // ================ MÉTHODES SPÉCIFIQUES AUX KDNSACCESSOR ================

    /**
     * Récupérer les affaires d'un KdnsAccessor par ID
     */
    List<AffaireResponseDTO> findAffairesByAccessorId(Integer accessorId);

    /**
     * Récupérer les affaires d'un accessor par login
     */
    List<AffaireResponseDTO> findAffairesByAccessorLogin(String login);

    /**
     * Compter les affaires d'un accessor
     */
    Long countAffairesByAccessorId(Integer accessorId);

    /**
     * Récupérer les affaires actives d'un accessor
     */
    List<AffaireResponseDTO> findActiveAffairesByAccessorId(Integer accessorId);

    // ================ MÉTHODES DE GESTION DES ASSIGNATIONS ================

    /**
     * Ajouter un accessor à une affaire
     */
    ResponseMessage addAccessorToAffaire(String affaireCode, Integer accessorId, String adminLogin);

    /**
     * Retirer un accessor d'une affaire
     */
    ResponseMessage removeAccessorFromAffaire(String affaireCode, Integer accessorId, String adminLogin);

    /**
     * Récupérer les utilisateurs assignés à une affaire
     */
    Set<String> getAffaireUsers(String affaireCode);

    /**
     * Changer le statut d'une affaire
     */
    ResponseMessage changeAffaireStatus(Long id, int newStatus, String adminLogin);

    // ================ MÉTHODES D'INFORMATION ET STATISTIQUES ================

    /**
     * Récupérer les détails complets d'une affaire
     */
    AffaireDetailsDTO getAffaireDetails(String affaireCode);

    /**
     * Récupérer toutes les assignations
     */
    List<AffaireUserAssignmentDTO> getAllUserAssignments();

    /**
     * Récupérer les assignations d'un accessor spécifique
     */
    List<AffaireUserAssignmentDTO> getAccessorAssignments(Integer accessorId);

    /**
     * Récupérer les statistiques des affaires
     */
    AffaireStatsDTO getAffaireStats();

    // ================ MÉTHODES DE VALIDATION ================

    /**
     * Valider qu'un code d'affaire existe
     */
    Boolean isValidAffaireCode(String code);

    /**
     * Vérifier si un accessor peut être assigné à une affaire
     */
    Boolean canAssignAccessorToAffaire(String affaireCode, Integer accessorId);
}