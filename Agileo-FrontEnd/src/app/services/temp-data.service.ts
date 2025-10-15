import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from "@angular/common/http";
import { Observable, timeout } from "rxjs";
import { Article } from "../../interfaces/iarticle";
import { environment } from "../../environments/environment";
import { catchError, retry } from "rxjs/operators";
import {PagedResponse} from "../../interfaces/PagedResponse";
import {ICommande} from "../../interfaces/icommande";
import {IFamilleStatistique} from "../../interfaces/ifamille-statistique";

const AUTH_API = 'temp';
const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable({
  providedIn: 'root'
})
export class TempDataService {

  constructor(private http: HttpClient) { }
  getArticlesFiltered(
    search?: string,
    familleCode?: string,
    page: number = 0,
    size: number = 10,
    sortBy: string = 'ref',
    sortDir: string = 'asc'
  ): Observable<PagedResponse<Article>> {

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    // ✅ Ajouter les paramètres optionnels s'ils existent
    if (search && search.trim() !== '') {
      params = params.set('search', search.trim());
    }

    if (familleCode && familleCode.trim() !== '') {
      params = params.set('familleCode', familleCode.trim());
    }

    const url = environment.apiUrl + 'temp/articles/filtered';

    console.log('🔍 Recherche articles avec filtres:', {
      search,
      familleCode,
      page,
      size,
      sortBy,
      sortDir
    });

    return this.http.get<PagedResponse<Article>>(url, { ...httpOptions, params })
      .pipe(
        timeout(10000),
        retry(2)
      );
  }
  /**
   * Récupérer tous les articles avec pagination
   */
  getAllArticlesPaginated(
    page: number = 0,
    size: number = 10,
    sortBy: string = 'ref',
    sortDir: string = 'asc'
  ): Observable<PagedResponse<Article>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<PagedResponse<Article>>(
      environment.apiUrl + 'temp/articles',
      { ...httpOptions, params }
    ).pipe(timeout(10000), retry(2));
  }

  /**
   * Rechercher des articles avec pagination
   */
  searchArticlesPaginated(
    searchTerm: string,
    page: number = 0,
    size: number = 10,
    sortBy: string = 'ref',
    sortDir: string = 'asc'
  ): Observable<PagedResponse<Article>> {
    let params = new HttpParams()
      .set('term', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<PagedResponse<Article>>(
      environment.apiUrl + 'temp/articles/search',
      { ...httpOptions, params }
    ).pipe(timeout(10000), retry(2));
  }

  /**
   * Récupérer les articles par famille avec pagination
   */
  getArticlesByFamillePaginated(
    codeFam: string,
    type: number = 1,
    page: number = 0,
    size: number = 10,
    sortBy: string = 'ref',
    sortDir: string = 'asc'
  ): Observable<PagedResponse<Article>> {
    let params = new HttpParams()
      .set('type', type.toString())
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<PagedResponse<Article>>(
      environment.apiUrl + 'temp' + `/articles/famille/${codeFam}`,
      { ...httpOptions, params }
    ).pipe(timeout(10000), retry(2));
  }

  // Méthode de compatibilité pour l'ancien code
  getAllArticle(): Observable<Article[]> {
    return new Observable(observer => {
      this.getAllArticlesPaginated(0, 1000).subscribe({
        next: (pagedResponse) => {
          observer.next(pagedResponse.content);
          observer.complete();
        },
        error: (error) => observer.error(error)
      });
    });
  }

  getAllAffaires(): Observable<any[]> {
    return this.http.get<any[]>(environment.apiUrl + AUTH_API + "/affaires");
  }

  /**
   * Récupérer tous les fournisseurs depuis la table Commande
   */
  getAllFournisseurs(): Observable<string[]> {
    const url = environment.apiUrl + AUTH_API + "/fournisseurs";
    console.log('🏢 Récupération fournisseurs depuis:', url);

    return this.http.get<string[]>(url, httpOptions)
      .pipe(
        timeout(10000),
        retry(2)
      );
  }

  /**
   * Récupérer les fournisseurs pour une affaire spécifique
   */
  getFournisseursByAffaire(affaireCode: string): Observable<string[]> {
    const url = environment.apiUrl + AUTH_API + `/fournisseurs/affaire/${affaireCode}`;
    console.log('🏢 Récupération fournisseurs pour affaire:', affaireCode, 'depuis:', url);

    return this.http.get<string[]>(url, httpOptions)
      .pipe(
        timeout(10000),
        retry(2)
      );
  }

  /**
   * Récupérer tous les bons de commande depuis la table Commande
   */
  getAllBonCommandes(): Observable<any[]> {
    const url = environment.apiUrl + AUTH_API + "/boncommandes";
    console.log('📋 Récupération bons de commande depuis:', url);

    return this.http.get<any[]>(url, httpOptions)
      .pipe(
        timeout(10000),
        retry(2)
      );
  }
  getBonCommandesByIDCommnade(id:string):Observable<ICommande>{
    return this.http.get<ICommande>(environment.apiUrl + AUTH_API + "/boncommandes/"+id);
  }

  /**
   * Récupérer les bons de commande pour une affaire spécifique
   */
  getBonCommandesByAffaire(affaireCode: string): Observable<any[]> {
    const url = environment.apiUrl + AUTH_API + `/boncommandes/affaire/${affaireCode}`;
    console.log('📋 Récupération bons de commande pour affaire:', affaireCode, 'depuis:', url);

    return this.http.get<any[]>(url, httpOptions)
      .pipe(
        timeout(10000),
        retry(2)
      );
  }

  /**
   * Récupérer un bon de commande par ID
   */
  getBonCommandeById(id: number): Observable<any> {
    const url = environment.apiUrl + AUTH_API + `/boncommandes/${id}`;
    console.log('📋 Récupération bon de commande ID:', id, 'depuis:', url);

    return this.http.get<any>(url, httpOptions)
      .pipe(
        timeout(10000),
        retry(2)
      );
  }


  getFamilleStatistiques(familleType: number): Observable<string[]> {
    const url = environment.apiUrl + 'temp' + `/articles/familles/${familleType}/values`;
    console.log('📊 Récupération familles statistiques:', url, { familleType });

    return this.http.get<string[]>(url, httpOptions)
      .pipe(
        timeout(10000),
        retry(2)
      );
  }



  getFamilleStatistiquesWithCount(familleType: number): Observable<{code: string, count: number}[]> {
    const url = environment.apiUrl + 'temp' + `/articles/familles/${familleType}/count`;
    console.log('📊 Récupération familles avec comptage:', url, { familleType });

    return this.http.get<{code: string, count: number}[]>(url, httpOptions)
      .pipe(
        timeout(10000),
        retry(2)
      );
  }

  getFamilleStatistiquesWithDesignation(familleType: number): Observable<IFamilleStatistique[]> {
    const url = environment.apiUrl + 'temp' + `/articles/familles/${familleType}/with-designation`;
    console.log('📊 Récupération familles avec désignations:', url, { familleType });

    return this.http.get<IFamilleStatistique[]>(url, httpOptions)
      .pipe(
        timeout(10000),
        retry(2)
      );
  }
}
