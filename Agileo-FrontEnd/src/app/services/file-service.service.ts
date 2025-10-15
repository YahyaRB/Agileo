import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { KeycloakService } from 'keycloak-angular';

export interface FileResponseDTO {
  fileId: number;
  name: string;
  extension: string;
  fullFileName: string;
  size: number;
  sizeFormatted: string;
  downloadUrl: string;
  canDelete: boolean;
  canDownload: boolean;
  uploadDate: string;
}

@Injectable({
  providedIn: 'root'
})
export class FileService {

  constructor(
    private http: HttpClient,
    private keycloakService: KeycloakService
  ) {}

  // Méthode pour prévisualiser un fichier (nouveau système KdnFile)
  async previewFile(fileId: number): Promise<Observable<Blob>> {
    try {
      const token = await this.keycloakService.getToken();
      const headers = new HttpHeaders({
        'Authorization': `Bearer ${token}`
      });

      const url = `${environment.apiUrl}receptions/files/${fileId}/download`;

      return this.http.get(url, {
        headers,
        responseType: 'blob'
      });
    } catch (error) {
      console.error('Erreur lors de la prévisualisation:', error);
      throw error;
    }
  }

  // Méthode pour télécharger un fichier (nouveau système KdnFile)
  async downloadFile(fileId: number): Promise<void> {
    try {
      const token = await this.keycloakService.getToken();
      const headers = new HttpHeaders({
        'Authorization': `Bearer ${token}`
      });

      const url = `${environment.apiUrl}receptions/files/${fileId}/download`;

      this.http.get(url, {
        headers,
        responseType: 'blob'
      }).subscribe({
        next: (blob) => {
          // Créer un lien de téléchargement
          const downloadUrl = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = downloadUrl;
          link.download = `fichier_${fileId}`;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          window.URL.revokeObjectURL(downloadUrl);
        },
        error: (error) => {
          console.error('Erreur téléchargement:', error);
          throw error;
        }
      });
    } catch (error) {
      console.error('Erreur lors du téléchargement:', error);
      throw error;
    }
  }

  // Méthodes pour l'ancien système PieceJointe (rétrocompatibilité)
  async previewPiece(pieceId: number): Promise<Observable<Blob>> {
    try {
      const token = await this.keycloakService.getToken();
      const headers = new HttpHeaders({
        'Authorization': `Bearer ${token}`
      });

      const url = `${environment.apiUrl}receptions/pieces/${pieceId}/view`;

      return this.http.get(url, {
        headers,
        responseType: 'blob'
      });
    } catch (error) {
      console.error('Erreur lors de la prévisualisation de pièce:', error);
      throw error;
    }
  }

  async downloadPiece(pieceId: number): Promise<void> {
    try {
      const token = await this.keycloakService.getToken();
      const headers = new HttpHeaders({
        'Authorization': `Bearer ${token}`
      });

      const url = `${environment.apiUrl}receptions/pieces/${pieceId}/download`;

      this.http.get(url, {
        headers,
        responseType: 'blob'
      }).subscribe({
        next: (blob) => {
          const downloadUrl = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = downloadUrl;
          link.download = `piece_${pieceId}`;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          window.URL.revokeObjectURL(downloadUrl);
        },
        error: (error) => {
          console.error('Erreur téléchargement pièce:', error);
          throw error;
        }
      });
    } catch (error) {
      console.error('Erreur lors du téléchargement de pièce:', error);
      throw error;
    }
  }

  // Méthode pour récupérer les fichiers d'une réception
  async getReceptionFiles(receptionId: number): Promise<Observable<FileResponseDTO[]>> {
    try {
      const token = await this.keycloakService.getToken();
      const headers = new HttpHeaders({
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      });

      const url = `${environment.apiUrl}receptions/${receptionId}/files`;

      return this.http.get<FileResponseDTO[]>(url, { headers });
    } catch (error) {
      console.error('Erreur lors de la récupération des fichiers:', error);
      throw error;
    }
  }
}
