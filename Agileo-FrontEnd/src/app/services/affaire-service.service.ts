import { Injectable } from '@angular/core';
import {Affaire} from "../../interfaces/iaffaire";
import {Observable, throwError} from "rxjs";
import {HttpClient, HttpHeaders, HttpErrorResponse} from "@angular/common/http";
import {environment} from "../../environments/environment";
import { catchError } from 'rxjs/operators';
import { map } from 'rxjs/operators';

const AUTH_API = 'affaires';
const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable({
  providedIn: 'root'
})
export class AffaireServiceService {

  constructor(private http: HttpClient) { }

  /**
   * Récupérer toutes les affaires
   */
  getAffaires(): Observable<Affaire[]> {
    return this.http.get<Affaire[]>(`${environment.apiUrl}${AUTH_API}`)
      .pipe(
        catchError(this.handleError)
      );
  }





  /**
   * Mettre à jour une affaire
   */
  updateAffaire(id: number | undefined, affaire: Affaire): Observable<any> {
    return this.http.put<any>(`${environment.apiUrl}${AUTH_API}/${id}`, affaire, httpOptions)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Ajouter une nouvelle affaire
   */
  addAffaire(affaire: Affaire): Observable<Affaire> {
    return this.http.post<Affaire>(`${environment.apiUrl}${AUTH_API}`, affaire, httpOptions)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Supprimer une affaire
   */
  deleteAffaire(id: number): Observable<any> {
    return this.http.delete(`${environment.apiUrl}${AUTH_API}/${id}`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Récupérer une affaire par ID
   */
  getAffaireById(id: number): Observable<Affaire> {
    return this.http.get<Affaire>(`${environment.apiUrl}${AUTH_API}/${id}`)
      .pipe(
        catchError(this.handleError)
      );
  }


  getAffaireByCode(code: string): Observable<Affaire> {
    return this.http.get<Affaire[]>(`${environment.apiUrl}${AUTH_API}/code/${code}`)
      .pipe(
        catchError(this.handleError),
        // Transformer le tableau en un seul élément
        map((affaires: Affaire[]) => {
          if (affaires && affaires.length > 0) {
            return affaires[0];
          }
          throw new Error('Aucune affaire trouvée avec ce code');
        })
      );
  }

  /**
   * Récupérer les affaires par statut
   */
  getAffairesByStatut(statut: number): Observable<Affaire[]> {
    return this.http.get<Affaire[]>(`${environment.apiUrl}${AUTH_API}/statut/${statut}`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Récupérer les utilisateurs d'une affaire
   */
  getAffaireUsers(affaireCode: string): Observable<string[]> {
    return this.http.get<string[]>(`${environment.apiUrl}${AUTH_API}/code/${affaireCode}/users`)
      .pipe(
        catchError(this.handleError)
      );
  }

  // ================ NOUVELLES MÉTHODES POUR KDNSACCESSOR ================

  /**
   * Ajouter un accessor à une affaire
   */
  addAccessorToAffaire(affaireCode: string, accessorId: number): Observable<any> {
    return this.http.post<any>(`${environment.apiUrl}${AUTH_API}/code/${affaireCode}/accessors/${accessorId}`, {})
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Retirer un accessor d'une affaire
   */
  removeAccessorFromAffaire(affaireCode: string, accessorId: number): Observable<any> {
    return this.http.delete(`${environment.apiUrl}${AUTH_API}/code/${affaireCode}/accessors/${accessorId}`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Récupérer les affaires d'un accessor spécifique
   */
  getAccessorAffaires(accessorId: number): Observable<Affaire[]> {
    return this.http.get<Affaire[]>(`${environment.apiUrl}${AUTH_API}/accessor/${accessorId}`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Récupérer les affaires de l'accessor connecté
   */
  getCurrentAccessorAffaires(): Observable<Affaire[]> {
    return this.http.get<Affaire[]>(`${environment.apiUrl}${AUTH_API}/my-affaires`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Compter les affaires d'un accessor
   */
  getAccessorAffairesCount(accessorId: number): Observable<number> {
    return this.http.get<number>(`${environment.apiUrl}${AUTH_API}/accessor/${accessorId}/count`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Récupérer les affaires actives d'un accessor
   */
  getActiveAccessorAffaires(accessorId: number): Observable<Affaire[]> {
    return this.http.get<Affaire[]>(`${environment.apiUrl}${AUTH_API}/accessor/${accessorId}/active`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Vérifier si un accessor peut être assigné à une affaire
   */
  canAssignAccessorToAffaire(affaireCode: string, accessorId: number): Observable<boolean> {
    return this.http.get<boolean>(`${environment.apiUrl}${AUTH_API}/validate/assignment/${affaireCode}/${accessorId}`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Récupérer les assignations d'un accessor
   */
  getAccessorAssignments(accessorId: number): Observable<any[]> {
    return this.http.get<any[]>(`${environment.apiUrl}${AUTH_API}/accessors/${accessorId}/assignments`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Récupérer les détails d'une affaire
   */
  getAffaireDetails(affaireCode: string): Observable<any> {
    return this.http.get<any>(`${environment.apiUrl}${AUTH_API}/code/${affaireCode}/details`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Récupérer toutes les assignations
   */
  getAllAssignments(): Observable<any[]> {
    return this.http.get<any[]>(`${environment.apiUrl}${AUTH_API}/assignments`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Récupérer les statistiques des affaires
   */
  getAffaireStats(): Observable<any> {
    return this.http.get<any>(`${environment.apiUrl}${AUTH_API}/stats`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Valider un code d'affaire
   */
  validateAffaireCode(code: string): Observable<boolean> {
    return this.http.get<boolean>(`${environment.apiUrl}${AUTH_API}/validate/code/${code}`)
      .pipe(
        catchError(this.handleError)
      );
  }

  // ================ MÉTHODES DÉPRÉCIÉES (RÉTROCOMPATIBILITÉ) ================

  /**
   * @deprecated Utiliser addAccessorToAffaire à la place
   */
  addUserToAffaire(affaireCode: string, userId: number): Observable<any> {
    console.warn('addUserToAffaire est déprécié. Utilisez addAccessorToAffaire à la place.');
    return this.addAccessorToAffaire(affaireCode, userId);
  }

  /**
   * @deprecated Utiliser removeAccessorFromAffaire à la place
   */
  removeUserFromAffaire(affaireCode: string, userId: number): Observable<any> {
    console.warn('removeUserFromAffaire est déprécié. Utilisez removeAccessorFromAffaire à la place.');
    return this.removeAccessorFromAffaire(affaireCode, userId);
  }

  /**
   * @deprecated Utiliser getAccessorAffaires à la place
   */
  getCurrentUserAffaires(userId: number): Observable<Affaire[]> {
    console.warn('getCurrentUserAffaires est déprécié. Utilisez getAccessorAffaires à la place.');
    return this.getAccessorAffaires(userId);
  }

  /**
   * Changer le statut d'une affaire
   */
  changeAffaireStatus(id: number, newStatus: number): Observable<any> {
    return this.http.put<any>(`${environment.apiUrl}${AUTH_API}/${id}/status/${newStatus}`, {})
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Test de connexion
   */
  testConnection(): Observable<string> {
    return this.http.get<string>(`${environment.apiUrl}${AUTH_API}/test`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Gestion centralisée des erreurs
   */
  private handleError = (error: HttpErrorResponse) => {
    console.error('Erreur dans AffaireServiceService:', error);

    let errorMessage = 'Une erreur est survenue';

    if (error.error instanceof ErrorEvent) {
      // Erreur côté client
      errorMessage = `Erreur client: ${error.error.message}`;
    } else {
      // Erreur côté serveur
      switch (error.status) {
        case 400:
          errorMessage = 'Données invalides';
          break;
        case 401:
          errorMessage = 'Non autorisé - Veuillez vous connecter';
          break;
        case 403:
          errorMessage = 'Accès refusé';
          break;
        case 404:
          errorMessage = 'Ressource non trouvée';
          break;
        case 500:
          errorMessage = 'Erreur serveur interne';
          break;
        default:
          errorMessage = `Erreur serveur: ${error.status} - ${error.message}`;
      }
    }

    return throwError(() => new Error(errorMessage));
  };
}
