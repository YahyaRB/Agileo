export interface IArticleStock {
  referenceArticle: string;
  designationArticle: string;
  unite: string;
  stockDisponible: number;
  totalRecu: number;
  totalConsomme: number;
  familleStatistique1?: string;
  familleStatistique2?: string;
  familleStatistique3?: string;
  familleStatistique4?: string;
}
