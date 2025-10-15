import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';

// DTOs pour les statistiques
export interface ConsommationStatsDTO {
  totalConsommations: number;
  totalLignes: number;
}

export interface DaStatsDTO {
  totalDemandes: number;
  enAttente: number;
  approuvees: number;
  rejetee: number;
}

export interface DashboardStats {
  statistiquesGenerales: {
    totalAffaires: number;
    totalUtilisateurs: number;
    totalFournisseurs: number;
    totalArticles: number;
  };
  demandesAchat: {
    total: number;
    brouillon: number;
    envoye: number;
    recu: number;
    rejete: number;
    tauxValidation: number;
    dernierMois: number;
    evolutionPourcentage: number;
  };
  consommations: {
    total: number;
    envoye: number;
    brouillon: number;
    dernierMois: number;
    lignesTotal: number;
  };
  commandes: {
    total: number;
    enCours: number;
    livrees: number;
    dernierMois: number;
    valeurTotale: number;
  };
  stock: {
    articlesEnStock: number;
    articlesEpuises: number;
    articlesStockFaible: number;
    valeurStock: number;
    tauxRotation: number;
  };
  receptions: {
    total: number;
    enAttente: number;
    integrees: number;
    dernierMois: number;
    lignesEnAttente: number;
  };
  articlesPopulaires: Array<{
    ref: string;
    designation: string;
    nombreDemandes: number;
    quantiteTotale: number;
    unite: string;
  }>;
  evolutionDemandes: Array<{
    date: string;
    demandes: number;
    consommations: number;
    receptions: number;
  }>;
  affaires?: Array<{
    code: string;
    libelle: string;
    demandesEnCours: number;
    commandesEnCours: number;
    budget: number;
    depense: number;
    pourcentageUtilise: number;
  }>;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private apiUrl = `${environment.apiUrl}dashboard`;

  constructor(private http: HttpClient) {}

  /**
   * Récupère les statistiques du dashboard pour l'utilisateur connecté
   */
  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/me`);
  }

  /**
   * Récupère les statistiques admin (toutes les données)
   */
  getAdminStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/admin`);
  }

  /**
   * Récupère les statistiques pour un utilisateur spécifique
   */
  getUserStats(accessorId: number): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/user/${accessorId}`);
  }

  /**
   * Statistiques de consommation par login (utilisateur)
   */
  getConsommationStatsByLogin(login: string): Observable<ConsommationStatsDTO> {
    return this.http.get<ConsommationStatsDTO>(`${this.apiUrl}/stats/consommation/user`);
  }

  /**
   * Statistiques DA par login (utilisateur)
   */
  getDaStatsByLogin(login: string): Observable<DaStatsDTO> {
    return this.http.get<DaStatsDTO>(`${this.apiUrl}/stats/da/user`);
  }

  /**
   * Statistiques de consommation globales
   */
  getConsommationStatsGlobal(): Observable<ConsommationStatsDTO> {
    return this.http.get<ConsommationStatsDTO>(`${this.apiUrl}/stats/consommation/global`);
  }

  /**
   * Statistiques DA globales
   */
  getDaStatsGlobal(): Observable<DaStatsDTO> {
    return this.http.get<DaStatsDTO>(`${this.apiUrl}/stats/da/global`);
  }
  getTotalDaStatsByUser(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/stats/TotalDAUser`);
  }
  getTotalConsommationStatsByUser(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/stats/TotalConsommationUser`);
  }
  getTotalReceptionStatsByUser(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/stats/TotalReceptionUser`);
  }
}
