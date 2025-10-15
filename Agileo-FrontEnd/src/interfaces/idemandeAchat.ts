  // idemandeAchat.ts
export interface IDemandeAchat {
  // Champs obligatoires selon la table M6000_DemandeAchat
  id?: number;
  chantier: string; // VARCHAR(8), REQUIS
  delaiSouhaite: string; // DATETIME, format ISO string

  // Champs optionnels
  commentaire?: string; // VARCHAR(8000) - champ 'comm' dans la table
  login?: number; // INTEGER - référence utilisateur
  dateDa?: string; // DATETIME - date de création de la demande
  statut?: number; // INTEGER - 0=brouillon, 1=envoyé, -1=rejeté
  numDa?: string; // VARCHAR(100) - numéro de demande généré
  dsDivalto?: string; // VARCHAR(40)
  pjDa?: number; // INTEGER - nombre de pièces jointes

  // Champs système (gérés automatiquement)
  sysCreationDate?: string;
  sysCreatorId?: number;
  sysModificationDate?: string;
  sysUserId?: number;
  sysSynchronizationDate?: string;
  sysState?: number; // 0=actif, -1=supprimé

  // NOUVEAUX CHAMPS POUR L'AFFICHAGE
  chantierLibelle?: string; // Libellé de l'affaire
  demandeurNom?: string; // Nom complet du demandeur
  demandeurLogin?: string; // Login du demandeur
  createurNom?: string; // Nom du créateur

  // Champs calculés pour l'affichage
  statutLabel?: string;
  active?: boolean;
  pending?: boolean;
  approved?: boolean;
  rejected?: boolean;
  urgent?: boolean;

  // Informations utilisateur pour l'affichage
  createdByUsername?: string;
  modifiedByUsername?: string;

  // Nombre de lignes associées
  nombreLignes?: number;
  nbLignes?: number; // Alias pour compatibilité

  // Champs legacy pour compatibilité avec l'ancien code
  affaireId?: number; // À mapper depuis 'chantier'
  affaireCode?: string; // Alias de 'chantier'
  dateCreation?: string; // Alias de 'sysCreationDate'
  createdBy?: string; // Alias de 'createdByUsername'
  delai?: string; // Alias de 'delaiSouhaite'
}
  export interface IDemandeAchatFile {
    fileId: number;
    name: string;
    extension: string;
    fullFileName: string;
    size: number;
    sizeFormatted: string;
    alt?: string;
    uploadDate: string; // ISO date string
    uploadedBy: string;
    uploadedByNom: string;
    downloadUrl: string;
    nbOpen: number;
    canDelete: boolean;
    canDownload: boolean;
    category?: string;
    documentType: string;
  }
