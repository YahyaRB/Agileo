import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders, HttpParams} from "@angular/common/http";
import { IDemandeAchat } from "../../interfaces/idemandeAchat";
import { Observable } from "rxjs";
import { environment } from "../../environments/environment";
import { ILigneDemande } from "../../interfaces/ilignedemande";
import {PagedResponse, PaginationParams} from "../../interfaces/PagedResponse";
import {IPieces} from "../../interfaces/ipiecesjoints";

const AUTH_API = 'demandes-achat';
const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable({
  providedIn: 'root'
})
export class DemandeAchatService {

  constructor(private http: HttpClient) { }

  addDemandeAchat(demande: IDemandeAchat): Observable<IDemandeAchat> {
    return this.http.post<IDemandeAchat>(environment.apiUrl + AUTH_API, demande, httpOptions);
  }
  uploadFiles(demandeId: number, formData: FormData): Observable<any> {
    return this.http.post(`${environment.apiUrl + AUTH_API}/upload/${demandeId}`, formData);
  }
  getPieceJointByDemandeAchatID(demandeId:number):Observable<IPieces[]>{
    return this.http.get<IPieces[]>(environment.apiUrl+AUTH_API+"/"+demandeId+"/pieces");
  }
  deletePieceJointe(pieceId: number): Observable<any> {
    return this.http.delete(environment.apiUrl+AUTH_API+"/pieces"+"/"+pieceId);
  }

  uploadPiecesJointes(demandeId: number, formData: FormData) {
    return this.http.post(environment.apiUrl+AUTH_API+"/upload/"+demandeId, formData);
  }
  deleteDemandeAchat(demandeId: number | string) {
    return this.http.delete(environment.apiUrl + AUTH_API + "/" + demandeId, httpOptions);
  }

  /**
   * Récupère les demandes selon les droits de l'utilisateur connecté
   * - ADMIN/RESPONSABLE_ACHAT : toutes les demandes
   * - Utilisateur normal : seulement ses propres demandes
   */
  getAllDemandesAchat(): Observable<IDemandeAchat[]> {
    return this.http.get<IDemandeAchat[]>(environment.apiUrl + AUTH_API);
  }
  //Avec pagination
  getAllDemandesPaginated(params: PaginationParams): Observable<PagedResponse<IDemandeAchat>> {
    let httpParams = new HttpParams()
      .set('page', params.page.toString())
      .set('size', params.size.toString())
      .set('sortBy', params.sortBy)
      .set('sortDirection', params.sortDirection);

    return this.http.get<PagedResponse<IDemandeAchat>>(
      `${environment.apiUrl + AUTH_API}/paginated`,
      { params: httpParams }
    );
  }

  /**
   * Récupère explicitement toutes les demandes (pour les administrateurs)
   */
  getAllDemandesForAdmin(): Observable<IDemandeAchat[]> {
    return this.http.get<IDemandeAchat[]>(environment.apiUrl + AUTH_API + '/all');
  }

  getDemandesAchatById(id: number | string): Observable<IDemandeAchat> {
    return this.http.get<IDemandeAchat>(environment.apiUrl + AUTH_API + "/" + id);
  }


  updateDemandeAchatStatut(id: number | string): Observable<any> {
    return this.http.put(environment.apiUrl + AUTH_API + "/" + id + "/statut", {}, httpOptions);
  }

  updateDemandeStatut(id: number | string, number: number): Observable<any> {
    return this.http.put(environment.apiUrl + AUTH_API + "/" + id + "/statut", {},{});
  }

  /**
   * Envoie une demande d'achat (change le statut à ENVOYE)
   */
  envoyerDemandeAchat(id: number): Observable<any> {
    return this.http.put(environment.apiUrl + AUTH_API + "/" + id + "/statut", {}, httpOptions);
  }

  /**
   * Récupère les demandes d'achat de l'utilisateur connecté
   */
  getCurrentUserDemandes(): Observable<IDemandeAchat[]> {
    return this.http.get<IDemandeAchat[]>(environment.apiUrl + AUTH_API);
  }
  /**
   * Récupère les demandes d'un utilisateur spécifique
   */
  getDemandesByUser(userId: number): Observable<IDemandeAchat[]> {
    return this.http.get<IDemandeAchat[]>(environment.apiUrl + AUTH_API + "/user/" + userId);
  }

  /**
   * Récupère les demandes d'une affaire spécifique
   */
  getDemandesByAffaire(affaireId: number): Observable<IDemandeAchat[]> {
    return this.http.get<IDemandeAchat[]>(environment.apiUrl + AUTH_API + "/affaire/" + affaireId);
  }

  /**
   * Met à jour une demande d'achat
   */
  updateDemandeAchat(id: number, demande: IDemandeAchat): Observable<any> {
    return this.http.put(environment.apiUrl + AUTH_API + "/" + id, demande, httpOptions);
  }
  uploadFilesForDemande(demandeId: number, files: FileList): Observable<any> {
    const formData = new FormData();

    // Ajouter tous les fichiers au FormData
    for (let i = 0; i < files.length; i++) {
      formData.append('files', files[i]);
    }

    // Pas de Content-Type header pour multipart/form-data
    return this.http.post(
      `${environment.apiUrl}${AUTH_API}/${demandeId}/files/upload`,
      formData
    );
  }


  /**
   * Upload de fichiers pour une demande (utilise KDN_FILE)
   */
  uploadFilesArrayForDemande(demandeId: number, files: File[]): Observable<any> {
    const formData = new FormData();
    files.forEach((file, index) => {
      formData.append('files', file, file.name);
      console.log(`Fichier ${index + 1} ajouté: ${file.name} (${file.size} bytes)`);
    });

    console.log('FormData créé avec', files.length, 'fichier(s)');

    // Utiliser l'endpoint qui gère KDN_FILE
    return this.http.post(
      `${environment.apiUrl}${AUTH_API}/${demandeId}/files/upload`,
      formData
    );
  }
  getFilesByDemande(demandeId: number): Observable<any[]> {
    console.log("Service: récupération fichiers pour demande", demandeId);
    return this.http.get<any[]>(`${environment.apiUrl}${AUTH_API}/${demandeId}/files`);
  }

  /**
   * Obtenir les informations sur les fichiers d'une demande
   */
  getDemandeFilesInfo(demandeId: number): Observable<any> {
    return this.http.get(`${environment.apiUrl}${AUTH_API}/${demandeId}/files/info`);
  }

  /**
   * Valider avant upload
   */
  validateFilesBeforeUpload(demandeId: number, fileCount: number): Observable<any> {
    return this.http.post(`${environment.apiUrl}${AUTH_API}/${demandeId}/files/validate`, null, {
      params: { fileCount: fileCount.toString() }
    });
  }

  /**
   * Supprimer un fichier d'une demande (depuis KDN_FILE)
   */
  deleteFileFromDemande(demandeId: number, fileId: number): Observable<any> {
    console.log("Service: suppression fichier", fileId, "de la demande", demandeId);
    return this.http.delete(`${environment.apiUrl}${AUTH_API}/${demandeId}/files/${fileId}`);
  }
  /**
   * Télécharger un fichier d'une demande d'achat (utilise KDN_FILE)
   */
  downloadFileFromDemande(fileId: number): Observable<Blob> {
    console.log("Service: téléchargement fichier", fileId);
    return this.http.get(`${environment.apiUrl}${AUTH_API}/files/${fileId}/download`, {
      responseType: 'blob'
    });
  }

}
