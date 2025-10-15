import { Injectable } from '@angular/core';
import {ToastrService} from "ngx-toastr";
@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  constructor(private toastr: ToastrService) { }


  fileDownloadStarted(fileName: string) {
    this.showInfo(`Téléchargement de "${fileName}" en cours...`, 'Téléchargement');
  }

  fileDownloadCompleted(fileName: string) {
    this.showSuccess(`"${fileName}" téléchargé avec succès`, 'Téléchargement terminé');
  }

  fileDownloadError(fileName: string, error?: string) {
    const message = error
      ? `Erreur lors du téléchargement de "${fileName}": ${error}`
      : `Impossible de télécharger "${fileName}"`;
    this.showError(message, 'Erreur de téléchargement');
  }

  filePreviewError(fileName: string) {
    this.showWarning(`Impossible de prévisualiser "${fileName}"`, 'Aperçu non disponible');
  }

  authenticationError() {
    this.showError('Votre session a expiré. Veuillez vous reconnecter.', 'Authentification requise');
  }

  fileNotFound(fileName: string) {
    this.showError(`Le fichier "${fileName}" est introuvable`, 'Fichier non trouvé');
  }

  connectionError() {
    this.showError('Impossible de se connecter au serveur. Vérifiez votre connexion.', 'Erreur de connexion');
  }

  showSuccess(message: string | undefined, title: string | undefined){
    this.toastr.success(message, title);
  }

  showError(message: string | undefined, title: string | undefined){
    this.toastr.error(message, title);
  }

  showInfo(message: string | undefined, title: string | undefined){
    this.toastr.info(message, title)
  }

  showWarning(message: string | undefined, title: string | undefined){
    this.toastr.warning(message, title)
  }
}
