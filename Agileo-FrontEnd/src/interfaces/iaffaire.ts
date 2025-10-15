export interface Affaire {
  // Propriétés principales de M6003AffaireUtilisateur
  numero?: number;
  accessoirId?: number;
  affaire?: string; // Code de l'affaire
  libelle?: string; // Libellé depuis la vue Affaires
  commentaire?: string;

  // Champs système
  sysCreationDate?: Date | string;
  sysCreatorId?: number;
  sysModificationDate?: Date | string;
  sysUserId?: number;
  sysSynchronizationDate?: Date | string;
  sysState?: number; // Statut système (1 = actif, 0 = inactif)

  // Informations utilisateur (depuis KdnsAccessor)
  creatorFullName?: string;
  userFullName?: string;

  // Propriétés pour rétrocompatibilité avec l'ancienne interface
  id?: number; // Mapping vers numero
  code?: string; // Mapping vers affaire
  nom?: string; // Mapping vers libelle
  statut?: number; // Mapping vers sysState
}

// Interface pour les détails complets d'une affaire
export interface AffaireDetails {
  affaire: string;
  libelle: string;
  assignedUsers: UserAssignment[];
  totalUsers: number;
}

// Interface pour les assignations utilisateur
export interface UserAssignment {
  accessoirId: number;
  fullName?: string;
  login?: string;
  email?: string;
  commentaire?: string;
  assignmentDate?: Date | string;
  assignedBy?: number;
  assignedByName?: string;
}

// Interface pour les statistiques des affaires
export interface AffaireStats {
  totalAffaires: number;
  totalAssignments: number;
  totalActiveUsers: number;
  totalInactiveAssignments: number;
  affaireMostUsed?: {
    affaire: string;
    libelle: string;
    assignmentCount: number;
  };
  userMostAssigned?: {
    accessoirId: number;
    fullName: string;
    assignmentCount: number;
  };
}

// Interface pour les assignations utilisateur-affaire
export interface AffaireUserAssignment {
  accessoirId: number;
  affaire: string;
  libelle: string;
  userFullName?: string;
  userLogin?: string;
  userEmail?: string;
}

// Interface pour les requêtes d'affaire
export interface AffaireRequest {
  accessoirId?: number;
  affaire?: string;
  commentaire?: string;
}
