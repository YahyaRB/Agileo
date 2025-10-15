package com.agileo.AGILEO.service;

import com.agileo.AGILEO.Dtos.request.DemandeAchatRequestDTO;
import com.agileo.AGILEO.Dtos.request.LigneDemandeAchatRequestDTO;
import com.agileo.AGILEO.Dtos.response.DemandeAchatFileResponseDTO;
import com.agileo.AGILEO.Dtos.response.DemandeAchatResponseDTO;
import com.agileo.AGILEO.Dtos.response.LigneDemandeAchatResponseDTO;
import com.agileo.AGILEO.Dtos.response.PagedResponse;
import com.agileo.AGILEO.message.ResponseMessage;

import java.util.List;

public interface DemandeAchatService {

    // ==================== GESTION DES DEMANDES D'ACHAT ====================

    /**
     * Créer une nouvelle demande d'achat
     * @param demandeDto Les données de la demande à créer
     * @param currentUsername L'utilisateur qui effectue l'opération
     * @return La demande créée avec toutes les informations enrichies
     */
    DemandeAchatResponseDTO createDemandeAchat(DemandeAchatRequestDTO demandeDto, String currentUsername);

    /**
     * Mettre à jour une demande d'achat existante
     * Seules les demandes en statut "en attente" peuvent être modifiées
     * @param id L'identifiant de la demande à modifier
     * @param demandeDto Les nouvelles données de la demande
     * @param currentUsername L'utilisateur qui effectue l'opération
     * @return Message de confirmation
     */
    ResponseMessage updateDemandeAchat(Integer id, DemandeAchatRequestDTO demandeDto, String currentUsername);

    /**
     * Mettre à jour le statut d'une demande d'achat (passer à "envoyée")
     * @param demandeID L'identifiant de la demande
     * @param currentUsername L'utilisateur qui effectue l'opération
     * @return Message de confirmation
     */
    ResponseMessage updateDemandeAchatStatut(Integer demandeID, String currentUsername);

    /**
     * Changer le statut d'une demande d'achat avec un statut personnalisé
     * @param id L'identifiant de la demande
     * @param newStatus Le nouveau statut (0=en attente, 1=approuvée, -1=rejetée)
     * @param currentUsername L'utilisateur qui effectue l'opération
     * @return Message de confirmation
     */
    ResponseMessage changeDemandeStatus(Integer id, Integer newStatus, String currentUsername);

    /**
     * Supprimer une demande d'achat (marquage logique)
     * Seules les demandes en statut "en attente" peuvent être supprimées
     * @param id L'identifiant de la demande à supprimer
     * @return Message de confirmation
     */
    ResponseMessage deleteDemandeAchat(Integer id);

    // ==================== CONSULTATION DES DEMANDES D'ACHAT ====================

    /**
     * Récupérer toutes les demandes d'achat
     * @return Liste de toutes les demandes avec informations enrichies
     */
    List<DemandeAchatResponseDTO> findAllDemandes();

    //Demande d'achate liste avec pagination
    PagedResponse<DemandeAchatResponseDTO> findAllDemandesPaginated(
            int page, int size, String sortBy, String sortDirection);

    /**
     * Récupérer une demande d'achat par son identifiant
     * @param id L'identifiant de la demande
     * @return La demande avec toutes les informations enrichies
     */
    DemandeAchatResponseDTO findDemandeById(Integer id);

    /**
     * Récupérer les demandes d'achat d'un utilisateur spécifique
     * @param login L'identifiant de l'utilisateur
     * @return Liste des demandes de l'utilisateur
     */
    List<DemandeAchatResponseDTO> findDemandesByLogin(String login);

    //Les demandes d'achat d'un utilisateurs avec pagination
    PagedResponse<DemandeAchatResponseDTO> findAllDemandesByLoginPaginated(
            int page, int size, String sortBy, String sortDirection, String login);

    /**
     * Récupérer les demandes d'achat pour un chantier spécifique
     * @param chantier Le code du chantier/affaire
     * @return Liste des demandes pour ce chantier
     */
    List<DemandeAchatResponseDTO> findDemandesByChantier(String chantier);

    /**
     * Récupérer une demande d'achat par son numéro
     * @param numDa Le numéro de la demande d'achat
     * @return La demande correspondante
     */
    DemandeAchatResponseDTO findDemandeByNumDa(String numDa);

    // ==================== GESTION DES FICHIERS ====================

    /**
     * Supprimer un fichier attaché à une demande d'achat
     * @param demandeId L'identifiant de la demande
     * @param fileId L'identifiant du fichier à supprimer
     * @param currentUsername L'utilisateur qui effectue l'opération
     * @return Message de confirmation
     */
    ResponseMessage removeFileFromDemande(Integer demandeId, Integer fileId, String currentUsername);

    // ==================== GESTION DES LIGNES DE DEMANDE ====================

    /**
     * Ajouter une ligne à une demande d'achat existante
     * Seules les demandes en statut "en attente" peuvent être modifiées
     * @param demandeId L'identifiant de la demande
     * @param lignesDto Les données de la ligne à ajouter
     * @param currentUsername L'utilisateur qui effectue l'opération
     * @return Message de confirmation
     */
    ResponseMessage addLignesDemande(Integer demandeId, LigneDemandeAchatRequestDTO lignesDto, String currentUsername);

    /**
     * Récupérer toutes les lignes d'une demande d'achat
     * @param demandeId L'identifiant de la demande
     * @return Liste des lignes de la demande
     */
    List<LigneDemandeAchatResponseDTO> findLignesDemandeByDemandeId(Integer demandeId);

    /**
     * Mettre à jour une ligne de demande d'achat
     * Seules les lignes de demandes en statut "en attente" peuvent être modifiées
     * @param ligneId L'identifiant de la ligne à modifier
     * @param ligneDto Les nouvelles données de la ligne
     * @param currentUsername L'utilisateur qui effectue l'opération
     * @return Message de confirmation
     */
    ResponseMessage updateLigneDemande(Integer ligneId, LigneDemandeAchatRequestDTO ligneDto, String currentUsername);

    ResponseMessage deleteLigneDemande(Integer ligneId, String currentUsername);

    ResponseMessage updateDemandeAchatStatut(Integer demandeID, Integer newStatut, String currentUsername);
    public List<DemandeAchatFileResponseDTO> getDemandeFiles(Integer demandeId);
}