import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, retry } from 'rxjs/operators';
import { IConsommation } from '../../interfaces/iconsommation';
import { IArticleStock } from "../../interfaces/iarticle-stock";
import { ILigneConsommation } from '../../interfaces/iligneconsommation';
import {environment} from "../../environments/environment";
const AUTH_API = 'consommations';
@Injectable({
  providedIn: 'root'
})
export class ConsommationService {


  constructor(private http: HttpClient) { }

  addConsommation(consommation: any): Observable<IConsommation> {
    return this.http.post<IConsommation>(environment.apiUrl+AUTH_API, consommation)
      .pipe(catchError(this.handleError));
  }

  getAllConsommations(): Observable<IConsommation[]> {
    return this.http.get<IConsommation[]>(environment.apiUrl+AUTH_API)
      .pipe(catchError(this.handleError));
  }

  getCurrentUserConsommations(): Observable<IConsommation[]> {
    return this.http.get<IConsommation[]>(`${environment.apiUrl+AUTH_API}/current/consommations`)
      .pipe(catchError(this.handleError));
  }

  getConsommationById(id: string | null): Observable<IConsommation> {
    return this.http.get<IConsommation>(`${environment.apiUrl+AUTH_API}/${id}`)
      .pipe(catchError(this.handleError));
  }

  getArticlesDisponibles(affaireId: number): Observable<IArticleStock[]> {
    return this.http.get<IArticleStock[]>(`${environment.apiUrl+AUTH_API}/articles-disponibles/${affaireId}`)
      .pipe(catchError(this.handleError));
  }

  getArticlesDisponiblesByCode(affaireCode: string): Observable<IArticleStock[]> {
    return this.http.get<IArticleStock[]>(`${environment.apiUrl+AUTH_API}/articles-disponibles/${affaireCode}`)
      .pipe(catchError(this.handleError));
  }

  getConsommationsByUser(userId: number): Observable<IConsommation[]> {
    return this.http.get<IConsommation[]>(`${environment.apiUrl+AUTH_API}/user/${userId}`)
      .pipe(catchError(this.handleError));
  }

  updateConsommation(id: number, consommation: any): Observable<any> {
    return this.http.put(`${environment.apiUrl+AUTH_API}/${id}`, consommation)
      .pipe(catchError(this.handleError));
  }

  envoyerConsommation(id: number): Observable<any> {
    return this.http.put(`${environment.apiUrl+AUTH_API}/${id}/envoyer`, {})
      .pipe(catchError(this.handleError));
  }

  deleteConsommation(id: number): Observable<any> {
    return this.http.delete(`${environment.apiUrl+AUTH_API}/${id}`)
      .pipe(catchError(this.handleError));
  }

  private handleError = (error: HttpErrorResponse): Observable<never> => {
    let errorMessage = 'Une erreur inattendue s\'est produite';

    if (error.error instanceof ErrorEvent) {
      errorMessage = `Erreur: ${error.error.message}`;
    } else {
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

    console.error('Erreur dans ConsommationService:', error);
    return throwError(() => new Error(errorMessage));
  }
}
