// Remplacer le contenu de update-demande-achat.component.ts

import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import {Affaire} from "../../../../interfaces/iaffaire";
import {AffaireServiceService} from "../../../services/affaire-service.service";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {IDemandeAchat} from "../../../../interfaces/idemandeAchat";
import {DemandeAchatService} from "../../../services/demande-achat.service";
import {NotificationService} from "../../../services/notification.service";
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {IFiles} from "../../../../interfaces/ifiles";
import {IPieces} from "../../../../interfaces/ipiecesjoints";
import {environment} from "../../../../environments/environment";
import {DomSanitizer, SafeResourceUrl} from "@angular/platform-browser";
import { KeycloakService } from 'keycloak-angular';

@Component({
  selector: 'app-update-demande-achat',
  templateUrl: './update-demande-achat.component.html',
  styleUrls: ['./update-demande-achat.component.css']
})
export class UpdateDemandeAchatComponent implements OnInit, OnChanges {
  @ViewChild('closebutton', {static: false}) closebutton!: ElementRef;
  @Output() ajoutEffectue = new EventEmitter<void>();
  @Input() public demandeToUpdate!: IDemandeAchat;

  // Propriétés existantes
  existenceAffaireForDemande!: Affaire;
  affairesDisponibles: Affaire[] = [];
  myFormUpdate!: FormGroup;
  isLoading = false;
  pieceUrl: SafeResourceUrl | null = null;
  AUTH = 'demandes-achat';

  // NOUVELLES propriétés pour le système KDN_FILE
  listPiecesJointes: any[] = []; // Fichiers existants (KDN_FILE)
  selectedFiles: File[] = []; // Nouveaux fichiers sélectionnés
  isDragOver = false;
  isUploading = false;
  maxFiles = 3;

  // Propriétés legacy (à conserver pour transition)
  allPieces: any[] = [];
  selectedFilesLegacy: IFiles[] = [];
  protected piecesJoints: IPieces[];
  piecesToDelete: number[] = [];

  constructor(
    private affaireService: AffaireServiceService,
    private demandeAchatService: DemandeAchatService,
    private notifyService: NotificationService,
    private sanitizer: DomSanitizer,
    private http: HttpClient,
    private formBuilder: FormBuilder,
    private keycloakService: KeycloakService
  ) {
    this.initMyUpdateForm();
  }

  ngOnInit(): void {
    this.initMyUpdateForm();
    this.loadAffaires();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['demandeToUpdate'] && this.demandeToUpdate) {
      this.selectedFiles = [];
      this.selectedFilesLegacy = [];
      this.piecesToDelete = [];
      this.listPiecesJointes = [];

      if (this.affairesDisponibles && this.affairesDisponibles.length > 0) {
        this.affectDemandeToForm(this.demandeToUpdate);
        this.getAffaireById();
        this.loadDemandeFiles(); // NOUVEAU: Charger les fichiers KDN_FILE
        // this.getPiecesJoint(); // LEGACY: Garder pour transition
      }
    }
  }

  // ==================== NOUVELLES MÉTHODES POUR KDN_FILE ====================

  /**
   * Charger les fichiers de la demande (système KDN_FILE)
   */
  private loadDemandeFiles(): void {
    if (!this.demandeToUpdate?.id) return;

    console.log("Chargement des fichiers KDN_FILE pour demande:", this.demandeToUpdate.id);

    this.demandeAchatService.getFilesByDemande(this.demandeToUpdate.id)
      .subscribe({
        next: (files) => {
          this.listPiecesJointes = files || [];
          console.log("Fichiers KDN_FILE chargés:", this.listPiecesJointes);
        },
        error: (error) => {
          console.error("Erreur chargement fichiers KDN_FILE:", error);
          this.listPiecesJointes = [];
        }
      });
  }

  /**
   * Gérer la sélection de fichiers
   */
  onFileSelect(event: any): void {
    const files = Array.from(event.target.files as FileList);
    this.handleFileSelection(files);
    event.target.value = '';
  }

  /**
   * Gérer le drag & drop
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

  /**
   * Traiter la sélection de fichiers avec validation
   */
  private handleFileSelection(files: File[]): void {
    console.log("Fichiers sélectionnés:", files);

    // Vérifier si la demande est modifiable
    if (this.demandeToUpdate && this.isDemandeEnvoyee()) {
      this.notifyService.showError('Impossible d\'ajouter des fichiers à une demande envoyée', 'Action interdite');
      return;
    }

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
   * Valider un fichier individuel
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
   * Supprimer un fichier existant
   */
  deleteExistingFile(fileId: number): void {
    if (!this.demandeToUpdate?.id) return;

    if (confirm('Êtes-vous sûr de vouloir supprimer ce fichier ?')) {
      this.demandeAchatService.deleteFileFromDemande(this.demandeToUpdate.id, fileId)
        .subscribe({
          next: () => {
            this.notifyService.showSuccess('Fichier supprimé avec succès', 'Supprimé');
            this.loadDemandeFiles(); // Recharger la liste
          },
          error: (err) => {
            console.error('Erreur suppression:', err);
            this.notifyService.showError('Erreur lors de la suppression', 'Erreur');
          }
        });
    }
  }

  /**
   * Upload des nouveaux fichiers sélectionnés
   */
  private async uploadNewFiles(): Promise<boolean> {
    if (!this.demandeToUpdate?.id || this.selectedFiles.length === 0) {
      return true; // Pas de fichiers à uploader
    }

    this.isUploading = true;

    try {
      console.log("Upload de", this.selectedFiles.length, "nouveaux fichiers");

      await this.demandeAchatService.uploadFilesArrayForDemande(
        this.demandeToUpdate.id,
        this.selectedFiles
      ).toPromise();

      this.notifyService.showSuccess(
        `${this.selectedFiles.length} fichier(s) uploadé(s) avec succès`,
        'Upload réussi'
      );

      this.selectedFiles = [];
      return true;

    } catch (error) {
      console.error('Erreur upload:', error);
      this.notifyService.showError('Erreur lors de l\'upload des fichiers', 'Erreur');
      return false;
    } finally {
      this.isUploading = false;
    }
  }

  /**
   * Télécharger un fichier existant
   */
  async downloadFile(file: any): Promise<void> {
    try {
      const token = await this.keycloakService.getToken();
      if (!token) {
        this.notifyService.showError('Session expirée', 'Erreur');
        return;
      }

      const downloadUrl = `${environment.apiUrl}demandes-achat/files/${file.fileId || file.id}/download`;
      const headers = new HttpHeaders({
        'Authorization': `Bearer ${token}`
      });

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
          link.download = file.fullFileName || file.name || 'fichier';
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

  // ==================== MÉTHODES UTILITAIRES ====================

  /**
   * Vérifier si on peut encore ajouter des fichiers
   */
  canAddMoreFiles(): boolean {
    return this.listPiecesJointes.length + this.selectedFiles.length < this.maxFiles &&
      !this.isDemandeEnvoyee();
  }

  /**
   * Obtenir le nombre de fichiers restants
   */
  getRemainingFilesCount(): number {
    return this.maxFiles - this.listPiecesJointes.length - this.selectedFiles.length;
  }

  /**
   * Vérifier si la demande est envoyée
   */
  isDemandeEnvoyee(): boolean {
    return this.demandeToUpdate?.statut !== null &&
      this.demandeToUpdate?.statut !== undefined &&
      this.demandeToUpdate?.statut !== 0;
  }

  /**
   * Formatage de la taille de fichier
   */
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  /**
   * Obtenir l'icône selon l'extension
   */
  getFileIcon(filename: string): string {
    const extension = filename.split('.').pop()?.toLowerCase();
    switch (extension) {
      case 'pdf': return 'fa-file-pdf text-danger';
      case 'doc':
      case 'docx': return 'fa-file-word text-primary';
      case 'xls':
      case 'xlsx': return 'fa-file-excel text-success';
      case 'jpg':
      case 'jpeg':
      case 'png': return 'fa-file-image text-info';
      default: return 'fa-file text-muted';
    }
  }

  // ==================== MÉTHODES EXISTANTES MODIFIÉES ====================

  async onUpdateDemandeAchat() {
    console.log("Mise à jour de la demande d'achat...");
    const { affaire, delai, commentaire } = this.myFormUpdate.value;
    const id = this.demandeToUpdate.id;

    this.isLoading = true;

    try {
      // 1. Upload des nouveaux fichiers d'abord
      const uploadSuccess = await this.uploadNewFiles();
      if (!uploadSuccess) {
        this.isLoading = false;
        return;
      }

      // 2. Mettre à jour la demande d'achat
      const payload: IDemandeAchat = {
        chantier: affaire.affaire,
        delaiSouhaite: delai ? new Date(delai).toISOString() : null,
        commentaire: commentaire
      };

      await this.demandeAchatService.updateDemandeAchat(id, payload).toPromise();

      this.notifyService.showSuccess('Demande d\'achat modifiée avec succès !', 'Modifiée');
      this.ajoutEffectue.emit();
      this.closebutton.nativeElement.click();

    } catch (error) {
      console.error("Erreur lors de la mise à jour:", error);
      this.notifyService.showError('Erreur lors de la modification', 'Erreur');
    } finally {
      this.isLoading = false;
    }
  }

  // ==================== MÉTHODES LEGACY (à conserver pour transition) ====================

  private initMyUpdateForm() {
    this.myFormUpdate = this.formBuilder.group({
      affaire: [null, Validators.required],
      delai: ['', Validators.required],
      commentaire: ['']
    });
  }

  private affectDemandeToForm(demande: IDemandeAchat) {
    const affaire = this.affairesDisponibles.find(a => a.affaire === demande.affaireCode);
    const delaiFormatted = demande.delai
      ? new Date(demande.delai).toISOString().slice(0, 10)
      : null;
    this.myFormUpdate.patchValue({
      affaire: affaire || null,
      delai: delaiFormatted,
      commentaire: demande.commentaire
    });
  }

  private loadAffaires() {
    this.affaireService.getAffaires().subscribe({
      next: data => {
        this.affairesDisponibles = data;
        console.log("Affaires chargées:", this.affairesDisponibles.length);
        if (this.demandeToUpdate?.id) {
          this.affectDemandeToForm(this.demandeToUpdate);
          this.getAffaireById();
          this.loadDemandeFiles();
        }
      },
      error: err => {
        console.error("Erreur chargement affaires:", err);
        this.notifyService.showError('Erreur lors du chargement des affaires', 'Erreur');
      }
    });
  }

  getAffaireById(){
    this.affaireService.getAffaireByCode(this.demandeToUpdate.affaireCode).subscribe({
      next: data => {
        this.existenceAffaireForDemande = data;
        console.log("Affaire trouvée:", this.existenceAffaireForDemande);
      },
      error: err => {
        console.log(err);
      }
    });
  }

  closeView() {
    this.pieceUrl = null;
  }

  onCancel() {
    this.closebutton.nativeElement.click();
  }

  // LEGACY - à supprimer après migration complète
  getPiecesJoint() {
    // Méthode legacy - garder pour transition
  }

  private updateAllPieces() {
    // Méthode legacy - garder pour transition
  }

  onFileSelected(event: Event) {
    // Rediriger vers la nouvelle méthode
    this.onFileSelect(event);
  }

  getFileIconClass(type: string): string {
    // Rediriger vers la nouvelle méthode
    return 'fa ' + this.getFileIcon(type || '');
  }
}
