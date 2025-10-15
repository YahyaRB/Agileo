import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {Observable, of} from 'rxjs';
import { Iuser } from "../../interfaces/iuser";
import { environment } from "../../environments/environment";
import { Access } from "../../interfaces/iaccess";
import { Affaire } from "../../interfaces/iaffaire";
import { Role } from "../../interfaces/irole";
import {catchError, map} from "rxjs/operators";

const AUTH_API2 = 'admin/users';
const ACCESSOR_API = 'admin/accessors';
const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable({
  providedIn: 'root'
})
export class UtilisateurServiceService {

  constructor(private http: HttpClient) { }


  /**
   * Récupérer tous les utilisateurs
   */
  getAllUsers(): Observable<Iuser[]> {
    return this.http.get<Iuser[]>(environment.apiUrl + AUTH_API2);
  }

  /**
   * Récupérer un utilisateur par ID
   */
  getUserById(userId: number): Observable<Iuser> {
    return this.http.get<Iuser>(`${environment.apiUrl}${AUTH_API2}/${userId}`);
  }

  /**
   * Activer un utilisateur
   */
  activateUser(userId: number): Observable<any> {
    return this.http.put(`${environment.apiUrl}${AUTH_API2}/${userId}/activate`, {}, httpOptions);
  }

  /**
   * Désactiver un utilisateur
   */
  deactivateUser(userId: number): Observable<any> {
    return this.http.put(`${environment.apiUrl}${AUTH_API2}/${userId}/deactivate`, {}, httpOptions);
  }

  /**
   * Mettre à jour l'ID Agelio d'un utilisateur
   */
  updateUserIdAgelio(userId: number, idAgelio: string): Observable<any> {
    const params = new HttpParams().set('idAgelio', idAgelio);
    return this.http.put(`${environment.apiUrl}${AUTH_API2}/${userId}/id-agelio`, {}, {
      headers: httpOptions.headers,
      params: params
    });
  }

  // ================ GESTION DES ACCÈS ================

  /**
   * Ajouter un accès à un utilisateur
   */
  addAccessToUser(userId: number, accessId: number | undefined): Observable<any> {
    return this.http.post(`${environment.apiUrl}${AUTH_API2}/${userId}/acces/${accessId}`, {}, httpOptions);
  }

  /**
   * Récupérer les accès d'un utilisateur
   */
  getUserAccess(userId: number): Observable<Access[]> {
    return this.http.get<Access[]>(`${environment.apiUrl}${AUTH_API2}/${userId}/acces`);
  }

  /**
   * Retirer un accès d'un utilisateur
   */
  removeAccessFromUser(userId: number, accessId: number | undefined): Observable<any> {
    return this.http.delete(`${environment.apiUrl}${AUTH_API2}/${userId}/acces/${accessId}`, httpOptions);
  }

  // ================ GESTION DES RÔLES ================

  /**
   * Ajouter un rôle à un utilisateur
   */
  addRoleToUser(userId: number, roleId: number | undefined): Observable<any> {
    return this.http.post(`${environment.apiUrl}${AUTH_API2}/${userId}/roles/${roleId}`, {}, httpOptions);
  }

  /**
   * Récupérer les rôles d'un utilisateur
   */
  getUserRoles(userId: number): Observable<Role[]> {
    return this.http.get<Role[]>(`${environment.apiUrl}${AUTH_API2}/${userId}/roles`);
  }

  /**
   * Retirer un rôle d'un utilisateur
   */
  removeRoleFromUser(userId: number, roleId: number | undefined): Observable<any> {
    return this.http.delete(`${environment.apiUrl}${AUTH_API2}/${userId}/roles/${roleId}`, httpOptions);
  }

  // ================ GESTION DES AFFAIRES - UTILISE KDNSACCESSOR ================



  /**
   * Récupérer les affaires actives d'un accessor (via KdnsAccessor)
   */
  getActiveAccessorAffaires(accessorId: number): Observable<Affaire[]> {
    return this.http.get<Affaire[]>(`${environment.apiUrl}${ACCESSOR_API}/${accessorId}/affaires/active`);
  }


  /**
   * Compter les affaires d'un accessor (via KdnsAccessor)
   */
  getAccessorAffairesCount(accessorId: number): Observable<number> {
    return this.http.get<number>(`${environment.apiUrl}${ACCESSOR_API}/${accessorId}/affaires/count`);
  }

  /**
   * Vérifier si un accessor peut être assigné à une affaire (via KdnsAccessor)
   */
  canAssignAccessorToAffaire(accessorId: number, affaireCode: string): Observable<boolean> {
    return this.http.get<boolean>(`${environment.apiUrl}${ACCESSOR_API}/${accessorId}/affaires/${affaireCode}/can-assign`);
  }

  /**
   * Récupérer les assignations d'un accessor spécifique (via KdnsAccessor)
   */
  getAccessorAssignments(accessorId: number): Observable<any[]> {
    return this.http.get<any[]>(`${environment.apiUrl}${ACCESSOR_API}/${accessorId}/assignments`);
  }

  // ================ NOUVELLES MÉTHODES POUR KDNSACCESSOR ================

  /**
   * Récupérer un accessor par login (lien User -> KdnsAccessor)
   */
  getAccessorByLogin(login: string): Observable<any> {
    return this.http.get<any>(`${environment.apiUrl}${ACCESSOR_API}/by-login/${login}`);
  }

  /**
   * Récupérer tous les accessors
   */
  getAllAccessors(): Observable<any[]> {
    return this.http.get<any[]>(`${environment.apiUrl}${ACCESSOR_API}`);
  }

  /**
   * Récupérer un accessor par ID
   */
  getAccessorById(accessorId: number): Observable<any> {
    return this.http.get<any>(`${environment.apiUrl}${ACCESSOR_API}/${accessorId}`);
  }

  /**
   * Rechercher des accessors par nom
   */
  searchAccessors(searchTerm: string): Observable<any[]> {
    const params = new HttpParams().set('search', searchTerm);
    return this.http.get<any[]>(`${environment.apiUrl}${ACCESSOR_API}/search`, { params });
  }

  // ================ MÉTHODES DÉPRÉCIÉES POUR RÉTROCOMPATIBILITÉ ================

  /**
   * @deprecated Utiliser getAccessorAffaires à la place
   * Récupérer les affaires d'un utilisateur (ancienne méthode)
   */
  // getUserAffaire(userId: number): Observable<Affaire[]> {
  //   console.warn('getUserAffaire est déprécié. Utilisez getAccessorAffaires à la place.');
  //   return this.getUserById(userId).pipe(
  //     );
  // }

  /**
   * @deprecated Utiliser addAffaireToAccessor à la place
   * Ajouter une affaire à un utilisateur (ancienne méthode)
   */
  addAffaireToUser(userId: number, affaireCode: string): Observable<any> {
    console.warn('addAffaireToUser est déprécié. Utilisez addAffaireToAccessor avec le code affaire à la place.');
    // Cette méthode devrait maintenant récupérer l'accessor correspondant au user
    // et utiliser addAffaireToAccessor
    throw new Error('Méthode dépréciée. Utilisez addAffaireToAccessor.');
  }

  /**
   * @deprecated Utiliser removeAffaireFromAccessor à la place
   * Retirer une affaire d'un utilisateur (ancienne méthode)
   */
  removeAffaireFromUser(userId: number, affaireCode: string): Observable<any> {
    console.warn('removeAffaireFromUser est déprécié. Utilisez removeAffaireFromAccessor avec le code affaire à la place.');
    throw new Error('Méthode dépréciée. Utilisez removeAffaireFromAccessor.');
  }

  // ================ SYNCHRONISATION KEYCLOAK ================

  /**
   * Synchroniser tous les utilisateurs avec Keycloak
   */
  syncAllUsersWithKeycloak(): Observable<any> {
    return this.http.post(`${environment.apiUrl}${AUTH_API2}/sync-keycloak`, {}, httpOptions);
  }

  /**
   * Synchroniser un utilisateur spécifique avec Keycloak
   */
  syncUserWithKeycloak(userId: number): Observable<any> {
    return this.http.post(`${environment.apiUrl}${AUTH_API2}/${userId}/sync-keycloak`, {}, httpOptions);
  }

  // ================ MÉTHODES UTILITAIRES POUR USERS ================

  /**
   * Rechercher des utilisateurs par terme de recherche
   */
  searchUsers(searchTerm: string): Observable<Iuser[]> {
    const params = new HttpParams().set('search', searchTerm);
    return this.http.get<Iuser[]>(`${environment.apiUrl}${AUTH_API2}/search`, { params });
  }

  /**
   * Récupérer les utilisateurs actifs seulement
   */
  getActiveUsers(): Observable<Iuser[]> {
    return this.http.get<Iuser[]>(`${environment.apiUrl}${AUTH_API2}?status=active`);
  }

  /**
   * Récupérer les utilisateurs inactifs seulement
   */
  getInactiveUsers(): Observable<Iuser[]> {
    return this.http.get<Iuser[]>(`${environment.apiUrl}${AUTH_API2}?status=inactive`);
  }



  /**
   * Récupérer l'ID de l'utilisateur correspondant à un accessor
   */
  getUserIdByAccessorId(accessorId: number): Observable<number> {
    return this.http.get<number>(`${environment.apiUrl}${ACCESSOR_API}/${accessorId}/user-id`);
  }





  getAccessorIdByUserId(userId: number): Observable<number | null> {
    return this.http.get<number>(`${environment.apiUrl}${AUTH_API2}/${userId}/accessor-id`)
      .pipe(
        map(response => response || null),
        catchError(error => {
          if (error.status === 204 || error.status === 404) {
            return of(null);
          }
          console.error('Error getting accessor ID for user:', error);
          return of(null);
        })
      );
  }

  linkUserToAccessor(userId: number, accessorId: number): Observable<any> {
    return this.http.post(`${environment.apiUrl}${AUTH_API2}/${userId}/link-accessor/${accessorId}`, {}, httpOptions);
  }

  unlinkUserFromAccessor(userId: number): Observable<any> {
    return this.http.delete(`${environment.apiUrl}${AUTH_API2}/${userId}/unlink-accessor`, httpOptions);
  }

// Remplacer les méthodes dépréciées pour les affaires
  getAccessorAffaires(accessorId: number): Observable<Affaire[]> {
    return this.http.get<Affaire[]>(`${environment.apiUrl}affaires/accessor/${accessorId}`);
  }

  addAffaireToAccessor(accessorId: number, affaireCode: string): Observable<any> {
    return this.http.post(`${environment.apiUrl}affaires/code/${affaireCode}/accessors/${accessorId}`, {}, httpOptions);
  }

  removeAffaireFromAccessor(accessorId: number, affaireCode: string): Observable<any> {
    return this.http.delete(`${environment.apiUrl}affaires/code/${affaireCode}/accessors/${accessorId}`, httpOptions);
  }
}
