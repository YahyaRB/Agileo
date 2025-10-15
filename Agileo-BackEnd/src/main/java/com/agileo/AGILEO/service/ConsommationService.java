package com.agileo.AGILEO.service;

import com.agileo.AGILEO.Dtos.request.*;
import com.agileo.AGILEO.Dtos.response.*;
import com.agileo.AGILEO.message.ResponseMessage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ConsommationService {

    // ==================== GESTION DES CONSOMMATIONS ====================

    /**
     * Créer une nouvelle consommation
     */
    ConsommationResponseDTO createConsommation(ConsommationRequestDTO consommationDto, String currentUsername);

    /**
     * Récupérer toutes les consommations (pour ADMIN/MANAGER/CONSULTEUR)
     */
    List<ConsommationResponseDTO> getAllConsommations();

    /**
     * Récupérer une consommation par son ID
     */
    ConsommationResponseDTO getConsommationById(Integer id);

    /**
     * Récupérer les consommations par affaire
     */
    List<ConsommationResponseDTO> getConsommationsByAffaire(Integer affaireId);

    /**
     * Récupérer les consommations de l'utilisateur courant
     */
    List<ConsommationResponseDTO> getCurrentUserConsommations(String currentUsername);

    /**
     * Récupérer les consommations par utilisateur
     */
    List<ConsommationResponseDTO> getConsommationsByUser(Integer userId);

    /**
     * Mettre à jour une consommation
     */
    ResponseMessage updateConsommation(Integer id, ConsommationRequestDTO consommationDto, String currentUsername);

    /**
     * Envoyer une consommation (changer le statut à "Envoyé")
     */
    ResponseMessage envoyerConsommation(Integer id, String currentUsername);

    /**
     * Supprimer une consommation
     */
    ResponseMessage deleteConsommation(Integer id);

    // ==================== GESTION DES ARTICLES DISPONIBLES ====================

    /**
     * Récupérer les articles en stock pour une affaire
     * (calcul basé sur réceptions - consommations)
     */
    List<ArticleStockDTO> getArticlesDisponibles(Integer affaireId);

    // ==================== GESTION DES LIGNES DE CONSOMMATION ====================

    /**
     * Ajouter des lignes de consommation (articles consommés)
     */
    ResponseMessage addLignesConsommation(Integer consommationId, List<LigneConsommationRequestDTO> lignesDto, String currentUsername);

    /**
     * Récupérer les lignes d'une consommation
     */
    List<LigneConsommationResponseDTO> getLignesConsommationByConsommationId(Integer consommationId);

    /**
     * Mettre à jour une ligne de consommation
     */
    ResponseMessage updateLigneConsommation(Integer ligneId, LigneConsommationRequestDTO ligneDto, String currentUsername);

    /**
     * Supprimer une ligne de consommation
     */
    ResponseMessage deleteLigneConsommation(Integer ligneId, String currentUsername);

    List<ConsommationResponseDTO> getConsommationsByAffaireCode(String affaireCode);
    List<ArticleStockDTO> getArticlesDisponiblesForAffaire(String affaireCode);


}