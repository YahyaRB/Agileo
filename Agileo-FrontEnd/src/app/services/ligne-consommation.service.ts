import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, retry } from 'rxjs/operators';
import { ILigneConsommation } from '../../interfaces/iligneconsommation';

@Injectable({
  providedIn: 'root'
})
export class LigneConsommationService {
  private apiUrl = 'http://localhost:8081/api/consommations';

  constructor(private http: HttpClient) { }

  // Ajouter des lignes de consommation avec gestion d'erreur améliorée
  addLigneConsommation(lignes: ILigneConsommation[], consommationId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${consommationId}/lignes`, lignes)
      .pipe(
        retry(1), // Retry une fois en cas d'échec
        catchError(this.handleError)
      );
  }

  // Récupérer les lignes de consommation par ID de consommation
  getLignesConsommationByConsommationId(consommationId: number): Observable<ILigneConsommation[]> {
    return this.http.get<ILigneConsommation[]>(`${this.apiUrl}/${consommationId}/lignes`)
      .pipe(
        catchError(this.handleError)
      );
  }

  // Mettre à jour une ligne de consommation
  updateLigneConsommation(ligneId: number, ligne: ILigneConsommation): Observable<any> {
    return this.http.put(`${this.apiUrl}/lignes/${ligneId}`, ligne)
      .pipe(
        catchError(this.handleError)
      );
  }

  // Supprimer une ligne de consommation
  deleteLigneConsommation(ligneId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/lignes/${ligneId}`)
      .pipe(
        catchError(this.handleError)
      );
  }

  // Valider une quantité avant ajout/modification (optionnel)
  validateQuantite(consommationId: number, referenceArticle: string, quantite: number): Observable<{valide: boolean, message: string}> {
    const payload = {
      referenceArticle,
      quantite
    };

    return this.http.post<{valide: boolean, message: string}>(`${this.apiUrl}/${consommationId}/validate-quantite`, payload)
      .pipe(
        catchError(this.handleError)
      );
  }

  // Gestion centralisée des erreurs
  private handleError = (error: HttpErrorResponse): Observable<never> => {
    let errorMessage = 'Une erreur inattendue s\'est produite';

    if (error.error instanceof ErrorEvent) {
      // Erreur côté client
      errorMessage = `Erreur: ${error.error.message}`;
    } else {
      // Erreur côté serveur
      switch (error.status) {
        case 400:
          errorMessage = error.error?.message || 'Données invalides';
          break;
        case 401:
          errorMessage = 'Vous n\'êtes pas autorisé à effectuer cette action';
          break;
        case 403:
          errorMessage = 'Accès refusé. Permissions insuffisantes';
          break;
        case 404:
          errorMessage = 'Ressource non trouvée';
          break;
        case 409:
          errorMessage = 'Conflit: ' + (error.error?.message || 'Stock insuffisant ou contrainte violée');
          break;
        case 500:
          errorMessage = 'Erreur interne du serveur';
          break;
        default:
          errorMessage = `Erreur ${error.status}: ${error.error?.message || error.message}`;
      }
    }

    console.error('Erreur dans LigneConsommationService:', error);
    return throwError(() => new Error(errorMessage));
  }
}
