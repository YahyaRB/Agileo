export interface MonthlyCount {
  month: string;
  count: number;
}

export interface UserActivityStats {
  demandesAchat: MonthlyCount[];
  receptions: MonthlyCount[];
  consommations: MonthlyCount[];
}

