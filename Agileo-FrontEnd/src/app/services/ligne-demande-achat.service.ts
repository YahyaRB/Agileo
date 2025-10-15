import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { ILigneDemande } from '../../interfaces/ilignedemande';
import { environment } from '../../environments/environment';

const AUTH_API = 'demandes-achat';
const httpOptions = {
  headers: new HttpHeaders({
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  })
};

@Injectable({
  providedIn: 'root'
})
export class LigneDemandeAchatService {

  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  /**
   * Ajouter une ligne Ã  une demande d'achat
   */
  addLigneDemande(lignDemande: any, idDemande: number | undefined): Observable<any> {
    const url = `${this.apiUrl}${AUTH_API}/${idDemande}/lignes`;

    console.log('=== AJOUT LIGNE DEMANDE - SERVICE ===');
    console.log('URL:', url);
    console.log('DonnÃ©es envoyÃ©es:', lignDemande);

    return this.http.post(url, lignDemande, httpOptions)
      .pipe(
        tap(response => {
          console.log('âœ… Ligne ajoutÃ©e avec succÃ¨s:', response);
        }),
        catchError(this.handleError)
      );
  }

  /**
   * RÃ©cupÃ©rer toutes les lignes d'une demande d'achat
   */
  getLignesDemandeByDemandeId(demandeId: number): Observable<ILigneDemande[]> {
    const url = `${this.apiUrl}${AUTH_API}/${demandeId}/lignes`;

    console.log('=== RÃ‰CUPÃ‰RATION LIGNES - SERVICE ===');
    console.log('URL:', url);

    return this.http.get<ILigneDemande[]>(url)
      .pipe(
        tap(response => {
          console.log('âœ… Lignes rÃ©cupÃ©rÃ©es:', response?.length || 0);
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Mettre Ã  jour une ligne de demande d'achat
   */
  updateLigneDemande(lignDemande: any, idLigneDemande: number | undefined): Observable<any> {
    const url = `${this.apiUrl}${AUTH_API}/lignes/${idLigneDemande}`;

    console.log('=== MISE Ã€ JOUR LIGNE - SERVICE ===');
    console.log('URL:', url);
    console.log('DonnÃ©es envoyÃ©es:', lignDemande);

    return this.http.put(url, lignDemande, httpOptions)
      .pipe(
        tap(response => {
          console.log('âœ… Ligne mise Ã  jour avec succÃ¨s:', response);
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Supprimer une ligne de demande d'achat
   */
  deleteLigneDemande(idLigneDemande: number | undefined): Observable<any> {
    const url = `${this.apiUrl}${AUTH_API}/lignes/${idLigneDemande}`;

    console.log('=== SUPPRESSION LIGNE - SERVICE ===');
    console.log('URL:', url);

    return this.http.delete(url, httpOptions)
      .pipe(
        tap(response => {
          console.log('âœ… Ligne supprimÃ©e avec succÃ¨s:', response);
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Gestion centralisÃ©e des erreurs
   */
  private handleError = (error: HttpErrorResponse): Observable<never> => {
    console.error('âŒ Erreur dans LigneDemandeAchatService:', error);

    let errorMessage = 'Une erreur inconnue est survenue';

    if (error.error instanceof ErrorEvent) {
      // Erreur cÃ´tÃ© client
      errorMessage = `Erreur client: ${error.error.message}`;
    } else {
      // Erreur cÃ´tÃ© serveur
      switch (error.status) {
        case 0:
          errorMessage = 'Impossible de contacter le serveur. VÃ©rifiez la connexion rÃ©seau.';
          break;
        case 400:
          errorMessage = error.error?.message || 'DonnÃ©es invalides envoyÃ©es au serveur.';
          break;
        case 403:
          errorMessage = 'AccÃ¨s refusÃ©. VÃ©rifiez vos permissions.';
          break;
        case 404:
          errorMessage = 'Ressource non trouvÃ©e.';
          break;
        case 500:
          errorMessage = 'Erreur interne du serveur.';
          break;
        default:
          errorMessage = error.error?.message || `Erreur ${error.status}: ${error.message}`;
      }
    }

    console.error('Message d\'erreur final:', errorMessage);
    return throwError(() => new Error(errorMessage));
  }
}
