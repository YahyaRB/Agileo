// bon-commande-cache.service.ts
import { Injectable } from '@angular/core';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { map, tap, catchError } from 'rxjs/operators';
import { TempDataService } from './temp-data.service';
import {HttpClient} from "@angular/common/http";
import {IConsommation} from "../../interfaces/iconsommation";
import {environment} from "../../environments/environment";
const AUTH_API = 'commandes';
@Injectable({
  providedIn: 'root'
})
export class BonCommandeCacheService {
  private bonCommandesSubject = new BehaviorSubject<any[]>([]);
  private isLoading = false;
  private cacheValidUntil: number = 0;
  private readonly CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

  constructor(private http: HttpClient,
    private tempDataService: TempDataService) {}

  /**
   * Récupère les bons de commande avec mise en cache
   */
  getBonCommandes(forceRefresh: boolean = false): Observable<any[]> {
    const now = Date.now();
    const hasValidCache = now < this.cacheValidUntil && this.bonCommandesSubject.value.length > 0;

    // Si cache valide et pas de refresh forcé
    if (hasValidCache && !forceRefresh) {
      console.log('📋 Utilisation du cache des bons de commande');
      return this.bonCommandesSubject.asObservable();
    }

    // Si déjà en cours de chargement
    if (this.isLoading) {
      console.log('📋 Chargement en cours, attente...');
      return this.bonCommandesSubject.asObservable();
    }

    // Charger depuis le backend
    console.log('📋 Chargement des bons de commande depuis le backend');
    this.isLoading = true;

    return this.tempDataService.getAllBonCommandes().pipe(
      tap(data => {
        console.log(`📋 ${data?.length || 0} bons de commande chargés`);
        this.bonCommandesSubject.next(data || []);
        this.cacheValidUntil = now + this.CACHE_DURATION;
        this.isLoading = false;
      }),
      catchError(error => {
        console.error('❌ Erreur chargement bons de commande:', error);
        this.bonCommandesSubject.next([]);
        this.isLoading = false;
        return of([]);
      }),
      map(() => this.bonCommandesSubject.value)
    );
  }

  /**
   * Vide le cache (utile après création/modification)
   */
  clearCache(): void {
    console.log('🗑️ Cache des bons de commande vidé');
    this.cacheValidUntil = 0;
    this.bonCommandesSubject.next([]);
  }

  /**
   * Obtient les bons de commande actuellement en cache
   */
  getCurrentBonCommandes(): any[] {
    return this.bonCommandesSubject.value;
  }

  /**
   * Vérifie si le cache est valide
   */
  isCacheValid(): boolean {
    return Date.now() < this.cacheValidUntil && this.bonCommandesSubject.value.length > 0;
  }

  getLignesBonCommande(idBC: number ): Observable<any[]> {

    return this.http.get<any[]>(environment.apiUrl+AUTH_API+"/lignesByCommande/"+idBC);

  }
}
