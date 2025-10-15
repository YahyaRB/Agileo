
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

import { AffaireServiceService } from './affaire-service.service';
import { UserProfileService } from './user-profile.service';
import {Affaire} from "../../interfaces/iaffaire";

@Injectable({
  providedIn: 'root'
})
export class SharedAffaireService {
  private allAffairesSubject = new BehaviorSubject<Affaire[]>([]);
  private userAffairesSubject = new BehaviorSubject<Affaire[]>([]);
  private affairesMapSubject = new BehaviorSubject<Map<string, Affaire>>(new Map());

  private allAffairesLoaded = false;
  private userAffairesLoaded = false;

  public allAffaires$ = this.allAffairesSubject.asObservable();
  public userAffaires$ = this.userAffairesSubject.asObservable();
  public affairesMap$ = this.affairesMapSubject.asObservable();

  constructor(
    private affaireService: AffaireServiceService,
    private userProfileService: UserProfileService
  ) {}

  // Méthode principale pour obtenir les affaires selon le rôle
  getAffairesForUser(currentUser: any): Observable<Affaire[]> {
    if (this.isAdminOrManager(currentUser)) {
      return this.getAllAffaires();
    } else {
      return this.getUserSpecificAffaires(currentUser);
    }
  }

  // Charger toutes les affaires (pour admin/manager)
  getAllAffaires(): Observable<Affaire[]> {
    if (this.allAffairesLoaded && this.allAffairesSubject.value.length > 0) {
      return this.allAffaires$;
    }

    return this.affaireService.getAffaires().pipe(
      map(affaires => {
        const normalizedAffaires = this.normalizeAffaires(affaires || []);
        this.allAffairesSubject.next(normalizedAffaires);
        this.updateAffairesMap(normalizedAffaires);
        this.allAffairesLoaded = true;
        return normalizedAffaires;
      }),
      catchError(error => {
        console.error('Erreur chargement toutes affaires:', error);
        this.allAffairesSubject.next([]);
        return of([]);
      })
    );
  }

  // Charger les affaires spécifiques à l'utilisateur
  getUserSpecificAffaires(currentUser: any): Observable<Affaire[]> {
    if (this.userAffairesLoaded && this.userAffairesSubject.value.length > 0) {
      return this.userAffaires$;
    }

    // Essayer plusieurs méthodes pour obtenir les affaires utilisateur
    return this.loadUserAffairesWithFallback(currentUser).pipe(
      map(affaires => {
        const normalizedAffaires = this.normalizeAffaires(affaires || []);
        this.userAffairesSubject.next(normalizedAffaires);
        this.updateAffairesMap(normalizedAffaires);
        this.userAffairesLoaded = true;
        return normalizedAffaires;
      }),
      catchError(error => {
        console.error('Erreur chargement affaires utilisateur:', error);
        this.userAffairesSubject.next([]);
        return of([]);
      })
    );
  }

  // Méthode avec fallback pour charger les affaires utilisateur
  private loadUserAffairesWithFallback(currentUser: any): Observable<Affaire[]> {
    // Méthode 1: getCurrentAccessorAffaires
    return this.affaireService.getCurrentAccessorAffaires().pipe(
      map(data => {
        if (data && data.length > 0) {
          return data;
        }
        throw new Error('Pas de données getCurrentAccessorAffaires');
      }),
      catchError(() => {
        // Méthode 2: getAccessorAffaires avec userId
        if (currentUser?.id) {
          return this.affaireService.getAccessorAffaires(currentUser.id).pipe(
            map(data => {
              if (data && data.length > 0) {
                return data;
              }
              throw new Error('Pas de données getAccessorAffaires');
            }),
            catchError(() => {
              // Méthode 3: Filtrer toutes les affaires avec les affaires utilisateur
              return this.filterAllAffairesForUser(currentUser);
            })
          );
        } else {
          return this.filterAllAffairesForUser(currentUser);
        }
      })
    );
  }

  // Filtrer toutes les affaires selon les permissions utilisateur
  private filterAllAffairesForUser(currentUser: any): Observable<Affaire[]> {
    return this.affaireService.getAffaires().pipe(
      map(allAffaires => {
        const normalizedAffaires = this.normalizeAffaires(allAffaires || []);

        if (currentUser?.affaires && Array.isArray(currentUser.affaires)) {
          const userAffaireCodes = this.extractUserAffaireCodes(currentUser.affaires);

          if (userAffaireCodes.length > 0) {
            return normalizedAffaires.filter(affaire =>
              userAffaireCodes.includes(affaire.code) ||
              userAffaireCodes.includes(affaire.affaire)
            );
          }
        }
        return [];
      })
    );
  }

  // Obtenir la map des affaires
  getAffairesMap(): Map<string, Affaire> {
    return this.affairesMapSubject.value;
  }

  // Obtenir une affaire par code
  getAffaireByCode(code: string): Affaire | undefined {
    return this.affairesMapSubject.value.get(code);
  }

  // Mettre à jour la map des affaires
  private updateAffairesMap(affaires: Affaire[]) {
    const newMap = new Map<string, Affaire>();
    affaires.forEach(affaire => {
      if (affaire.code) {
        newMap.set(affaire.code, affaire);
      }
      if (affaire.affaire && affaire.affaire !== affaire.code) {
        newMap.set(affaire.affaire, affaire);
      }
    });
    this.affairesMapSubject.next(newMap);
  }

  // Vérifier si l'utilisateur est admin ou manager
  isAdminOrManager(currentUser: any): boolean {
    if (!currentUser?.roles) return false;

    const roles = Array.isArray(currentUser.roles)
      ? currentUser.roles
      : [currentUser.roles];

    return roles.some((role: any) => {
      const roleName = typeof role === 'string' ? role : role.name;
      return roleName === 'ADMIN' || roleName === 'MANAGER';
    });
  }

  // Normaliser les affaires
  private normalizeAffaires(affaires: any[]): Affaire[] {
    return affaires.map(affaire => {
      const normalized = {
        id: affaire.id || affaire.numero,
        code: affaire.code || affaire.affaire,
        affaire: affaire.affaire || affaire.code,
        nom: affaire.nom || affaire.libelle,
        libelle: affaire.libelle || affaire.nom,
        ...affaire
      };

      // Ajouter displayLabel pour l'affichage
      normalized.displayLabel = `${normalized.code || normalized.affaire} - ${normalized.libelle || normalized.nom}`;

      return normalized;
    });
  }

  // Extraire les codes d'affaires utilisateur
  private extractUserAffaireCodes(userAffaires: any[]): string[] {
    if (!Array.isArray(userAffaires) || userAffaires.length === 0) {
      return [];
    }

    const firstItem = userAffaires[0];

    if (typeof firstItem === 'string') {
      return userAffaires;
    } else if (typeof firstItem === 'object' && firstItem !== null) {
      if (firstItem.code) {
        return userAffaires.map(aff => aff.code);
      } else if (firstItem.affaire) {
        return userAffaires.map(aff => aff.affaire);
      } else if (firstItem.id) {
        return userAffaires.map(aff => aff.id.toString());
      }
    }

    return [];
  }

  // Forcer le rechargement des affaires
  forceReload() {
    this.allAffairesLoaded = false;
    this.userAffairesLoaded = false;
    this.allAffairesSubject.next([]);
    this.userAffairesSubject.next([]);
    this.affairesMapSubject.next(new Map());
  }

  // Créer un objet affaire à partir des données de consommation
  createAffaireFromConsommation(consommation: any): Affaire {
    return {
      id: consommation.affaireId || this.generateNumericId(consommation.affaireCode!),
      code: consommation.affaireCode,
      affaire: consommation.affaireCode,
      libelle: consommation.affaireLibelle || consommation.affaireCode,
      nom: consommation.affaireLibelle || consommation.affaireCode,
      displayLabel: `${consommation.affaireCode} - ${consommation.affaireLibelle || consommation.affaireCode}`
    } as Affaire;
  }

  // Générer un ID numérique
  private generateNumericId(affaireCode: string): number | undefined {
    if (!affaireCode) return undefined;

    const numericPart = affaireCode.replace(/[^0-9]/g, '');
    if (numericPart) {
      return parseInt(numericPart, 10);
    }

    let hash = 0;
    for (let i = 0; i < affaireCode.length; i++) {
      const char = affaireCode.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash;
    }
    return Math.abs(hash);
  }
}
