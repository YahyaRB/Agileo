import {ILigneConsommation} from "./iligneconsommation";

export interface IConsommation {
  id: number;
  dateConsommation: string;
  affaireId: number;
  affaireCode: string;
  affaireLibelle?: string;
  userId: number;
  userLogin: string;
  lignesConsommation?: ILigneConsommation[];
  createdBy: string;
  statut?: string;
  createdDate: string;
}
