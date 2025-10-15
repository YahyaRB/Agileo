export interface ICommande {
  id?: number;
  affaireId: number;
  userId?: number;
  dateReception?: string;
  referenceBl?: string;
  dateBl?: string;
  refFournisseur?: string;
  nomFournisseur?: string;
  idAgelio?: number;
  statut?: string;
  affaireCode?: string;
  affaireLibelle?: string;
  userLogin?: string;
  createdBy?: string;
  createdDate?: string;
  commande: number;
  ce: string;
  fournisseurId: string;
  fournisseur: string;
  affaireName: string;
  dateCommande: string;
  votreReference: string;
  piece: string;
}
