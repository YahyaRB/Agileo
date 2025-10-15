package com.agileo.AGILEO.service;

import com.agileo.AGILEO.Dtos.request.*;
import com.agileo.AGILEO.Dtos.response.*;
import com.agileo.AGILEO.message.ResponseMessage;

import java.math.BigDecimal;
import java.util.List;

public interface ReceptionService {

    // ==================== GESTION DES RÉCEPTIONS ====================

    /**
     * Créer une nouvelle réception
     */
    ReceptionResponseDTO createReception(ReceptionRequestDTO receptionDto, String currentUsername);

    /**
     * Récupérer toutes les réceptions (pour ADMIN/MANAGER/CONSULTEUR)
     */
    List<ReceptionResponseDTO> getAllReceptions();



    //Liste des receptions avec pagination
    PagedResponse<ReceptionResponseDTO> getAllReceptionsPaginated(
            int page, int size, String sortBy, String sortDirection, String search);

    /**
     * Récupérer une réception par son ID
     */
    ReceptionResponseDTO getReceptionById(Integer id);

    /**
     * Récupérer les réceptions par affaire
     */
    List<ReceptionResponseDTO> getReceptionsByAffaire(Integer affaireId);

    /**
     * Récupérer les réceptions de l'utilisateur courant
     */
    List<ReceptionResponseDTO> getCurrentUserReceptions(String currentUsername);

    PagedResponse<ReceptionResponseDTO> getCurrentUserReceptionsPaginated(
            int page, int size, String sortBy, String sortDirection, String currentUsername, String search);
    /**
     * Mettre à jour une réception
     */
    ResponseMessage updateReception(Integer id, ReceptionRequestDTO receptionDto, String currentUsername);

    /**
     * Supprimer une réception
     */
    ResponseMessage deleteReception(Integer id);

    // ==================== GESTION DES ARTICLES DISPONIBLES ====================

    /**
     * Récupérer les articles disponibles pour une réception
     * (basé sur les bons de commande de l'affaire et le fournisseur)
     */
    List<ArticleDisponibleDTO> getArticlesDisponibles(Integer receptionId);

    /**
     * Valider la quantité d'un article avant ajout/modification
     */
    ValidationQuantiteDTO validerQuantiteArticle(Integer receptionId, String referenceArticle,
                                                 BigDecimal quantiteDemandee, Integer ligneReceptionId);

    // ==================== GESTION DES LIGNES DE RÉCEPTION ====================

    /**
     * Ajouter des lignes de réception (articles reçus)
     */
    ResponseMessage addLignesReception(Integer receptionId, List<LigneReceptionRequestDTO> lignesDto, String currentUsername);

    /**
     * Récupérer les lignes d'une réception
     */
    List<LigneReceptionResponseDTO> getLignesReceptionByReceptionId(Integer receptionId);

    /**
     * Mettre à jour une ligne de réception
     */
    ResponseMessage updateLigneReception(Integer ligneId, LigneReceptionRequestDTO ligneDto, String currentUsername);

    /**
     * Supprimer une ligne de réception
     */
    ResponseMessage deleteLigneReception(Integer ligneId, String currentUsername);

    /**
     * Récupérer les fichiers associés à une réception
     */
    List<DemandeAchatFileResponseDTO> getReceptionFiles(Integer receptionId);
}