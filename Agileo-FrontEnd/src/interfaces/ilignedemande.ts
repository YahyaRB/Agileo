export interface ILigneDemande {
  // Champs backend (LigneDemandeAchatResponseDTO)
  idArt?: number; // ID principal du backend
  ref?: string; // Référence article
  designation?: string; // Désignation article
  qte?: number; // Quantité
  unite?: string; // Unité
  sysModificationDate?: string;
  sysUserId?: number;
  sysCreationDate?: string;

  // Champs frontend (pour compatibilité)
  id?: number; // Alias de idArt
  quantite?: number; // Alias de qte
  designationArticle?: string; // Alias de designation
  referenceArticle?: string; // Alias de ref

  // Champs additionnels pour les familles statistiques
  familleStatistique1?: string;
  familleStatistique2?: string;
  familleStatistique3?: string;
  familleStatistique4?: string;
}
