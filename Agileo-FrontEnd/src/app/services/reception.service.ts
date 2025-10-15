import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders, HttpParams} from "@angular/common/http";
import {IReception} from "../../interfaces/ireception";
import {Observable} from "rxjs";
import {environment} from "../../environments/environment";
import {PagedResponse, PaginationParams} from "../../interfaces/PagedResponse";
import {IDemandeAchat} from "../../interfaces/idemandeAchat";
import {IPieces} from "../../interfaces/ipiecesjoints";
import { KeycloakService } from 'keycloak-angular';

export interface ArticleDisponible {
  reference: string;
  designation: string;
  unite: string;
  quantiteCommandee: number;
  quantiteDejaRecue: number;
  quantiteDisponible: number;
  referenceBonCommande: string;
  nomFournisseur: string;
  familleStatistique1?: string;
  familleStatistique2?: string;
  familleStatistique3?: string;
  familleStatistique4?: string;
}

export interface ValidationQuantite {
  valide: boolean;
  message: string;
  quantiteMaximale: number;
  quantiteActuelle: number;
}

export interface ValidateQuantiteRequest {
  referenceArticle: string;
  quantiteDemandee: number;
  ligneReceptionId?: number;
}

// Interface harmonisée pour les fichiers (comme demandes d'achat)
export interface ReceptionFileResponseDTO {
  fileId: number;
  name: string;
  extension: string;
  fullFileName: string;
  size: number;
  sizeFormatted: string;
  uploadDate: string;
  downloadUrl: string;
  canDelete: boolean;
  canDownload: boolean;
  uploadedByNom?: string;
  uploadedBy?: string;
  category?: string;
  documentType?: string;
  alt?: string;
  nbOpen?: number;
}

const AUTH_API = 'receptions';
const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable({
  providedIn: 'root'
})
export class ReceptionService {

  constructor(
    private http: HttpClient,
    private keycloakService: KeycloakService
  ) { }

  // ==================== MÉTHODES EXISTANTES ====================

  addReception(reception: {
    affaireId: any;
    dateReception: any;
    referenceBl: any;
    dateBl: any;
    refFournisseur: any;
    nomFournisseur: any;
    idAgelio: string | null;
    statut: string
  }): Observable<IReception> {
    return this.http.post<IReception>(environment.apiUrl + AUTH_API, reception, httpOptions);
  }

  getAllReceptions(): Observable<IReception[]> {
    return this.http.get<IReception[]>(environment.apiUrl + AUTH_API);
  }

  getAllReceptionsPaginated(params: PaginationParams, search?: string): Observable<PagedResponse<IReception>> {
    let httpParams = new HttpParams()
      .set('page', params.page.toString())
      .set('size', params.size.toString())
      .set('sortBy', params.sortBy)
      .set('sortDirection', params.sortDirection);

    // ✅ AJOUTER le paramètre de recherche si présent
    if (search && search.trim() !== '') {
      httpParams = httpParams.set('search', search.trim());
    }

    return this.http.get<PagedResponse<IReception>>(
      `${environment.apiUrl}${AUTH_API}/paginated`,
      { params: httpParams }
    );
  }

  getArticlesDisponibles(receptionId: number): Observable<ArticleDisponible[]> {
    return this.http.get<ArticleDisponible[]>(`${environment.apiUrl}${AUTH_API}/${receptionId}/articles-disponibles`);
  }

  validerQuantiteArticle(receptionId: number, referenceArticle: string, quantiteDemandee: number, ligneReceptionId?: number): Observable<ValidationQuantite> {
    const request: ValidateQuantiteRequest = {
      referenceArticle: referenceArticle,
      quantiteDemandee: quantiteDemandee,
      ligneReceptionId: ligneReceptionId || undefined
    };

    return this.http.post<ValidationQuantite>(`${environment.apiUrl}${AUTH_API}/${receptionId}/valider-quantite`, request, httpOptions);
  }

  getReceptionById(id: string | null): Observable<IReception> {
    return this.http.get<IReception>(environment.apiUrl + AUTH_API + "/" + id);
  }

  deleteReception(id: number): Observable<any> {
    return this.http.delete<any>(`${environment.apiUrl}${AUTH_API}/${id}`, httpOptions);
  }

  getReceptionsByAffaire(affaireId: number): Observable<IReception[]> {
    return this.http.get<IReception[]>(`${environment.apiUrl}${AUTH_API}/affaire/${affaireId}`);
  }

  getReceptionsByUser(userId: number): Observable<IReception[]> {
    return this.http.get<IReception[]>(`${environment.apiUrl}${AUTH_API}/user/${userId}`);
  }

  getCurrentUserReceptions(): Observable<IReception[]> {
    return this.http.get<IReception[]>(`${environment.apiUrl}${AUTH_API}/current/receptions`);
  }

  updateReception(id: number, reception: any): Observable<any> {
    return this.http.put<any>(`${environment.apiUrl}${AUTH_API}/${id}`, reception);
  }

  // ==================== MÉTHODES HARMONISÉES POUR FICHIERS (COMME DEMANDES D'ACHAT) ====================

  /**
   * Upload de fichiers pour une réception (harmonisé avec demandes d'achat)
   */
  uploadFilesArrayForReception(receptionId: number, files: File[]): Observable<any> {
    const formData = new FormData();
    files.forEach((file, index) => {
      formData.append('files', file, file.name);
      console.log(`Fichier ${index + 1} ajouté: ${file.name} (${file.size} bytes)`);
    });

    console.log('FormData créé avec', files.length, 'fichier(s)');

    // Utiliser l'endpoint qui gère KDN_FILE (harmonisé avec demandes d'achat)
    return this.http.post(
      `${environment.apiUrl}${AUTH_API}/${receptionId}/files/upload`,
      formData
    );
  }

  /**
   * Récupérer les fichiers d'une réception (harmonisé avec demandes d'achat)
   */
  getFilesByReception(receptionId: number): Observable<ReceptionFileResponseDTO[]> {
    console.log("Service: récupération fichiers pour réception", receptionId);
    return this.http.get<ReceptionFileResponseDTO[]>(`${environment.apiUrl}${AUTH_API}/${receptionId}/files`);
  }

  /**
   * Supprimer un fichier d'une réception (harmonisé avec demandes d'achat)
   */
  deleteFileFromReception(receptionId: number, fileId: number): Observable<any> {
    console.log("Service: suppression fichier", fileId, "de la réception", receptionId);
    return this.http.delete(`${environment.apiUrl}${AUTH_API}/${receptionId}/files/${fileId}`);
  }

  /**
   * Télécharger un fichier d'une réception (harmonisé avec demandes d'achat)
   */
  downloadFileFromReception(fileId: number): Observable<Blob> {
    console.log("Service: téléchargement fichier", fileId);
    return this.http.get(`${environment.apiUrl}${AUTH_API}/files/${fileId}/download`, {
      responseType: 'blob'
    });
  }

  /**
   * Prévisualiser un fichier d'une réception
   */
  previewFileFromReception(fileId: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}${AUTH_API}/files/${fileId}/download`, {
      responseType: 'blob'
    });
  }

  /**
   * Obtenir les informations sur les fichiers d'une réception
   */
  getReceptionFilesInfo(receptionId: number): Observable<any> {
    return this.http.get(`${environment.apiUrl}${AUTH_API}/${receptionId}/files/info`);
  }

  /**
   * Valider avant upload (harmonisé avec demandes d'achat)
   */
  validateFilesBeforeUpload(receptionId: number, fileCount: number): Observable<any> {
    return this.http.post(`${environment.apiUrl}${AUTH_API}/${receptionId}/files/validate`, null, {
      params: { fileCount: fileCount.toString() }
    });
  }

  // ==================== MÉTHODES LEGACY POUR RÉTROCOMPATIBILITÉ ====================

  /**
   * @deprecated Utiliser uploadFilesArrayForReception à la place
   */
  uploadFiles(receptionId: number, files: File[]): Observable<any> {
    return this.uploadFilesArrayForReception(receptionId, files);
  }

  /**
   * @deprecated Utiliser uploadFilesArrayForReception à la place
   */
  uploadFilesForReception(receptionId: number, files: File[]): Observable<any> {
    return this.uploadFilesArrayForReception(receptionId, files);
  }

  /**
   * @deprecated Utiliser downloadFileFromReception à la place
   */
  downloadFile(fileId: number): Observable<Blob> {
    return this.downloadFileFromReception(fileId);
  }

  /**
   * @deprecated Utiliser previewFileFromReception à la place
   */
  previewFile(fileId: number): Observable<Blob> {
    return this.previewFileFromReception(fileId);
  }

  /**
   * @deprecated Utiliser getFilesByReception à la place
   */
  getReceptionFiles(receptionId: number): Observable<any[]> {
    return this.getFilesByReception(receptionId);
  }

  /**
   * @deprecated Utiliser deleteFileFromReception à la place
   */
  deleteReceptionFile(receptionId: number, fileId: number): Observable<any> {
    return this.deleteFileFromReception(receptionId, fileId);
  }

  /**
   * @deprecated Utiliser downloadFileFromReception à la place
   */
  downloadReceptionFileById(receptionId: number, fileId: number): Observable<Blob> {
    return this.downloadFileFromReception(fileId);
  }

  // ==================== MÉTHODES LEGACY POUR ANCIEN SYSTÈME ====================

  getPieceJointByDemandeAchatID(receptionId: number): Observable<IPieces[]> {
    return this.http.get<IPieces[]>(environment.apiUrl + AUTH_API + "/" + receptionId + "/pieces");
  }

  deletePieceJointe(pieceId: number): Observable<any> {
    return this.http.delete(environment.apiUrl + AUTH_API + "/pieces" + "/" + pieceId);
  }

  downloadReceptionFile(fileId: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl + AUTH_API}/files/${fileId}/download`, {
      responseType: 'blob'
    });
  }

}
