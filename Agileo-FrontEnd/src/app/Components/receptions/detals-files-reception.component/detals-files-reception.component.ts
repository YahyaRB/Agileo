// details-files-reception.component.ts - VERSION HARMONISÉE AVEC DEMANDES D'ACHAT - CORRIGÉE

import { Component, Input, OnInit, OnDestroy, OnChanges, SimpleChanges } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { environment } from '../../../../environments/environment';
import { ReceptionService } from '../../../services/reception.service';
import { KeycloakService } from 'keycloak-angular';
import { NotificationService } from '../../../services/notification.service';
import {FileSyncService} from "../../../services/file-sync.service";
import {Subscription} from "rxjs";

// Interface harmonisée (comme demandes d'achat)
interface ReceptionFileResponse {
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

@Component({
  selector: 'app-details-files-reception',
  templateUrl: './detals-files-reception.component.html',
  styleUrls: ['./detals-files-reception.component.css']
})
export class DetailsFilesReceptionComponent implements OnInit, OnDestroy, OnChanges {
  @Input() receptionId!: number;

  // États harmonisés (comme demandes d'achat)
  listPiecesJointes: ReceptionFileResponse[] = [];
  selectedFiles: File[] = [];
  isDragOver = false;
  isUploading = false;
  maxFiles = 3;
  loading = false;
  error = false;
  errorMessage = '';
  private fileSyncSubscription?: Subscription;
  // Variables du modal (harmonisées)
  isModalOpen = false;
  selectedFileName = '';
  previewUrl: SafeResourceUrl | null = null;
  currentFileId: number | null = null;
  previewBlobUrl: string | null = null;
  previewLoading = false;
  private refreshEventListener?: (event: CustomEvent) => void;
  constructor(
    private http: HttpClient,
    private sanitizer: DomSanitizer,
    private receptionService: ReceptionService,
    private fileSyncService: FileSyncService, // AJOUTER ce service
    private keycloakService: KeycloakService,
    private notifyService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadFilesIfReady();
    this.subscribeToFileSync(); // AJOUTER
  }



  ngOnChanges(changes: SimpleChanges): void {
    if (changes['receptionId']) {
      const currentId = changes['receptionId'].currentValue;
      const previousId = changes['receptionId'].previousValue;

      console.log('Changement détecté - Reception ID:', previousId, '->', currentId);

      if (currentId && currentId !== previousId) {
        // Vider les données précédentes
        this.listPiecesJointes = [];
        this.selectedFiles = [];
        this.error = false;

        // Recharger avec le nouvel ID
        this.loadFilesIfReady();
      }
    }
  }

  private loadFilesIfReady(): void {
    if (this.receptionId && this.receptionId > 0) {
      this.loadFiles();
    } else {
      setTimeout(() => {
        if (this.receptionId && this.receptionId > 0) {
          this.loadFiles();
        } else {
          this.error = true;
          this.errorMessage = 'ID de réception non fourni';
        }
      }, 500);
    }
  }

  // ==================== CHARGEMENT DES FICHIERS ====================
  async loadFiles() {
    if (!this.receptionId || this.receptionId <= 0) {
      console.error('Cannot load files: invalid receptionId:', this.receptionId);
      return;
    }

    this.loading = true;
    this.error = false;
    this.errorMessage = '';

    try {
      console.log('Chargement des fichiers pour réception:', this.receptionId);

      // NOUVEAU : Ajouter un timestamp pour éviter le cache
      const timestamp = Date.now();
      console.log('Timestamp anti-cache:', timestamp);

      this.receptionService.getFilesByReception(this.receptionId).subscribe({
        next: (files) => {
          // CORRECTION : Utiliser spread operator pour forcer la détection
          this.listPiecesJointes = [...(files || [])];
          console.log('Fichiers chargés (details-files):', this.listPiecesJointes.length);
          console.log('Détails fichiers:', this.listPiecesJointes);

          this.loading = false;
          this.error = false;

          // FORCER la détection des changements
          if ((this as any).cdr) {
            (this as any).cdr.detectChanges();
          }
        },
        error: (error) => {
          console.error('Erreur lors du chargement des fichiers (details-files):', error);

          // NOUVEAU : Retry automatique en cas d'erreur
          if (!this.error) { // Éviter les boucles infinies
            console.log('Tentative de rechargement après erreur...');
            setTimeout(() => {
              this.loadFiles();
            }, 2000);
          }

          this.listPiecesJointes = [];
          this.loading = false;
          this.error = true;
          this.errorMessage = 'Erreur lors du chargement des fichiers';
        }
      });
    } catch (error) {
      console.error('Erreur lors du chargement des fichiers (details-files):', error);
      this.listPiecesJointes = [];
      this.loading = false;
      this.error = true;
      this.errorMessage = 'Erreur lors du chargement des fichiers';
    }
  }
  private subscribeToFileSync(): void {
    this.fileSyncSubscription = this.fileSyncService.fileSync$.subscribe(event => {
      if (!event || !this.receptionId || event.receptionId !== this.receptionId) {
        return;
      }

      console.log('🔄 DetailsFiles - Événement reçu:', event);
      console.log('Action:', event.action, 'pour réception:', event.receptionId);

      // Recharger les fichiers avec un délai
      setTimeout(() => {
        this.forceReloadFiles();
      }, 500);
    });
  }



  // ==================== GESTION DES FICHIERS ====================

  /**
   * Visualiser un fichier
   */
  async viewFile(fileId: number, fileName: string) {
    console.log('Prévisualisation fichier:', fileId, fileName);

    this.cleanupPreview();
    this.selectedFileName = fileName;
    this.currentFileId = fileId;
    this.previewLoading = true;
    this.isModalOpen = true;
    this.openModal();

    if (this.canPreview(fileName)) {
      try {
        this.receptionService.previewFileFromReception(fileId).subscribe({
          next: (blob: Blob) => {
            this.previewBlobUrl = URL.createObjectURL(blob);
            this.previewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.previewBlobUrl);
            this.previewLoading = false;
          },
          error: (error) => {
            console.error('Erreur prévisualisation:', error);
            this.previewUrl = null;
            this.previewLoading = false;
          }
        });
      } catch (error) {
        console.error('Erreur lors de la prévisualisation:', error);
        this.previewUrl = null;
        this.previewLoading = false;
      }
    } else {
      this.previewUrl = null;
      this.previewLoading = false;
    }
  }

  /**
   * Télécharger un fichier
   */
  async downloadFile(fileId: number, fileName: string) {
    console.log('Téléchargement fichier:', fileName);
    try {
      const token = await this.keycloakService.getToken();
      const headers = new HttpHeaders({
        'Authorization': `Bearer ${token}`
      });

      const downloadUrl = `${environment.apiUrl}receptions/files/${fileId}/download`;

      this.http.get(downloadUrl, {
        headers: headers,
        responseType: 'blob',
        observe: 'response'
      }).subscribe({
        next: (response: any) => {
          const blob = new Blob([response.body]);
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = fileName;
          link.click();
          window.URL.revokeObjectURL(url);
        },
        error: (error) => {
          console.error('Erreur téléchargement:', error);
          this.notifyService.showError('Erreur lors du téléchargement', 'Erreur');
        }
      });
    } catch (error) {
      console.error('Erreur:', error);
      this.notifyService.showError('Erreur technique', 'Erreur');
    }
  }

  /**
   * Supprimer un fichier
   */
  deleteFile(fileId: number): void {
    if (!confirm('Êtes-vous sûr de vouloir supprimer ce fichier définitivement ?')) {
      return;
    }

    this.receptionService.deleteFileFromReception(this.receptionId, fileId).subscribe({
      next: () => {
        this.notifyService.showSuccess('Fichier supprimé avec succès', 'Supprimé');

        // NOUVEAU : Émettre un événement de synchronisation
        const event = new CustomEvent('receptionFilesUpdated', {
          detail: {
            receptionId: this.receptionId,
            timestamp: Date.now(),
            action: 'delete'
          }
        });
        window.dispatchEvent(event);

        // Recharger la liste
        setTimeout(() => {
          this.loadFiles();
        }, 500);
      },
      error: (error) => {
        console.error('Erreur suppression:', error);
        this.notifyService.showError('Erreur lors de la suppression', 'Erreur');
      }
    });
  }


  /**
   * Télécharger le fichier actuellement affiché
   */
  async downloadCurrentFile() {
    if (this.currentFileId && this.selectedFileName) {
      await this.downloadFile(this.currentFileId, this.selectedFileName);
    }
  }

  // ==================== GESTION DES UPLOADS ====================

  /**
   * Sélectionner des fichiers
   */
  onFileSelect(event: any): void {
    const files = Array.from(event.target.files as FileList);
    this.handleFileSelection(files);
    event.target.value = '';
  }

  /**
   * Alias pour compatibilité avec le template
   */
  onFileSelected(event: any): void {
    this.onFileSelect(event);
  }

  /**
   * Drag & Drop
   */
  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = false;

    if (event.dataTransfer?.files) {
      const files = Array.from(event.dataTransfer.files);
      this.handleFileSelection(files);
    }
  }
  public refreshFiles(): void {
    console.log('Refresh externe demandé pour details-files-reception');
    this.forceReloadFiles();
  }
  public forceReloadFiles(): void {
    console.log('🔄 RECHARGEMENT FORCÉ DÉTAILS-FILES');

    if (!this.receptionId || this.receptionId <= 0) {
      console.warn('ID de réception invalide');
      return;
    }

    // Vider et recharger
    this.listPiecesJointes = [];
    this.loading = true;
    this.error = false;

    setTimeout(() => {
      this.loadFiles();
    }, 300);
  }

  public updateFiles(files: any[]): void {
    console.log('📁 Mise à jour directe des fichiers:', files.length);
    this.listPiecesJointes = [...files];
    this.loading = false;
    this.error = false;

    if ((this as any).cdr) {
      (this as any).cdr.detectChanges();
    }
  }


// 5. AMÉLIORER la méthode loadFiles avec retry automatique

// 10. AJOUTER une méthode pour vérifier la synchronisation
  public checkSynchronization(): void {
    console.log('=== VÉRIFICATION SYNCHRONISATION ===');
    console.log('Reception ID:', this.receptionId);
    console.log('Nombre de fichiers:', this.listPiecesJointes.length);
    console.log('Loading:', this.loading);
    console.log('Error:', this.error);
    console.log('Fichiers:', this.listPiecesJointes.map(f => f.fullFileName || f.name));
  }
  /**
   * Traiter la sélection de fichiers
   */
  private handleFileSelection(files: File[]): void {
    console.log("Fichiers sélectionnés:", files);

    // Vérifier le nombre total
    const totalFiles = this.listPiecesJointes.length + this.selectedFiles.length + files.length;

    if (totalFiles > this.maxFiles) {
      const remaining = this.maxFiles - this.listPiecesJointes.length - this.selectedFiles.length;
      this.notifyService.showError(
        `Maximum ${this.maxFiles} fichiers autorisés. Vous pouvez encore ajouter ${remaining} fichier(s).`,
        'Limite atteinte'
      );
      return;
    }

    // Valider et ajouter les fichiers
    const validFiles: File[] = [];
    const errors: string[] = [];

    files.forEach(file => {
      const validation = this.validateFile(file);
      if (validation.valid) {
        if (!this.selectedFiles.some(f => f.name === file.name && f.size === file.size)) {
          validFiles.push(file);
        } else {
          errors.push(`Fichier "${file.name}" déjà sélectionné`);
        }
      } else {
        errors.push(`${file.name}: ${validation.error}`);
      }
    });

    this.selectedFiles = [...this.selectedFiles, ...validFiles];

    if (errors.length > 0) {
      this.notifyService.showError(errors.join('\n'), 'Fichiers rejetés');
    }

    if (validFiles.length > 0) {
      this.notifyService.showSuccess(
        `${validFiles.length} fichier(s) sélectionné(s)`,
        'Fichiers ajoutés'
      );
    }
  }

  /**
   * Valider un fichier
   */
  private validateFile(file: File): {valid: boolean, error?: string} {
    const maxSize = 50 * 1024 * 1024; // 50MB
    if (file.size > maxSize) {
      return {valid: false, error: 'Fichier trop volumineux (max 50MB)'};
    }

    const allowedExtensions = ['pdf', 'jpg', 'jpeg', 'png', 'doc', 'docx', 'xls', 'xlsx'];
    const extension = file.name.split('.').pop()?.toLowerCase();

    if (!extension || !allowedExtensions.includes(extension)) {
      return {
        valid: false,
        error: `Extension non autorisée. Extensions acceptées: ${allowedExtensions.join(', ')}`
      };
    }

    return {valid: true};
  }

  /**
   * Supprimer un fichier de la sélection
   */
  removeSelectedFile(index: number): void {
    this.selectedFiles.splice(index, 1);
    this.notifyService.showSuccess('Fichier retiré de la sélection', 'Supprimé');
  }

  /**
   * Vider la sélection
   */
  clearSelectedFiles(): void {
    this.selectedFiles = [];
    this.notifyService.showSuccess('Sélection vidée', 'Effacé');
  }

  /**
   * Upload des fichiers sélectionnés - CORRIGÉ
   */
  async uploadSelectedFiles(): Promise<void> {
    if (this.selectedFiles.length === 0) return;

    this.isUploading = true;

    try {
      const response = await this.receptionService.uploadFilesArrayForReception(
        this.receptionId,
        this.selectedFiles
      ).toPromise();

      if (response && Array.isArray(response)) {
        this.notifyService.showSuccess(
          `${response.length} fichier(s) uploadé(s) avec succès`,
          'Upload réussi'
        );

        // Vider la sélection
        this.clearSelectedFiles();

        // Recharger et émettre
        setTimeout(() => {
          this.loadFiles();
          this.fileSyncService.notifyFileUpload(this.receptionId);
        }, 1000);
      }
    } catch (error) {
      console.error('Erreur upload:', error);
      this.notifyService.showError('Erreur lors de l\'upload', 'Erreur');
    } finally {
      this.isUploading = false;
    }
  }

  ngOnDestroy(): void {
    if (this.fileSyncSubscription) {
      this.fileSyncSubscription.unsubscribe();
    }

    // Nettoyage existant
    if (this.isModalOpen) {
      this.closeModal();
    }
    this.cleanupPreview();

    if (this.refreshEventListener) {
      window.removeEventListener('receptionFilesUpdated', this.refreshEventListener as EventListener);
      window.removeEventListener('refreshFilesRequest', this.refreshEventListener as EventListener);
    }
  }


  // ==================== GESTION DU MODAL ====================

  private openModal() {
    const modalElement = document.getElementById('previewModal');
    if (modalElement) {
      modalElement.style.display = 'block';
      modalElement.classList.add('show');
      document.body.classList.add('modal-open');

      const backdrop = document.createElement('div');
      backdrop.className = 'modal-backdrop fade show';
      backdrop.onclick = () => this.closeModal();
      document.body.appendChild(backdrop);
    }
  }

  closeModal() {
    this.isModalOpen = false;

    const modalElement = document.getElementById('previewModal');
    if (modalElement) {
      modalElement.style.display = 'none';
      modalElement.classList.remove('show');
    }

    const backdrop = document.querySelector('.modal-backdrop');
    if (backdrop) {
      backdrop.remove();
    }

    document.body.classList.remove('modal-open');
    document.body.style.overflow = '';
    document.body.style.paddingRight = '';

    this.cleanupPreview();
    this.resetModalData();
  }

  private cleanupPreview() {
    if (this.previewBlobUrl) {
      URL.revokeObjectURL(this.previewBlobUrl);
      this.previewBlobUrl = null;
    }
    this.previewUrl = null;
  }

  private resetModalData() {
    this.selectedFileName = '';
    this.currentFileId = null;
    this.previewLoading = false;
  }

  // ==================== UTILITAIRES ====================

  /**
   * Vérifier si on peut encore ajouter des fichiers
   */
  canAddMoreFiles(): boolean {
    const currentFilesCount = this.listPiecesJointes ? this.listPiecesJointes.length : 0;
    const selectedFilesCount = this.selectedFiles ? this.selectedFiles.length : 0;
    return (currentFilesCount + selectedFilesCount) < this.maxFiles;
  }

  /**
   * Obtenir le nombre de fichiers restants
   */
  getRemainingFilesCount(): number {
    const currentFilesCount = this.listPiecesJointes ? this.listPiecesJointes.length : 0;
    const selectedFilesCount = this.selectedFiles ? this.selectedFiles.length : 0;
    return Math.max(0, this.maxFiles - currentFilesCount - selectedFilesCount);
  }

  /**
   * Obtenir l'extension d'un fichier
   */
  getFileExtension(fileName: string): string {
    return fileName.split('.').pop() || '';
  }

  /**
   * Formater la taille d'un fichier
   */
  formatFileSize(bytes: number): string {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  /**
   * Vérifier si un fichier peut être prévisualisé
   */
  canPreview(fileName: string): boolean {
    const extension = this.getFileExtension(fileName).toLowerCase();
    const previewableExtensions = ['pdf', 'txt', 'jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg'];
    return previewableExtensions.includes(extension);
  }

  /**
   * Formater une date
   */
  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    } catch (error) {
      return 'N/A';
    }
  }

  /**
   * Obtenir l'icône d'un fichier
   */
  getFileIcon(filename: string): string {
    const extension = filename.split('.').pop()?.toLowerCase();
    switch (extension) {
      case 'pdf': return 'fa fa-file-pdf-o text-danger';
      case 'doc':
      case 'docx': return 'fa fa-file-word-o text-primary';
      case 'xls':
      case 'xlsx': return 'fa fa-file-excel-o text-success';
      case 'jpg':
      case 'jpeg':
      case 'png':
      case 'gif':
      case 'bmp': return 'fa fa-file-image-o text-info';
      default: return 'fa fa-file-o text-muted';
    }
  }

  /**
   * Alias pour compatibilité avec le template
   */
  getFileIconClass(filename: string): string {
    return this.getFileIcon(filename);
  }

  // ==================== GESTION DES ÉVÉNEMENTS ====================

  onModalBackdropClick(event: Event) {
    const target = event.target as HTMLElement;
    if (target.classList.contains('modal') || target.classList.contains('modal-backdrop')) {
      this.closeModal();
    }
  }

  onModalContentClick(event: Event) {
    event.stopPropagation();
  }


}
