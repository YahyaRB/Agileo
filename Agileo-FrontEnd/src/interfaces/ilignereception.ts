export interface ILigneReception {
  id?: number;
  referenceArticle: string;
  designationArticle: string;
  quantite: number;
  qteCmd?: number;
  qteLivre?: number;
  reste?: number;
  unite: string;
  familleStatistique1?: string;
  familleStatistique2?: string;
  familleStatistique3?: string;
  familleStatistique4?: string;
}
