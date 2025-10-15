import {ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import { AffaireServiceService } from "../../../services/affaire-service.service";
import { ActivatedRoute, Router } from "@angular/router";
import { MatDialog } from "@angular/material/dialog";
import { ReceptionService } from "../../../services/reception.service";
import { LigneReceptionService } from "../../../services/ligne-reception.service";
import { UserProfileService } from "../../../services/user-profile.service";
import { IReception } from "../../../../interfaces/ireception";
import { ILigneReception } from "../../../../interfaces/ilignereception";
import { NotificationService } from "../../../services/notification.service";
import { Affaire } from "../../../../interfaces/iaffaire";
import { SortService } from "../../../services/sort.service";
import { UserData } from "../../../../interfaces/iuser";
import { TempDataService } from "../../../services/temp-data.service";
import {Subscription, take} from "rxjs";
import { PagedResponse, PaginationParams } from "../../../../interfaces/PagedResponse";
import { environment } from "../../../../environments/environment";
import { IPieces } from "../../../../interfaces/ipiecesjoints";
import { DomSanitizer, SafeResourceUrl } from "@angular/platform-browser";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { KeycloakService } from 'keycloak-angular';
import {DetailsFilesReceptionComponent} from "../detals-files-reception.component/detals-files-reception.component";
import {FileSyncService} from "../../../services/file-sync.service";
import {BonCommandeCacheService} from "../../../services/bon-commande-cache.service";

@Component({
  selector: 'app-list-receptions',
  templateUrl: './list-receptions.component.html',
  styleUrls: ['./list-receptions.component.css', '../../../../assets/css/styles.css']
})
export class ListReceptionsComponent implements OnInit, OnDestroy {
  @ViewChild('fileInput') fileInput!: ElementRef;
  @ViewChild('detailsFilesComponent') detailsFilesComponent!: DetailsFilesReceptionComponent;
  @ViewChild('updateForm') updateForm: any;
  receptionHasLignes: boolean = false;
  private fileSyncSubscription?: Subscription;
  showModal = false;
  showDetailsModal = false;
  listReceptions!: IReception[];
  selectedReception: IReception | null = null;
  affairesMap: Map<String, Affaire> = new Map();
  isCollapsed = false;
  isLoading = false;
  isFullscreen = false;
  receptionEnCoursDeSuppression: IReception | null = null;
  receptionEnCoursDEnvoi: IReception | null = null;
  isLoadingDetails = false;
  selectedReceptionForDetails: IReception | null = null;
  currentUser: UserData | null = null;
  isLoadingUser = true;

  lignesReception: ILigneReception[] = [];
  loadingLignes = false;
  errorLignes = false;
  currentStepUpdate: number = 1;
  lignesBonCommandeUpdate: any[] = [];
  isLoadingLignesBCUpdate = false;
  filteredBonCommandesUpdate: any[] = [];
  availableAffairesUpdate: any[] = [];
  availableFournisseursUpdate: any[] = [];
  selectedAffaireUpdate: any = null;
  selectedFournisseurUpdate: any = null;
  // ==================== NOUVELLES PROPRIÉTÉS HARMONISÉES ====================

  // Gestion des fichiers moderne (comme demandes d'achat)
  selectedFiles: File[] = [];
  isDragOver = false;
  isUploading = false;
  maxFiles = 3;
  showUpdateModal = false;
  receptionToUpdate: IReception | null = null;

  // Fichiers existants
  listPiecesJointes: any[] = [];
  loadingFiles = false;
  errorFiles = false;

  // Pagination et tri
  pfiltre: any;
  sort = {field: '', direction: 'asc' as 'asc' | 'desc'};
  page: number = 1;
  count: number = 0;
  tableSize: number = 10;

  pagedResponse: PagedResponse<IReception> | null = null;
  paginationParams: PaginationParams = {
    page: 0,
    size: 10,
    sortBy: 'numero',
    sortDirection: 'desc'
  };

  selectedBonCommande: any = null;
  affaireDisplay: string = '';
  bonCommandes: any[] = [];
  fournisseurs: any[] = [];
  allPieces: any[] = [];
  protected piecesJoints: IPieces[] = [];
  piecesToDelete: number[] = [];
  pieceUrl: SafeResourceUrl | null = null;
  AUTH = 'receptions';
  private filteredCount: number;

  constructor(
    private affaireService: AffaireServiceService,
    private receptionService: ReceptionService,
    private ligneReceptionService: LigneReceptionService,
    private sanitizer: DomSanitizer,
    private bonCommandeCacheService: BonCommandeCacheService,
    private fileSyncService: FileSyncService,
    private http: HttpClient,
    private userProfileService: UserProfileService,
    private elementRef: ElementRef,
    private cdr: ChangeDetectorRef,
    private tempDataService: TempDataService,
    private notifyService: NotificationService,
    private keycloakService: KeycloakService
  ) {
  }

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadAffaires();

    // 3. AJOUTER uniquement cet appel (si pas déjà présent)
    this.subscribeToFileSync();
  }

  private subscribeToFileSync(): void {
    this.fileSyncSubscription = this.fileSyncService.fileSync$.subscribe(event => {
      if (!event) return;

      console.log('🔄 ListReceptions - Événement reçu:', event);

      // Synchroniser le modal UPDATE
      if (this.receptionToUpdate?.id === event.receptionId) {
        console.log('✅ Sync modal UPDATE pour réception:', event.receptionId);
        this.loadReceptionFiles(event.receptionId).then(() => {
          console.log('📁 Fichiers rechargés pour modal UPDATE');
          this.cdr.detectChanges();
        });
      }

      // Synchroniser le modal DETAILS
      if (this.selectedReceptionForDetails?.id === event.receptionId) {
        console.log('✅ Sync modal DETAILS pour réception:', event.receptionId);
        this.refreshDetailsFiles();
      }
    });
  }

  public refreshDetailsFiles(): void {
    console.log('Rafraîchissement manuel demandé');

    if (this.selectedReceptionForDetails?.id) {
      // AJOUTER cette ligne :
      this.fileSyncService.notifyFileRefresh(this.selectedReceptionForDetails.id);

      // Garder le code existant aussi :
      this.loadReceptionFiles(this.selectedReceptionForDetails.id).then(() => {
        this.cdr.detectChanges();
      });
    }
  }
// Ajouter cette propriété après la déclaration de 'count'

  onSearchChange(): void {
    console.log('🔍 Recherche:', this.pfiltre);

    // Réinitialiser à la première page
    this.page = 1;
    this.paginationParams.page = 0;

    // Recharger les réceptions avec le filtre
    this.loadReceptions();
  }

// Modifier la méthode onTableDataChange
  onTableDataChange(event: any): void {
    this.page = event;
    this.paginationParams.page = event - 1;

    // Toujours recharger depuis le serveur (avec ou sans filtre)
    this.loadReceptions();
  }
  canAddMoreFiles(): boolean {
    const currentFilesCount = this.listPiecesJointes ? this.listPiecesJointes.length : 0;
    const selectedFilesCount = this.selectedFiles ? this.selectedFiles.length : 0;
    const totalFiles = currentFilesCount + selectedFilesCount;

    // Pour le modal de détails, vérifier aussi le statut
    if (this.selectedReceptionForDetails) {
      return totalFiles < this.maxFiles && !this.isReceptionEnvoyee(this.selectedReceptionForDetails);
    }

    // Pour le modal de modification - SEULEMENT vérifier le statut, PAS receptionHasLignes
    if (this.receptionToUpdate) {
      return totalFiles < this.maxFiles && this.canModifyFiles();
    }

    return totalFiles < this.maxFiles;
  }

  canModifyFiles(): boolean {
    return this.receptionToUpdate?.statut !== 'Envoyé';
  }

  getRemainingFilesCount(): number {
    const currentFilesCount = this.listPiecesJointes ? this.listPiecesJointes.length : 0;
    const selectedFilesCount = this.selectedFiles ? this.selectedFiles.length : 0;
    return Math.max(0, this.maxFiles - currentFilesCount - selectedFilesCount);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;

    if (!this.canAddMoreFiles()) {
      this.notifyService.showError('Maximum 3 fichiers autorisés', 'Limite atteinte');
      return;
    }

    const files = event.dataTransfer?.files;
    if (files) {
      this.handleFileSelection(Array.from(files));
    }
  }

  onFileSelected(event: any): void {
    this.onFileSelect(event);
  }

  onFileSelect(event: any): void {
    const files = Array.from(event.target.files as FileList);
    this.handleFileSelection(files);
    // Réinitialiser l'input
    event.target.value = '';
  }

  private handleFileSelection(files: File[]): void {
    console.log("Fichiers sélectionnés:", files);

    // Vérifier le nombre total (existants + nouveaux)
    const totalFiles = this.listPiecesJointes.length + this.selectedFiles.length + files.length;

    if (totalFiles > this.maxFiles) {
      const remaining = this.maxFiles - this.listPiecesJointes.length - this.selectedFiles.length;
      this.notifyService.showError(
        `Maximum ${this.maxFiles} fichiers autorisés. Vous pouvez encore ajouter ${remaining} fichier(s).`,
        'Limite atteinte'
      );
      return;
    }

    // Valider chaque fichier
    const validFiles: File[] = [];
    const errors: string[] = [];

    files.forEach(file => {
      const validation = this.validateFile(file);
      if (validation.valid) {
        // Éviter les doublons
        if (!this.selectedFiles.some(f => f.name === file.name && f.size === file.size)) {
          validFiles.push(file);
        } else {
          errors.push(`Fichier "${file.name}" déjà sélectionné`);
        }
      } else {
        errors.push(`${file.name}: ${validation.error}`);
      }
    });

    // Afficher les erreurs s'il y en a
    if (errors.length > 0) {
      this.notifyService.showError(errors.join('\n'), 'Fichiers rejetés');
    }

    // CORRECTION PRINCIPALE: Ajouter à la sélection et uploader automatiquement
    if (validFiles.length > 0) {
      // Ajouter à la sélection d'abord
      this.selectedFiles = [...this.selectedFiles, ...validFiles];

      this.notifyService.showSuccess(
        `${validFiles.length} fichier(s) sélectionné(s)`,
        'Fichiers ajoutés'
      );


    }
  }

  refreshManuallyFilesList(): void {
    let receptionId: number | null = null;

    if (this.receptionToUpdate?.id) {
      receptionId = this.receptionToUpdate.id;
    } else if (this.selectedReceptionForDetails?.id) {
      receptionId = this.selectedReceptionForDetails.id;
    }

    if (receptionId) {
      console.log("Refresh manuel demandé pour réception:", receptionId);
      this.forceReloadFilesList(receptionId);
    } else {
      console.error("Impossible de rafraîchir: ID réception manquant");
      this.notifyService.showError('ID de réception manquant', 'Erreur');
    }
  }

  private async forceReloadFilesList(receptionId: number): Promise<void> {
    console.log("=== DÉBUT FORCE RELOAD LIST ===");
    console.log("Rechargement pour réception:", receptionId);

    this.loadingFiles = true;

    try {
      // Attendre que le backend finisse de traiter
      await new Promise(resolve => setTimeout(resolve, 1500));

      // Première tentative de chargement
      console.log("Première tentative de chargement...");
      let files = await this.receptionService.getFilesByReception(receptionId).toPromise();

      console.log("Fichiers reçus (1ère tentative):", files?.length || 0);

      // Si pas de fichiers, essayer une deuxième fois
      if (!files || files.length === 0) {
        console.log("Aucun fichier trouvé, deuxième tentative...");
        await new Promise(resolve => setTimeout(resolve, 1000));
        files = await this.receptionService.getFilesByReception(receptionId).toPromise();
        console.log("Fichiers reçus (2ème tentative):", files?.length || 0);
      }

      // Mettre à jour la liste avec un nouveau tableau
      const oldCount = this.listPiecesJointes.length;
      this.listPiecesJointes = [...(files || [])];
      const newCount = this.listPiecesJointes.length;

      console.log(`Liste mise à jour: ${oldCount} → ${newCount} fichiers`);

      // Forcer la détection des changements plusieurs fois
      this.cdr.markForCheck();
      this.cdr.detectChanges();

      // Forcer encore après un délai
      setTimeout(() => {
        this.cdr.detectChanges();
        console.log("Détection forcée terminée");
      }, 500);

    } catch (error) {
      console.error("Erreur lors du rechargement forcé:", error);
      this.errorFiles = true;
    } finally {
      this.loadingFiles = false;
      this.cdr.detectChanges();
    }

    console.log("=== FIN FORCE RELOAD LIST ===");
  }

  private validateFile(file: File): { valid: boolean, error?: string } {
    // Taille maximale (50MB)
    const maxSize = 50 * 1024 * 1024;
    if (file.size > maxSize) {
      return {valid: false, error: 'Fichier trop volumineux (max 50MB)'};
    }

    // Extensions autorisées
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

  removeSelectedFile(index: number): void {
    this.selectedFiles.splice(index, 1);
    this.notifyService.showSuccess('Fichier retiré de la sélection', 'Supprimé');
  }

  protected loadReceptionFiles(receptionId: number): Promise<any[]> {
    console.log("loadReceptionFiles appelée pour:", receptionId);

    return this.receptionService.getFilesByReception(receptionId)
      .toPromise()
      .then((files) => {
        console.log("Fichiers récupérés:", files);

        // CORRECTION : Assigner avec spread operator et forcer détection
        this.listPiecesJointes = [...(files || [])];
        this.errorFiles = false;

        // CORRECTION : Forcer la détection des changements
        this.cdr.detectChanges();

        return files || [];
      })
      .catch((error) => {
        console.error("Erreur lors du chargement des fichiers:", error);
        this.listPiecesJointes = [];
        this.errorFiles = true;

        // CORRECTION : Forcer la détection même en cas d'erreur
        this.cdr.detectChanges();

        return [];
      });
  }

  deleteFile(fileId: number): void {
    if (!fileId) {
      console.error('ID de fichier manquant pour suppression');
      return;
    }

    let receptionId: number | null = null;

    if (this.receptionToUpdate?.id) {
      receptionId = this.receptionToUpdate.id;
    } else if (this.selectedReceptionForDetails?.id) {
      receptionId = this.selectedReceptionForDetails.id;
    }

    if (!receptionId || !confirm('Êtes-vous sûr de vouloir supprimer ce fichier définitivement ?')) {
      return;
    }

    this.loadingFiles = true;

    this.receptionService.deleteFileFromReception(receptionId, fileId)
      .subscribe({
        next: async (response) => {
          console.log('Fichier supprimé avec succès');
          this.notifyService.showSuccess('Fichier supprimé avec succès', 'Supprimé');

          // REMPLACER le bloc setTimeout par ceci :
          setTimeout(async () => {
            const updatedFiles = await this.receptionService.getFilesByReception(receptionId!).toPromise();
            this.listPiecesJointes = [...(updatedFiles || [])];

            // ÉMETTRE l'événement de synchronisation
            this.fileSyncService.notifyFileDelete(receptionId!);

            this.loadingFiles = false;
            this.cdr.detectChanges();
          }, 1000);
        },
        error: (err) => {
          console.error('Erreur suppression:', err);
          this.loadingFiles = false;
          this.notifyService.showError('Erreur lors de la suppression', 'Erreur');
        }
      });
  }

  private async forceReloadFiles(receptionId: number): Promise<void> {
    console.log("=== DEBUT FORCE RELOAD ===");

    try {
      // Attendre un peu que le backend finisse
      await new Promise(resolve => setTimeout(resolve, 1000));

      // Vider d'abord la liste pour forcer le rechargement
      this.listPiecesJointes = [];
      this.cdr.detectChanges();

      console.log("Liste vidée, rechargement...");

      // Recharger depuis le backend
      const files = await this.receptionService.getFilesByReception(receptionId).toPromise();

      console.log("Fichiers reçus du backend:", files);

      // Mettre à jour avec un nouveau tableau
      this.listPiecesJointes = [...(files || [])];

      console.log("Nouvelle liste mise à jour:", this.listPiecesJointes.length, "fichiers");

      // Forcer la détection multiple fois
      this.cdr.markForCheck();
      this.cdr.detectChanges();

      // Forcer encore après un délai
      setTimeout(() => {
        this.cdr.detectChanges();
      }, 100);

    } catch (error) {
      console.error("Erreur rechargement forcé:", error);
      this.errorFiles = true;
    }

    console.log("=== FIN FORCE RELOAD ===");
  }

  async uploadSelectedFiles(): Promise<void> {
    if (this.selectedFiles.length === 0) {
      console.log("Aucun fichier en attente d'upload");
      return;
    }

    let receptionId: number | null = null;

    if (this.receptionToUpdate?.id) {
      receptionId = this.receptionToUpdate.id;
    } else if (this.selectedReceptionForDetails?.id) {
      receptionId = this.selectedReceptionForDetails.id;
    }

    if (!receptionId) {
      this.notifyService.showError('ID de réception manquant', 'Erreur');
      return;
    }

    this.isUploading = true;
    this.loadingFiles = true;
    const filesToUpload = [...this.selectedFiles];

    try {
      const response = await this.receptionService.uploadFilesArrayForReception(
        receptionId,
        filesToUpload
      ).toPromise();

      this.notifyService.showSuccess(
        `${filesToUpload.length} fichier(s) uploadé(s) avec succès`,
        'Upload réussi'
      );

      // Vider la sélection IMMÉDIATEMENT
      this.selectedFiles = [];

      // REMPLACER le bloc setTimeout par ceci :
      setTimeout(async () => {
        const updatedFiles = await this.receptionService.getFilesByReception(receptionId!).toPromise();
        this.listPiecesJointes = [...(updatedFiles || [])];

        // ÉMETTRE l'événement de synchronisation
        this.fileSyncService.notifyFileUpload(receptionId!);

        this.cdr.detectChanges();
      }, 1000);

    } catch (error) {
      console.error('Erreur upload:', error);
      this.notifyService.showError('Erreur lors de l\'upload des fichiers', 'Erreur');
    } finally {
      this.isUploading = false;
      this.loadingFiles = false;
      this.cdr.detectChanges();
    }
  }

  openDetailsModal(reception: IReception): void {
    this.selectedReceptionForDetails = reception;
    this.isLoadingDetails = true;

    // Réinitialiser les listes
    this.lignesReception = [];
    this.listPiecesJointes = [];
    this.selectedFiles = []; // AJOUT : Réinitialiser aussi les fichiers sélectionnés
    this.errorFiles = false; // AJOUT : Réinitialiser l'état d'erreur

    // CORRECTION : Forcer la détection après réinitialisation
    this.cdr.detectChanges();

    // Charger les lignes ET les fichiers en parallèle
    const lignesPromise = reception.id ?
      this.ligneReceptionService.getLignesReceptionByReceptionId(reception.id).toPromise() :
      Promise.resolve([]);

    const filesPromise = reception.id ?
      this.loadReceptionFiles(reception.id) :
      Promise.resolve([]);

    Promise.all([lignesPromise, filesPromise])
      .then(([lignes, files]) => {
        this.lignesReception = lignes || [];
        this.listPiecesJointes = [...(files || [])]; // CORRECTION : Utiliser spread operator

        console.log("Lignes chargées:", this.lignesReception.length);
        console.log("Fichiers chargés:", this.listPiecesJointes.length);

        this.isLoadingDetails = false;
        this.showDetailsModal = true;

        // CORRECTION : Forcer la détection après chargement
        this.cdr.detectChanges();

        // Ouvrir le modal après un court délai
        setTimeout(() => {
          const modalBtn = document.querySelector('[data-target="#detailsModal"]') as HTMLElement;
          if (modalBtn) {
            modalBtn.click();
          } else {
            // Alternative: ouvrir directement avec jQuery si disponible
            (window as any).$('#detailsModal').modal('show');
          }
        }, 100);
      })
      .catch((err) => {
        console.error("Erreur chargement détails:", err);
        this.notifyService.showError('Erreur lors du chargement des détails', 'Erreur');
        this.isLoadingDetails = false;
        this.errorFiles = true;

        // CORRECTION : Forcer la détection même en cas d'erreur
        this.cdr.detectChanges();
      });
  }

  isReceptionEnvoyee(reception: IReception): boolean {
    return reception.statut === 'Envoyé';
  }

  trackByFileId(index: number, file: any): any {
    return file.fileId || file.id || index;
  }

  getLastUpdateTime(): string {
    return new Date().toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  }

  resetFilesState(): void {
    this.selectedFiles = [];
    this.listPiecesJointes = [];
    this.loadingFiles = false;
    this.errorFiles = false;
    this.isUploading = false;

    // Forcer la détection des changements
    this.cdr.detectChanges();
  }

  updateReceptionInfo() {
    if (!this.receptionToUpdate?.referenceBl || this.receptionToUpdate.referenceBl.trim() === '') {
      this.notifyService.showError('Le numéro BL est obligatoire', 'Champ manquant');
      return;
    }

    if (!this.receptionToUpdate || !this.selectedBonCommande) {
      this.notifyService.showError('Veuillez sélectionner un bon de commande', 'Formulaire incomplet');
      return;
    }

    const receptionAvant = JSON.parse(JSON.stringify(this.receptionToUpdate));
    console.log("Bon selected est : ", this.selectedBonCommande);
    console.log('--- Réception avant modification ---');
    console.log(receptionAvant);

    const bcSelected = this.bonCommandes.find(bc => bc.commande === this.selectedBonCommande);
    const dateReceptionStr = this.receptionToUpdate.dateReception
      ? this.receptionToUpdate.dateReception + 'T00:00:00'
      : null;

    const dateBlStr = this.receptionToUpdate.dateBl
      ? this.receptionToUpdate.dateBl + 'T00:00:00'
      : null;

    const receptionApres = {
      ...this.receptionToUpdate,
      referenceBl: this.receptionToUpdate.referenceBl,
      dateBl: dateBlStr,
      refFournisseur: this.receptionToUpdate.refFournisseur,
      nomFournisseur: this.receptionToUpdate.nomFournisseur,
      affaireCode: bcSelected?.affaireCode || this.receptionToUpdate.affaireCode,
      affaireLibelle: bcSelected?.affaireName,
      affaireId: this.selectedBonCommande,
      dateReception: dateReceptionStr,
      idAgelio: this.receptionToUpdate.idAgelio,
      statut: this.receptionToUpdate.statut || 'En cours'
    };

    console.log('--- Réception après modification ---');
    console.log(receptionApres);

    const payload = {
      id: this.receptionToUpdate.id,
      ...receptionApres
    };

    console.log('--- Payload à envoyer au backend ---');
    console.log(payload);

    this.receptionService.updateReception(payload.id!, payload).subscribe({
      next: (response) => {
        console.log('Réponse serveur:', response);
        this.notifyService.showSuccess(
          `Réception #${payload.id} modifiée avec succès`,
          'Modification réussie'
        );

        // CORRECTION : Gérer l'upload des nouveaux fichiers avec rafraîchissement automatique
        if (this.selectedFiles.length > 0) {
          console.log('Upload des nouveaux fichiers en cours...');

          this.uploadSelectedFiles().then(() => {
            console.log('Upload terminé, rafraîchissement en cours...');

            // NOUVEAU : Rafraîchir le modal de détails si ouvert
            this.refreshDetailsModalAfterUpdate();

            // Recharger la liste principale
            this.loadReceptions();

            // Fermer le modal de modification
            this.fermerModalUpdate();

          }).catch((error) => {
            console.error("Erreur upload après modification:", error);

            // NOUVEAU : Même en cas d'erreur d'upload, rafraîchir le modal de détails
            this.refreshDetailsModalAfterUpdate();

            // Continuer avec le processus normal
            this.loadReceptions();
            this.fermerModalUpdate();
          });
        } else {
          console.log('Pas de nouveaux fichiers à uploader');

          // NOUVEAU : Rafraîchir le modal de détails même sans upload
          this.refreshDetailsModalAfterUpdate();

          // Processus normal
          this.loadReceptions();
          this.fermerModalUpdate();
        }
      },
      error: (error) => {
        console.error('Erreur modification réception:', error);

        let errorMessage = 'Erreur lors de la modification';
        if (error.error && error.error.message) {
          errorMessage = error.error.message;
        } else if (error.message) {
          errorMessage = error.message;
        }

        this.notifyService.showError(errorMessage, 'Erreur');
      }
    });
  }

  private refreshDetailsModalAfterUpdate(): void {
    console.log('=== DÉBUT RAFRAÎCHISSEMENT MODAL DÉTAILS ===');

    // Vérifier si le modal de détails est ouvert pour la même réception
    if (this.selectedReceptionForDetails?.id === this.receptionToUpdate?.id) {
      console.log('Modal de détails ouvert pour la même réception, rafraîchissement en cours...');

      const receptionId = this.selectedReceptionForDetails.id;

      // MÉTHODE 1 : Utiliser ViewChild avec la méthode loadFiles existante
      if (this.detailsFilesComponent && typeof this.detailsFilesComponent.loadFiles === 'function') {
        console.log('Rafraîchissement via ViewChild - méthode loadFiles');

        setTimeout(() => {
          this.detailsFilesComponent.loadFiles();
        }, 1000);
      }
      // MÉTHODE 2 : Rafraîchissement manuel via les services
      else {
        console.log('Rafraîchissement manuel via services');

        // Recharger les lignes de réception
        this.ligneReceptionService.getLignesReceptionByReceptionId(receptionId).subscribe({
          next: (lignes) => {
            this.lignesReception = lignes || [];
            console.log('Lignes rechargées:', this.lignesReception.length);
            this.cdr.detectChanges();
          },
          error: (error) => {
            console.error('Erreur rechargement lignes:', error);
          }
        });

        // Recharger les fichiers avec délai pour s'assurer que le backend a fini
        setTimeout(() => {
          this.loadReceptionFiles(receptionId).then((files) => {
            console.log('Fichiers rechargés pour le modal de détails:', files?.length || 0);
            this.cdr.detectChanges();

            // Forcer un deuxième rafraîchissement après un délai supplémentaire
            setTimeout(() => {
              this.cdr.detectChanges();
            }, 500);

          }).catch((error) => {
            console.error('Erreur rechargement fichiers:', error);
          });
        }, 1500);
      }

      // MÉTHODE 3 : Émettre un événement personnalisé pour le composant enfant
      setTimeout(() => {
        const event = new CustomEvent('filesUpdated', {
          detail: {receptionId: receptionId}
        });
        window.dispatchEvent(event);
        console.log('Événement filesUpdated émis');
      }, 2000);

    } else {
      console.log('Modal de détails fermé ou pour une autre réception, pas de rafraîchissement nécessaire');
    }

    console.log('=== FIN RAFRAÎCHISSEMENT MODAL DÉTAILS ===');
  }

  ngOnDestroy(): void {
    // AJOUTEZ ce code existant
    if (this.fileSyncSubscription) {
      this.fileSyncSubscription.unsubscribe();
    }

    // AJOUTEZ ce nettoyage pour le fullscreen
    const backdrop = document.querySelector('.fullscreen-backdrop');
    if (backdrop) {
      backdrop.remove();
    }
    document.body.classList.remove('fullscreen-active', 'modal-open');
    document.body.style.overflow = '';
    document.body.style.paddingRight = '';

    // Nettoyer les backdrops orphelins
    document.querySelectorAll('.modal-backdrop').forEach(el => el.remove());
    document.querySelectorAll('.ng-dropdown-panel').forEach(el => el.remove());
  }b

  isRecentlyUploaded(file: any): boolean {
    if (!file.dateCreation && !file.sysCreationDate) {
      return false;
    }

    const fileDate = new Date(file.dateCreation || file.sysCreationDate);
    const now = new Date();
    const diffMinutes = (now.getTime() - fileDate.getTime()) / (1000 * 60);

    // Considérer comme récent si créé dans les 5 dernières minutes
    return diffMinutes <= 5;
  }

  formatDate(date: any): string {
    if (!date) return '-';

    try {
      const d = new Date(date);
      if (isNaN(d.getTime())) return '-';

      const day = d.getDate().toString().padStart(2, '0');
      const month = (d.getMonth() + 1).toString().padStart(2, '0');
      const year = d.getFullYear();

      return `${day}/${month}/${year}`;
    } catch (e) {
      return '-';
    }
  }

  async downloadFile(fileOrId: any | number, fileName?: string): Promise<void> {
    try {
      console.log("=== DEBUG DOWNLOAD FILE ===");

      let fileId: number;
      let downloadFileName: string;

      // Si c'est un objet file
      if (typeof fileOrId === 'object' && fileOrId !== null) {
        console.log("Objet file complet:", JSON.stringify(fileOrId, null, 2));
        fileId = fileOrId.fileId || fileOrId.id;
        downloadFileName = fileOrId.fullFileName || fileOrId.name || 'fichier';
      }
      // Si c'est un ID avec nom de fichier
      else if (typeof fileOrId === 'number' && fileName) {
        fileId = fileOrId;
        downloadFileName = fileName;
      } else {
        console.error("ERREUR: Paramètres de téléchargement invalides");
        this.notifyService.showError('Paramètres de fichier invalides', 'Erreur');
        return;
      }

      console.log("fileId calculé:", fileId);
      console.log("fileName calculé:", downloadFileName);

      if (!fileId) {
        console.error("ERREUR: fileId est undefined, null ou falsy");
        this.notifyService.showError('ID de fichier manquant', 'Erreur');
        return;
      }

      const downloadUrl = `${environment.apiUrl}receptions/files/${fileId}/download`;
      console.log("URL de téléchargement:", downloadUrl);

      // Récupérer le token depuis Keycloak
      const token = await this.keycloakService.getToken();

      if (!token) {
        console.error('Token Keycloak manquant');
        this.notifyService.showError('Session expirée, veuillez vous reconnecter', 'Erreur');
        return;
      }

      const headers = new HttpHeaders({
        'Authorization': `Bearer ${token}`
      });

      this.http.get(downloadUrl, {
        headers: headers,
        responseType: 'blob',
        observe: 'response'
      }).subscribe({
        next: (response: any) => {
          const blob = new Blob([response.body], {
            type: response.headers.get('content-type') || 'application/octet-stream'
          });

          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;

          // Extraire le nom du fichier
          let filename = downloadFileName;
          const contentDisposition = response.headers.get('content-disposition');
          if (contentDisposition) {
            const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
            const matches = filenameRegex.exec(contentDisposition);
            if (matches != null && matches[1]) {
              filename = matches[1].replace(/['"]/g, '');
            }
          }

          link.download = filename;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          window.URL.revokeObjectURL(url);

          console.log('Fichier téléchargé avec succès:', filename);
        },
        error: (error) => {
          console.error('Erreur lors du téléchargement:', error);
          if (error.status === 401) {
            this.notifyService.showError('Session expirée, veuillez vous reconnecter', 'Erreur');
          } else if (error.status === 404) {
            this.notifyService.showError('Fichier non trouvé', 'Erreur');
          } else {
            this.notifyService.showError('Erreur lors du téléchargement du fichier', 'Erreur');
          }
        }
      });
    } catch (error) {
      console.error('Erreur Keycloak:', error);
      this.notifyService.showError('Erreur d\'authentification', 'Erreur');
    }
  }

  getFileIcon(filename: string): string {
    const extension = filename.split('.').pop()?.toLowerCase();
    switch (extension) {
      case 'pdf':
        return 'fa fa-file-pdf text-danger';
      case 'doc':
      case 'docx':
        return 'fa fa-file-word text-primary';
      case 'xls':
      case 'xlsx':
        return 'fa fa-file-excel text-success';
      case 'jpg':
      case 'jpeg':
      case 'png':
        return 'fa fa-file-image text-info';
      default:
        return 'fa fa-file text-muted';
    }
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  hasRole(roleName: string): boolean {
    return this.currentUser?.roles?.includes(roleName) || false;
  }

  hasAccess(accessCode: string): boolean {
    return this.currentUser?.acces?.includes(accessCode) || false;
  }

  canViewAllReceptions(): boolean {
    return this.hasRole('ADMIN') || this.hasRole('MANAGER') || this.hasRole('CONSULTEUR');
  }

  canCreateReceptions(): boolean {
    return this.hasRole('ADMIN') || this.hasRole('MANAGER') || this.hasAccess('reception');
  }

  canModifyReceptions(): boolean {
    return this.hasRole('ADMIN') || this.hasRole('MANAGER') || this.hasAccess('reception');
  }

  canDeleteReceptions(): boolean {
    return this.hasRole('ADMIN') || this.hasRole('MANAGER') || this.hasAccess('reception');
  }

  canSendReceptions(): boolean {
    return this.hasRole('ADMIN') || this.hasRole('MANAGER') || this.hasAccess('reception');
  }

  loadCurrentUser() {
    this.isLoadingUser = true;
    this.userProfileService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUser = {
          id: user.id,
          login: user.login,
          roles: user.roles ? Array.from(user.roles).map((role: any) =>
            typeof role === 'string' ? role : role.name) : [],
          acces: user.acces ? user.acces.map((access: any) => access.code) : []
        };
        this.isLoadingUser = false;

        this.loadReceptions();
        this.loadBonCommandesForUpdate();
        this.loadFournisseurs();
      },
      error: (error) => {
        console.error('Erreur chargement utilisateur:', error);
        this.isLoadingUser = false;
        this.loadReceptions();
      }
    });
  }
  loadReceptions(): void {
    const searchTerm = this.pfiltre && this.pfiltre.trim() !== '' ? this.pfiltre.trim() : undefined;

    console.log('📡 Chargement réceptions - Page:', this.paginationParams.page, 'Recherche:', searchTerm);

    this.isLoading = true;

    this.receptionService.getAllReceptionsPaginated(this.paginationParams, searchTerm).subscribe({
      next: (response) => {
        this.pagedResponse = response;
        this.listReceptions = response.content;
        this.count = response.totalElements;  // ✅ IMPORTANT: nombre total FILTRÉ
        this.page = response.pageNumber + 1;
        this.isLoading = false;

        console.log('✅ Réceptions chargées:', this.listReceptions.length, 'Total:', this.count);
      },
      error: (err) => {
        this.handleLoadError(err);
      }
    });
  }

  private handleLoadError(err: any): void {
    this.isLoading = false;
    console.error("❌ Erreur lors du chargement des réceptions:", err);
    this.notifyService.showError('Erreur lors du chargement des réceptions', 'Erreur');
  }

  loadAffaires() {
    this.affaireService.getAffaires().subscribe({
      next: (affaires) => {
        affaires.forEach(affaire => {
          this.affairesMap.set(affaire.code!, affaire);
        });
        this.loadReceptions();
      },
      error: (err) => {
        console.error("Erreur chargement affaires", err);
        this.loadReceptions();
      }
    });
  }

  getAffaireDisplay(affaireId: string | undefined): string {
    if (affaireId && this.affairesMap.has(affaireId)) {
      const affaire = this.affairesMap.get(affaireId)!;
      return `${affaire.affaire} - ${affaire.libelle}`;
    }
    return 'Non spécifiée';
  }

  ouvrirModalAvecDelay() {
    if (!this.canCreateReceptions()) {
      this.notifyService.showError('Accès non autorisé', 'Permissions insuffisantes');
      return;
    }
    this.showModal = true;
    this.cdr.detectChanges();
    setTimeout(() => {
      const modalBtn = document.querySelector('[data-target="#addModal"]') as HTMLElement;
      if (modalBtn) modalBtn.click();
    }, 100);
  }

  ouvrirModalDetails(reception: IReception) {
    this.selectedReception = reception;
    this.showDetailsModal = true;
    this.loadLignesReception(reception.id!);

    this.cdr.detectChanges();
    setTimeout(() => {
      const modalBtn = document.querySelector('[data-target="#detailsModal"]') as HTMLElement;
      if (modalBtn) modalBtn.click();
    }, 100);
  }

  loadLignesReception(receptionId: number) {
    this.loadingLignes = true;
    this.errorLignes = false;
    this.ligneReceptionService.getLignesReceptionByReceptionId(receptionId).subscribe({
      next: (lignes) => {
        this.lignesReception = lignes;
        console.log("Les lignes de receptions pour cette reception : ", this.lignesReception);
        this.loadingLignes = false;
      },
      error: (error) => {
        console.error("Erreur chargement lignes:", error);
        this.errorLignes = true;
        this.loadingLignes = false;
        this.lignesReception = [];
      }
    });
  }

  changerStatutReception(reception: IReception, nouveauStatut: string) {
    if (nouveauStatut === 'Envoyé' && !this.canSendReceptions()) {
      this.notifyService.showError('Envoi non autorisé', 'Permissions insuffisantes');
      return;
    }

    if (nouveauStatut === 'Envoyé') {
      this.ligneReceptionService.getLignesReceptionByReceptionId(reception.id!).subscribe({
        next: (lignes) => {
          if (!lignes || lignes.length === 0) {
            // ✅ Message harmonisé avec consommations
            this.notifyService.showError(
              'Impossible d\'envoyer une réception vide. Veuillez ajouter au moins un article avant l\'envoi.',
              'Réception vide'
            );
            return;
          }
          this.receptionEnCoursDEnvoi = {...reception, statut: nouveauStatut};
          this.ouvrirModalConfirmationEnvoi();
        },
        error: (error) => {
          // ✅ Message d'erreur harmonisé
          console.error('Erreur lors de la vérification des lignes:', error);
          this.notifyService.showError(
            'Erreur lors de la vérification du contenu de la réception',
            'Erreur'
          );
        }
      });
    }
  }

  deleteReception(reception: IReception) {
    if (!this.canDeleteReceptions()) {
      this.notifyService.showError('Suppression non autorisée', 'Permissions insuffisantes');
      return;
    }

    if (reception.statut === 'Envoyé') {
      this.notifyService.showError('Impossible de supprimer une réception envoyée', 'Suppression interdite');
      return;
    }

    this.receptionEnCoursDeSuppression = reception;
    this.ouvrirModalConfirmationSuppression();
  }

  sortColumn(column: string): void {
    if (this.paginationParams.sortBy === column) {
      this.paginationParams.sortDirection =
        this.paginationParams.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.paginationParams.sortBy = column;
      this.paginationParams.sortDirection = 'asc';
    }
    this.paginationParams.page = 0;
    this.sort.field = column;
    this.sort.direction = this.paginationParams.sortDirection;
    this.loadReceptions();
  }

  toggleCollapse() {
    this.isCollapsed = !this.isCollapsed;
  }

  toggleFullscreen() {
    this.isFullscreen = !this.isFullscreen;

    if (this.isFullscreen) {
      // Créer ou récupérer le backdrop fullscreen
      let backdrop = document.querySelector('.fullscreen-backdrop') as HTMLElement;
      if (!backdrop) {
        backdrop = document.createElement('div');
        backdrop.className = 'fullscreen-backdrop';
        backdrop.addEventListener('click', () => this.toggleFullscreen());
        document.body.appendChild(backdrop);
      }

      // Activer le fullscreen
      backdrop.classList.add('show');
      document.body.classList.add('fullscreen-active');

      // Appliquer le z-index à tous les modals
      const modals = document.querySelectorAll('.modal');
      modals.forEach(modal => {
        (modal as HTMLElement).style.zIndex = '10050';
      });
    } else {
      // Désactiver le fullscreen
      const backdrop = document.querySelector('.fullscreen-backdrop') as HTMLElement;
      if (backdrop) {
        backdrop.classList.remove('show');
      }
      document.body.classList.remove('fullscreen-active');

      // Remettre le z-index par défaut
      const modals = document.querySelectorAll('.modal');
      modals.forEach(modal => {
        (modal as HTMLElement).style.zIndex = '';
      });
    }

    this.cdr.detectChanges();
  }



  ouvrirModalConfirmationEnvoi() {
    this.cdr.detectChanges();
    setTimeout(() => {
      const modal = document.getElementById('confirmSendModal');
      if (modal) {
        if ((window as any).$) {
          ((window as any).$ as any)(modal).modal('show');
        } else if ((window as any).bootstrap) {
          const bsModal = new (window as any).bootstrap.Modal(modal);
          bsModal.show();
        }
      }
    }, 100);
  }

  ouvrirModalConfirmationSuppression() {
    this.cdr.detectChanges();
    setTimeout(() => {
      const modal = document.getElementById('confirmDeleteModal');
      if (modal) {
        if ((window as any).$) {
          ((window as any).$ as any)(modal).modal('show');
        } else if ((window as any).bootstrap) {
          const bsModal = new (window as any).bootstrap.Modal(modal);
          bsModal.show();
        }
      }
    }, 100);
  }

  envoyerDemandeConfirmed() {
    if (this.receptionEnCoursDEnvoi) {
      this.receptionService.updateReception(this.receptionEnCoursDEnvoi.id!, this.receptionEnCoursDEnvoi).subscribe({
        next: () => {
          this.notifyService.showSuccess(`Réception #${this.receptionEnCoursDEnvoi!.id} envoyée`, 'Envoi confirmé');
          this.loadReceptions();
          this.fermerModalConfirmationEnvoi();
          this.fermerModalDetails();
        },
        error: (err) => {
          console.error('Erreur envoi:', err);
          this.notifyService.showError('Erreur lors de l\'envoi', 'Erreur');
          this.fermerModalConfirmationEnvoi();
        }
      });
    }
  }

  suppressionDemandeConfirmed() {
    if (this.receptionEnCoursDeSuppression) {
      this.receptionService.deleteReception(this.receptionEnCoursDeSuppression.id).subscribe({
        next: () => {
          this.notifyService.showSuccess(`Réception #${this.receptionEnCoursDeSuppression!.id} supprimée`, 'Suppression confirmée');
          this.loadReceptions();
          this.fermerModalConfirmationSuppression();
        },
        error: (err) => {
          console.error('Erreur suppression:', err);
          console.error('Erreur suppression de reception de id :', this.receptionEnCoursDeSuppression!.id);
          this.notifyService.showError('Erreur lors de la suppression', 'Erreur');
          this.fermerModalConfirmationSuppression();
        }
      });
    }
  }

  annulerEnvoi() {
    this.fermerModalConfirmationEnvoi();
  }

  annulerSuppression() {
    this.fermerModalConfirmationSuppression();
  }

  fermerModalConfirmationEnvoi() {
    const modal = document.getElementById('confirmSendModal');
    if (modal) {
      if ((window as any).$) {
        ((window as any).$ as any)(modal).modal('hide');
      } else if ((window as any).bootstrap) {
        const bsModal = (window as any).bootstrap.Modal.getInstance(modal) ||
          new (window as any).bootstrap.Modal(modal);
        bsModal.hide();
      } else {
        modal.classList.remove('show');
        modal.style.display = 'none';
        document.body.classList.remove('modal-open');
        const backdrop = document.querySelector('.modal-backdrop');
        if (backdrop) backdrop.remove();
      }
    }
    this.receptionEnCoursDEnvoi = null;
  }

  fermerModalConfirmationSuppression() {
    const modal = document.getElementById('confirmDeleteModal');
    if (modal) {
      if ((window as any).$) {
        ((window as any).$ as any)(modal).modal('hide');
      } else if ((window as any).bootstrap) {
        const bsModal = (window as any).bootstrap.Modal.getInstance(modal) ||
          new (window as any).bootstrap.Modal(modal);
        bsModal.hide();
      } else {
        modal.classList.remove('show');
        modal.style.display = 'none';
        document.body.classList.remove('modal-open');
        const backdrop = document.querySelector('.modal-backdrop');
        if (backdrop) backdrop.remove();
      }
    }
    this.receptionEnCoursDeSuppression = null;
  }

  fermerModalDetails() {
    const modal = document.getElementById('detailsModal');
    if (modal && (window as any).bootstrap) {
      const bsModal = (window as any).bootstrap.Modal.getInstance(modal);
      if (bsModal) bsModal.hide();
    }
  }

  private  loadBonCommandesForUpdate() {
    if (this.canViewAllReceptions()) {
      this.tempDataService.getAllBonCommandes().subscribe({
        next: data => {
          this.bonCommandes = data || [];
          console.log("✅ Bons de commandes chargés:", this.bonCommandes.length);

          // ✅ NOUVEAU : Charger et filtrer les affaires réelles
          this.loadAndFilterAffairesFromBonCommandes();
        },
        error: err => {
          console.error("Erreur chargement bons de commandes", err);
          this.bonCommandes = [];
        }
      });
    } else {
      this.tempDataService.getAllBonCommandes().subscribe({
        next: data => {
          const userAffaires = this.currentUser?.affaires?.map((aff: any) => aff.code) || [];
          this.bonCommandes = (data || []).filter(bc =>
            userAffaires.includes(bc.affaire)
          );

          // ✅ NOUVEAU : Charger et filtrer les affaires réelles
          this.loadAndFilterAffairesFromBonCommandes();
        },
        error: err => {
          console.error("Erreur chargement bons de commandes", err);
          this.bonCommandes = [];
        }
      });
    }
  }
  /**
   * Charge les affaires RÉELLES depuis le backend et filtre les BC
   */
  private loadAndFilterAffairesFromBonCommandes(): void {
    console.log('=== CHARGEMENT AFFAIRES RÉELLES ===');

    this.affaireService.getAffaires().subscribe({
      next: (affaires) => {
        console.log(`✅ ${affaires.length} affaires chargées depuis la base`);

        // Créer un Set des codes d'affaires réelles
        const affairesReellesSet = new Set(
          affaires.map(aff => aff.code || aff.affaire)
        );

        console.log('Affaires réelles:', Array.from(affairesReellesSet));

        // Filtrer les bons de commandes : garder SEULEMENT ceux avec des affaires réelles
        const bcAvant = this.bonCommandes.length;
        this.bonCommandes = this.bonCommandes.filter(bc => {
          const affaireExiste = affairesReellesSet.has(bc.affaireCode);

          if (!affaireExiste) {
            console.warn(`⚠️ BC ${bc.commande} ignoré : affaire "${bc.affaireCode}" inexistante`);
          }

          return affaireExiste;
        });

        console.log(`📊 Filtrage BC: ${bcAvant} → ${this.bonCommandes.length}`);

        // Extraire les affaires disponibles APRÈS filtrage
        this.extractAvailableAffairesUpdate();
      },
      error: (err) => {
        console.error('❌ Erreur chargement affaires:', err);
        this.notifyService.showError(
          'Erreur lors du chargement des affaires',
          'Erreur'
        );

        // En cas d'erreur, continuer avec tous les BC
        this.extractAvailableAffairesUpdate();
      }
    });
  }
  private loadFournisseurs() {
    this.tempDataService.getAllFournisseurs().subscribe({
      next: data => {
        this.fournisseurs = data || [];
      },
      error: err => {
        console.error("Erreur chargement fournisseurs", err);
        this.fournisseurs = [];
      }
    });
  }

  private updateAllPieces() {
    this.allPieces = [];
    this.piecesJoints
      .filter(piece => !this.piecesToDelete.includes(Number(piece.id)))
      .forEach(piece => {
        this.allPieces.push({
          id: piece.id,
          displayName: piece.nom,
          type: piece.type,
          url: piece.url,
          isNew: false
        });
      });
  }

  // ==================== MÉTHODES POUR LA GESTION DES STEPS DU MODAL UPDATE ====================

  nextStepUpdate(): void {
    if (this.canGoToNextStepUpdate() && this.currentStepUpdate < 4) {
      // Si la réception a des lignes et qu'on est aux steps 1 ou 2, passer directement au step 3
      if (this.receptionHasLignes && this.currentStepUpdate < 3) {
        this.currentStepUpdate = 3;
      } else {
        this.currentStepUpdate++;
      }
      console.log('Navigation vers step update', this.currentStepUpdate);
    }
  }

  previousStepUpdate(): void {
    if (this.currentStepUpdate > 1) {
      // Si la réception a des lignes et qu'on est au step 3, ne pas revenir en arrière
      if (this.receptionHasLignes && this.currentStepUpdate === 3) {
        this.notifyService.showInfo(
          'Impossible de modifier l\'affaire ou le bon de commande car cette réception contient déjà des articles.',
          'Navigation bloquée'
        );
        return;
      }
      this.currentStepUpdate--;
      console.log('Retour vers step update', this.currentStepUpdate);
    }
  }

  canGoToNextStepUpdate(): boolean {
    // Si la réception a des lignes, on peut passer directement au step 3
    if (this.receptionHasLignes && this.currentStepUpdate < 3) {
      return true;
    }

    switch (this.currentStepUpdate) {
      case 1:
        // Step 1: Affaire ET Bon de commande obligatoires (sauf si réception a des lignes)
        if (this.receptionHasLignes) return true;
        return this.selectedAffaireUpdate && this.selectedBonCommande !== null;

      case 2:
        // Step 2: Détails du BC - toujours valide
        return true;

      case 3:
        // Step 3: Date de réception obligatoire
        return this.receptionToUpdate?.dateBl !== null &&
          this.receptionToUpdate?.dateBl !== undefined &&
          this.receptionToUpdate?.referenceBl !== null &&
          this.receptionToUpdate?.referenceBl !== undefined &&
          this.receptionToUpdate?.referenceBl.trim() !== '';

      case 4:
        // Step 4: Pas de validation obligatoire
        return true;

      default:
        return false;
    }
  }

  private continueModalOpening(reception: any) {
    // CORRECTION : Vérifier si les bons de commande sont chargés
    if (!this.bonCommandes || this.bonCommandes.length === 0) {
      console.log('📋 Bons de commande non chargés, chargement en cours...');

      this.tempDataService.getAllBonCommandes().subscribe({
        next: (data) => {
          this.bonCommandes = data || [];
          console.log(`📋 ${this.bonCommandes.length} bons de commande chargés`);
          this.continueOpeningModalUpdate(reception);
        },
        error: (err) => {
          console.error("❌ Erreur chargement bons de commandes", err);
          this.notifyService.showError('Erreur lors du chargement des données', 'Erreur');
          this.bonCommandes = [];
        }
      });
    } else {
      console.log(`📋 Utilisation des ${this.bonCommandes.length} bons de commande en cache`);
      this.continueOpeningModalUpdate(reception);
    }
  }
  loadLignesBonCommandeUpdate(bonCommande: any) {
    if (!bonCommande || !bonCommande.commande) {
      this.lignesBonCommandeUpdate = [];
      return;
    }

    this.isLoadingLignesBCUpdate = true;
    console.log('Chargement des lignes pour BC:', bonCommande.commande);

    this.bonCommandeCacheService.getLignesBonCommande(bonCommande.commande).subscribe({
      next: (lignes) => {
        this.lignesBonCommandeUpdate = lignes || [];
        console.log('Lignes BC chargées:', this.lignesBonCommandeUpdate);
        this.isLoadingLignesBCUpdate = false;
      },
      error: (err) => {
        console.error('Erreur chargement lignes BC:', err);
        this.lignesBonCommandeUpdate = [];
        this.isLoadingLignesBCUpdate = false;
        this.notifyService.showError('Erreur lors du chargement des lignes du bon de commande', 'Erreur');
      }
    });
  }

  // Modifier la méthode existante ouvrirModalUpdateAvecDelay
  ouvrirModalUpdateAvecDelay(reception: any) {
    console.log('Ouverture modal de modification pour:', reception);

    if (!this.canModifyReceptions()) {
      this.notifyService.showError('Modification non autorisée', 'Permissions insuffisantes');
      return;
    }

    if (reception.statut === 'Envoyé') {
      this.notifyService.showError('Impossible de modifier une réception envoyée', 'Modification interdite');
      return;
    }

    try {
      // Réinitialiser le step
      this.currentStepUpdate = 1;
      this.receptionHasLignes = false; // AJOUT : Réinitialiser

      this.receptionToUpdate = {...reception};

      // Formater les dates
      if (this.receptionToUpdate.dateReception) {
        const dateRec = new Date(this.receptionToUpdate.dateReception);
        this.receptionToUpdate.dateReception = this.formatDateForInput(dateRec);
      }
      if (this.receptionToUpdate.dateBl) {
        const dateBl = new Date(this.receptionToUpdate.dateBl);
        this.receptionToUpdate.dateBl = this.formatDateForInput(dateBl);
      }
      // Initialiser les arrays
      this.selectedFiles = [];
      this.listPiecesJointes = [];
      this.lignesBonCommandeUpdate = [];
      this.selectedAffaireUpdate = null;
      this.selectedFournisseurUpdate = null;
      this.selectedBonCommande = null;

      // NOUVEAU : Vérifier si la réception a des lignes
      if (reception.id) {
        this.ligneReceptionService.getLignesReceptionByReceptionId(reception.id).subscribe({
          next: (lignes) => {
            this.receptionHasLignes = lignes && lignes.length > 0;
            console.log('Réception a des lignes:', this.receptionHasLignes, '(' + (lignes?.length || 0) + ' lignes)');

            // Si la réception a des lignes, passer directement au step 3
            if (this.receptionHasLignes) {
              this.currentStepUpdate = 3;
              this.notifyService.showInfo(
                'Cette réception contient déjà des articles. Seules la date et les pièces jointes peuvent être modifiées.',
                'Modification limitée'
              );
            }

            // Continuer l'ouverture du modal
            this.continueModalOpening(reception);
          },
          error: (err) => {
            console.error('Erreur vérification lignes:', err);
            this.receptionHasLignes = false;
            this.continueModalOpening(reception);
          }
        });
      } else {
        this.continueModalOpening(reception);
      }

    } catch (error) {
      console.error('Erreur dans ouvrirModalUpdateAvecDelay:', error);
      this.notifyService.showError('Erreur lors de l\'ouverture du modal', 'Erreur');
    }
  }

  private formatDateForInput(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
  private extractAvailableAffairesUpdate() {
    console.log('=== EXTRACTION DES AFFAIRES DISPONIBLES ===');
    console.log('Nombre de bons de commande filtrés:', this.bonCommandes.length);

    const affairesMap = new Map<string, any>();

    this.bonCommandes.forEach(bc => {
      if (bc.affaireCode) {
        if (!affairesMap.has(bc.affaireCode)) {
          affairesMap.set(bc.affaireCode, {
            code: bc.affaireCode,
            name: bc.affaireName || bc.affaireCode
          });
        }
      }
    });

    this.availableAffairesUpdate = Array.from(affairesMap.values())
      .sort((a, b) => a.code.localeCompare(b.code)); // Tri alphabétique

    console.log(`✅ ${this.availableAffairesUpdate.length} affaires disponibles:`,
      this.availableAffairesUpdate.map(a => a.code));
  }

  onAffaireFilterChangeUpdate(selectedAffaireCodeOrObj: any) {
    console.log('=== CHANGEMENT AFFAIRE UPDATE ===');
    console.log('Valeur reçue:', selectedAffaireCodeOrObj);

    let affaireCode: string;

    if (typeof selectedAffaireCodeOrObj === 'string') {
      affaireCode = selectedAffaireCodeOrObj;
    } else if (selectedAffaireCodeOrObj?.code) {
      affaireCode = selectedAffaireCodeOrObj.code;
    } else {
      this.filteredBonCommandesUpdate = [];
      this.availableFournisseursUpdate = [];
      this.selectedFournisseurUpdate = null;
      this.selectedBonCommande = null;
      return;
    }

    console.log('Code affaire extrait:', affaireCode);

    const bonCommandesByAffaire = this.bonCommandes.filter(
      bc => bc.affaireCode === affaireCode
    );

    // ⚠️ IMPORTANT : Extraire les fournisseurs AVANT de réinitialiser
    this.extractFournisseursFromBonCommandesUpdate(bonCommandesByAffaire);
    this.filteredBonCommandesUpdate = bonCommandesByAffaire;

    console.log(`${this.filteredBonCommandesUpdate.length} bons de commande pour l'affaire ${affaireCode}`);
    console.log('Fournisseurs extraits:', this.availableFournisseursUpdate);

    // ✅ CORRECTION : TOUJOURS réinitialiser lors du changement d'affaire
    // pour éviter de garder le BC de l'ancienne affaire
    this.selectedFournisseurUpdate = null;
    this.selectedBonCommande = null;

    // Vider aussi les lignes du BC
    this.lignesBonCommandeUpdate = [];

    console.log('✅ Fournisseur et BC réinitialisés suite au changement d\'affaire');
  }

  private extractFournisseursFromBonCommandesUpdate(bonCommandes: any[]) {
    const fournisseursMap = new Map<string, { name: string, id: string, count: number }>();

    bonCommandes.forEach(bc => {
      if (bc.fournisseur) {
        const key = bc.fournisseur.trim();
        if (fournisseursMap.has(key)) {
          const existing = fournisseursMap.get(key)!;
          existing.count++;
        } else {
          fournisseursMap.set(key, {
            name: bc.fournisseur.trim(),
            id: bc.fournisseurId || '',
            count: 1
          });
        }
      }
    });

    this.availableFournisseursUpdate = Array.from(fournisseursMap.values())
      .sort((a, b) => a.name.localeCompare(b.name));

    console.log(`Fournisseurs extraits pour update: ${this.availableFournisseursUpdate.length}`);
  }
  onFournisseurFilterChangeUpdate(selectedFournisseur: any) {
    console.log('=== CHANGEMENT FOURNISSEUR UPDATE ===');
    console.log('Fournisseur sélectionné:', selectedFournisseur);

    if (!selectedFournisseur) {
      console.log('Aucun fournisseur sélectionné - affichage de tous les BC de l\'affaire');

      // Si pas de fournisseur, afficher tous les BC de l'affaire
      if (this.selectedAffaireUpdate) {
        const affaireCode = typeof this.selectedAffaireUpdate === 'string'
          ? this.selectedAffaireUpdate
          : this.selectedAffaireUpdate.code;

        this.filteredBonCommandesUpdate = this.bonCommandes.filter(
          bc => bc.affaireCode === affaireCode
        );
      } else {
        this.filteredBonCommandesUpdate = [];
      }

      // Réinitialiser le BC sélectionné
      this.selectedBonCommande = null;
      if (this.receptionToUpdate) {
        this.receptionToUpdate.nomFournisseur = '';
        this.receptionToUpdate.refFournisseur = '';
      }

      this.cdr.detectChanges();
      return;
    }

    // Appliquer le filtre par fournisseur
    this.applyFournisseurFilter(selectedFournisseur);
  }
  /**
   * Applique le filtre par fournisseur sur les bons de commande
   */
  private applyFournisseurFilter(fournisseur: any) {
    console.log('=== APPLICATION FILTRE FOURNISSEUR ===');
    console.log('Fournisseur:', fournisseur);

    if (!fournisseur || !fournisseur.name) {
      console.warn('⚠️ Fournisseur invalide');
      return;
    }

    // Obtenir le code de l'affaire
    const affaireCode = typeof this.selectedAffaireUpdate === 'string'
      ? this.selectedAffaireUpdate
      : this.selectedAffaireUpdate?.code;

    if (!affaireCode) {
      console.warn('⚠️ Aucune affaire sélectionnée');
      return;
    }

    // Filtrer les BC par affaire ET fournisseur
    this.filteredBonCommandesUpdate = this.bonCommandes.filter(bc => {
      const matchAffaire = bc.affaireCode === affaireCode;
      const matchFournisseur = bc.fournisseur?.trim().toLowerCase() === fournisseur.name?.trim().toLowerCase();

      console.log(`BC ${bc.commande}: affaire=${matchAffaire}, fournisseur=${matchFournisseur}`);

      return matchAffaire && matchFournisseur;
    });

    console.log(`✅ ${this.filteredBonCommandesUpdate.length} BC filtrés pour le fournisseur "${fournisseur.name}"`);

    // Si le BC actuellement sélectionné n'est pas dans la liste filtrée, le réinitialiser
    if (this.selectedBonCommande) {
      const bcExists = this.filteredBonCommandesUpdate.find(
        bc => bc.commande === this.selectedBonCommande
      );

      if (!bcExists) {
        console.log('⚠️ BC actuel non compatible avec le fournisseur - réinitialisation');
        this.selectedBonCommande = null;

        if (this.receptionToUpdate) {
          this.receptionToUpdate.referenceBl = '';
          this.receptionToUpdate.dateBl = new Date().toISOString().split('T')[0];
        }

        // Vider les lignes
        this.lignesBonCommandeUpdate = [];
        this.lignesReception = [];
      }
    }

    // Forcer la détection des changements
    this.cdr.detectChanges();
  }
  onFournisseurClearUpdate() {
    console.log('=== CLEAR FOURNISSEUR UPDATE ===');

    this.selectedFournisseurUpdate = null;

    // Réafficher tous les BC de l'affaire
    if (this.selectedAffaireUpdate) {
      const affaireCode = typeof this.selectedAffaireUpdate === 'string'
        ? this.selectedAffaireUpdate
        : this.selectedAffaireUpdate.code;

      this.filteredBonCommandesUpdate = this.bonCommandes.filter(
        bc => bc.affaireCode === affaireCode
      );

      console.log(`✅ ${this.filteredBonCommandesUpdate.length} BC disponibles (tous fournisseurs)`);
    }

    // Réinitialiser le BC sélectionné
    this.selectedBonCommande = null;

    if (this.receptionToUpdate) {
      this.receptionToUpdate.nomFournisseur = '';
      this.receptionToUpdate.refFournisseur = '';
    }

    this.cdr.detectChanges();
  }

  onBonCommandeChangeUpdate(selectedBonCommande: any) {
    console.log('=== CHANGEMENT BON DE COMMANDE UPDATE ===');
    console.log('BC sélectionné:', selectedBonCommande);

    if (selectedBonCommande && this.receptionToUpdate) {
    /*  //this.receptionToUpdate.referenceBl = selectedBonCommande.referenceBC;
      this.receptionToUpdate.dateBl = receptionToUpdate.dateBl
        ? receptionToUpdate.dateBl.split('T')[0]
        : new Date().toISOString().split('T')[0];*/
      this.receptionToUpdate.affaireCode = selectedBonCommande.affaireCode;
      this.receptionToUpdate.nomFournisseur = selectedBonCommande.fournisseur;

      const fournisseur = this.fournisseurs.find(f => f.refFournisseur === selectedBonCommande.fournisseurId);
      if (fournisseur) {
        this.receptionToUpdate.nomFournisseur = fournisseur.nomFournisseur;
      }

      // Charger les lignes du bon de commande
      this.loadLignesBonCommandeUpdate(selectedBonCommande);
    }
  }

  // Modifier fermerModalUpdate pour réinitialiser le step
  fermerModalUpdate() {
    console.log('Fermeture du modal de modification');

    try {
      const modal = document.getElementById('updateModal');
      if (modal) {
        if ((window as any).$) {
          ((window as any).$ as any)(modal).modal('hide');
        } else if ((window as any).bootstrap) {
          const bsModal = (window as any).bootstrap.Modal.getInstance(modal) ||
            new (window as any).bootstrap.Modal(modal);
          bsModal.hide();
        } else {
          modal.classList.remove('show');
          modal.style.display = 'none';
          document.body.classList.remove('modal-open');
          const backdrop = document.querySelector('.modal-backdrop');
          if (backdrop) {
            backdrop.remove();
          }
        }
      }

      // Réinitialiser l'état
      this.resetFilesState();
      this.receptionToUpdate = null;
      this.selectedBonCommande = null;
      this.affaireDisplay = '';
      this.showUpdateModal = false;
      this.currentStepUpdate = 1;
      this.lignesBonCommandeUpdate = [];
      this.selectedAffaireUpdate = null;
      this.selectedFournisseurUpdate = null;
      this.receptionHasLignes = false; // AJOUT : Réinitialiser

      this.cdr.detectChanges();

    } catch (error) {
      console.error('Erreur fermeture modal:', error);

      // Force reset
      this.resetFilesState();
      this.receptionToUpdate = null;
      this.selectedBonCommande = null;
      this.affaireDisplay = '';
      this.showUpdateModal = false;
      this.currentStepUpdate = 1;
      this.lignesBonCommandeUpdate = [];
      this.receptionHasLignes = false; // AJOUT : Réinitialiser

      const modals = document.querySelectorAll('.modal.show');
      modals.forEach(modal => {
        modal.classList.remove('show');
        (modal as HTMLElement).style.display = 'none';
      });

      const backdrops = document.querySelectorAll('.modal-backdrop');
      backdrops.forEach(backdrop => backdrop.remove());

      document.body.classList.remove('modal-open');

      this.cdr.detectChanges();
    }
  }

  private continueOpeningModalUpdate(reception: any) {
    console.log('=== CONTINUATION OUVERTURE MODAL ===');
    console.log('Reception complète:', reception);

    // Extraire les affaires disponibles depuis les bons de commande
    this.extractAvailableAffairesUpdate();
    console.log('Affaires disponibles:', this.availableAffairesUpdate);

    // ========== CORRECTION 1 : Affaire ==========
    if (reception.affaireCode) {
      this.selectedAffaireUpdate = reception.affaireCode;
      console.log('✅ Affaire pré-sélectionnée (CODE):', this.selectedAffaireUpdate);

      const affaireObj = this.availableAffairesUpdate.find(
        aff => aff.code === reception.affaireCode
      );

      if (affaireObj) {
        // Filtrer les BC par affaire ET extraire les fournisseurs
        this.onAffaireFilterChangeUpdate(affaireObj);
      }
    }

    // ========== CORRECTION 2 : Bon de Commande ==========
    setTimeout(() => {
      if (reception.affaireId) {
        console.log('Recherche BC pour affaireId:', reception.affaireId);

        const bc = this.bonCommandes.find(bc =>
          bc.commande === parseInt(reception.affaireId) ||
          bc.commande?.toString() === reception.affaireId?.toString()
        );

        if (bc) {
          this.selectedBonCommande = bc.commande;
          console.log('✅ BC pré-sélectionné (NUMÉRO):', this.selectedBonCommande);

          if (this.receptionToUpdate) {
          /*  this.receptionToUpdate.referenceBl = bc.referenceBC || bc.commande?.toString();
            this.receptionToUpdate.dateBl = bc.dateCommande
              ? bc.dateCommande.split('T')[0]
              : new Date().toISOString().split('T')[0];*/
            this.receptionToUpdate.affaireCode = bc.affaireCode;
            this.receptionToUpdate.nomFournisseur = bc.fournisseur;
          }

          // Charger les lignes du BC
          this.loadLignesBonCommandeUpdate(bc);

          // ⚠️ CORRECTION FOURNISSEUR : Pré-sélectionner APRÈS avoir chargé le BC
          setTimeout(() => {
            const nomFournisseur = bc.fournisseur || reception.nomFournisseur;
            this.preselectFournisseur(nomFournisseur);

            // ⚠️ NOUVEAU : Filtrer les BC par fournisseur après la pré-sélection
            setTimeout(() => {
              if (this.selectedFournisseurUpdate) {
                this.applyFournisseurFilter(this.selectedFournisseurUpdate);
              }
            }, 50);
          }, 100);
        }
      }
    }, 200);

    // ========== Charger les fichiers existants ==========
    if (this.receptionToUpdate?.id) {
      this.loadReceptionFiles(this.receptionToUpdate.id)
        .then((files) => {
          this.listPiecesJointes = files;
          console.log("✅ Fichiers chargés:", files.length);
        })
        .catch((error) => {
          console.error("❌ Erreur chargement fichiers:", error);
          this.listPiecesJointes = [];
        });
    }

    // ========== Ouvrir le modal ==========
    this.showUpdateModal = true;
    this.cdr.detectChanges();

    setTimeout(() => {
      try {
        const modalElement = document.getElementById('updateModal');

        if ((window as any).$) {
          ((window as any).$ as any)('#updateModal').modal('show');
        } else if ((window as any).bootstrap) {
          if (modalElement) {
            const bsModal = new (window as any).bootstrap.Modal(modalElement);
            bsModal.show();
          }
        } else {
          if (modalElement) {
            modalElement.classList.add('show');
            modalElement.style.display = 'block';
            document.body.classList.add('modal-open');

            const backdrop = document.createElement('div');
            backdrop.className = 'modal-backdrop fade show';
            document.body.appendChild(backdrop);
          }
        }

        console.log('✅ Modal ouvert avec succès');
      } catch (error) {
        console.error('❌ Erreur ouverture modal:', error);
        this.notifyService.showError('Erreur ouverture du modal', 'Erreur');
      }
    }, 400);
  }
  /**
   * Pré-sélectionne le fournisseur dans le ng-select
   */
  private preselectFournisseur(nomFournisseur: string) {
    console.log('=== PRÉ-SÉLECTION FOURNISSEUR ===');
    console.log('Nom fournisseur à rechercher:', nomFournisseur);
    console.log('Fournisseurs disponibles:', this.availableFournisseursUpdate);

    if (!nomFournisseur || this.availableFournisseursUpdate.length === 0) {
      console.warn('⚠️ Pas de fournisseur à pré-sélectionner ou liste vide');
      return;
    }

    // Recherche flexible du fournisseur
    const fournisseurObj = this.availableFournisseursUpdate.find(f => {
      const fName = f.name?.trim().toLowerCase();
      const searchName = nomFournisseur?.trim().toLowerCase();

      console.log(`Comparaison: "${fName}" === "${searchName}"`);

      return fName === searchName;
    });

    if (fournisseurObj) {
      this.selectedFournisseurUpdate = fournisseurObj;
      console.log('✅ Fournisseur pré-sélectionné:', this.selectedFournisseurUpdate);

      // Forcer la détection des changements
      this.cdr.detectChanges();

      // Optionnel : filtrer les BC par ce fournisseur
      // this.onFournisseurFilterChangeUpdate(fournisseurObj);
    } else {
      console.error('❌ Fournisseur non trouvé dans la liste:', nomFournisseur);
      console.log('Fournisseurs disponibles:', this.availableFournisseursUpdate.map(f => f.name));
    }
  }
}

