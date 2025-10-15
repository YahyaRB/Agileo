// list-demande-achat.component.ts
import {ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, Renderer2, ViewChild} from '@angular/core';
import {DemandeAchatService} from "../../../services/demande-achat.service";
import {IDemandeAchat} from "../../../../interfaces/idemandeAchat";
import {NotificationService} from "../../../services/notification.service";
import {ILigneDemande} from "../../../../interfaces/ilignedemande";
import {LigneDemandeAchatService} from "../../../services/ligne-demande-achat.service";
import {SortService} from "../../../services/sort.service";
import {ConsommationService} from "../../../services/consommation.service";
import {PagedResponse, PaginationParams} from "../../../../interfaces/PagedResponse";
import {environment} from "../../../../environments/environment";
import { HttpClient, HttpHeaders } from '@angular/common/http';
import {KeycloakService} from "keycloak-angular";
import { Router, NavigationStart } from '@angular/router'; // ✅ AJOUTER CET IMPORT

// Déclarer jQuery pour éviter les erreurs TypeScript
declare var $: any; // ✅ AJOUTER CETTE LIGNE


@Component({
  selector: 'app-list-demande-achat',
  templateUrl: './list-demande-achat.component.html',
  styleUrls: ['./list-demande-achat.component.css']
})
export class ListDemandeAchatComponent implements OnInit,OnDestroy {
  @ViewChild('btnFakeAdd', {static: false}) btnFakeAdd!: ElementRef;
  @ViewChild('btnFakeUpdate', {static: false}) btnFakeUpdate!: ElementRef;
  @ViewChild('btnFake', {static: false}) btnFake!: ElementRef;

  // État des modals
  showModal = false;
  showDetailsModal = false;
  selectedFiles: File[] = [];
  isDragOver = false;
  isUploading = false;
  maxFiles = 3;
  // Listes et données
  listDemandesAchat: IDemandeAchat[] = [];
  listLigneDemandeDchat: ILigneDemande[] = [];
  selectedDemande!: IDemandeAchat;
  selectedDemandeForDetails: IDemandeAchat | null = null;

  // Gestion de l'envoi et suppression
  demandeIdToSend: number | null = null;
  demandeIdToDelete: number | null = null;

  // État de l'interface
  isCollapsed = false;
  isFullscreen = false;
  isLoading = false;
  isLoadingDetails = false;

  // Recherche et filtrage
  pfiltre: any;
  listPiecesJointes: any[] = [];
  loadingFiles = false;
  errorFiles = false;
  // Pagination
  page: number = 1;
  count: number = 0;
  tableSize: number = 10;
  tableSizes: number[] = [5, 10, 15, 20];
// NOUVELLE PAGINATION
  pagedResponse: PagedResponse<IDemandeAchat> | null = null;
  paginationParams: PaginationParams = {
    page: 0, // Spring Boot commence à 0
    size: 10,
    sortBy: 'id',
    sortDirection: 'desc'
  };
  // Tri
  sort = {
    field: '',
    direction: 'asc' as 'asc' | 'desc'
  };

  constructor(
    private demandeAchatService: DemandeAchatService,
    private ligneDemandeService: LigneDemandeAchatService,
    private notifyService: NotificationService,
    private http: HttpClient,
    private keycloakService: KeycloakService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnDestroy(): void {
    this.forceCleanup();
  }
  ngOnInit(): void {
    this.loadDemandesAchat();
    setTimeout(() => {
      this.forceCleanup();
    }, 500);
  }


  private forceCleanup(): void {
    try {
      const modals = document.querySelectorAll('.modal');
      modals.forEach((modalEl: Element) => {
        const modal = modalEl as HTMLElement;

        // Méthode 1 : Via Bootstrap 5
        if ((window as any).bootstrap?.Modal) {
          const bsModal = (window as any).bootstrap.Modal.getInstance(modal);
          if (bsModal) {
            bsModal.hide();
            bsModal.dispose();
          }
        }

        // Méthode 2 : Via jQuery/Bootstrap 4
        if (typeof $ !== 'undefined' && $.fn.modal) {
          $(modal).modal('hide');
          $(modal).modal('dispose');
        }

        // Méthode 3 : Nettoyage manuel
        modal.classList.remove('show', 'fade', 'in');
        modal.style.display = 'none';
        modal.setAttribute('aria-hidden', 'true');
        modal.removeAttribute('aria-modal');
        modal.removeAttribute('role');
        modal.removeAttribute('style');
      });
    } catch (e) {
      console.log('Erreur nettoyage modals (normal):', e);
    }

    // 2. Supprimer TOUS les backdrops
    setTimeout(() => {
      const backdrops = document.querySelectorAll('.modal-backdrop, .fade, .show');
      backdrops.forEach(backdrop => {
        if (backdrop.classList.contains('modal-backdrop')) {
          backdrop.remove();
        }
      });
    }, 100);

    // 3. Nettoyer le body
    document.body.classList.remove('modal-open');
    document.body.style.removeProperty('overflow');
    document.body.style.removeProperty('padding-right');
    document.body.removeAttribute('data-bs-overflow');
    document.body.removeAttribute('data-bs-padding-right');

    // 4. Réinitialiser le padding-right qui peut bloquer
    document.body.style.paddingRight = '';

    console.log('✅ Nettoyage terminé');
  }

  private forceModalAboveFullscreen(): void {
    console.log('Forçage des z-index des modals...');

    setTimeout(() => {
      // 1. Nettoyer les anciens backdrops
      const oldBackdrops = document.querySelectorAll('.modal-backdrop');
      if (oldBackdrops.length > 1) {
        for (let i = 0; i < oldBackdrops.length - 1; i++) {
          oldBackdrops[i].remove();
        }
      }

      // 2. Forcer le z-index de la carte fullscreen
      const fullscreenCard = document.querySelector('.card.fullscreen, .card-fullscreen');
      if (fullscreenCard) {
        (fullscreenCard as HTMLElement).style.zIndex = '1040';
      }

      // 3. Gérer les backdrops
      const backdrops = document.querySelectorAll('.modal-backdrop');
      backdrops.forEach((backdrop: Element) => {
        const backdropElement = backdrop as HTMLElement;
        backdropElement.style.zIndex = '1050';
        backdropElement.style.pointerEvents = 'none';
      });

      // 4. Gérer les modals
      const modals = document.querySelectorAll('.modal');
      modals.forEach((modal: Element) => {
        const modalElement = modal as HTMLElement;
        modalElement.style.zIndex = '1055';

        const modalDialog = modalElement.querySelector('.modal-dialog') as HTMLElement;
        if (modalDialog) {
          modalDialog.style.zIndex = '1060';
          modalDialog.style.position = 'relative';
        }

        const modalContent = modalElement.querySelector('.modal-content') as HTMLElement;
        if (modalContent) {
          modalContent.style.zIndex = '1061';
          modalContent.style.position = 'relative';
          modalContent.style.pointerEvents = 'auto';
        }
      });

      // 5. S'assurer que le body est dans le bon état
      if (!document.querySelector('.modal.show')) {
        document.body.classList.remove('modal-open');
        document.body.style.removeProperty('padding-right');
      }

      console.log('✅ Z-index appliqués');
    }, 100);
  }

  toggleCollapse() {
    this.isCollapsed = !this.isCollapsed;
  }


  private loadDemandeFiles(demandeId: number): Promise<any[]> {


    console.log("=== DEBUG LOAD FILES ===");
    console.log("Chargement des fichiers pour demande ID:", demandeId);
    console.log("Chargement des fichiers pour demande ID:", demandeId);

    return this.demandeAchatService.getFilesByDemande(demandeId)
      .toPromise()
      .then((files) => {
        console.log("=== RÉPONSE API FICHIERS ===");
        console.log("Fichiers récupérés:", JSON.stringify(files, null, 2));
        console.log("Nombre de fichiers:", files?.length || 0);
        console.log("Fichiers récupérés:", files);
        return files || [];
      })
      .catch((error) => {
        console.error("Erreur lors du chargement des fichiers:", error);
        return []; // Retourner un tableau vide en cas d'erreur
      });
  }
  ouvrirModalAvecDelay() {
    this.showModal = true;
    this.forceModalAboveFullscreen(); // <-- AJOUTEZ
    this.cdr.detectChanges();
    setTimeout(() => {
      const modalBtn = document.querySelector('[data-target="#addModal"]') as HTMLElement;
      if (modalBtn) {
        modalBtn.click();
      }
    }, 100);
  }

  /**
   * Chargement des demandes d'achat avec gestion du statut
   */
  loadDemandesAchat(): void {
    this.isLoading = true;
    this.demandeAchatService.getAllDemandesPaginated(this.paginationParams).subscribe({
      next: (response) => {
        this.pagedResponse = response;
        this.listDemandesAchat = response.content;
        // Synchroniser avec l'ancienne pagination pour le template
        this.count = response.totalElements;
        this.page = response.pageNumber + 1; // Convertir pour ngx-pagination (commence à 1)
        this.processDemandesData();
        this.isLoading = false;
        console.log("Chargement liste de demandes paginee : ", this.listDemandesAchat);

      },
      error: (err) => {
        this.handleLoadError(err);
      }
    });
  }
  private handleLoadError(err: any): void {
    this.isLoading = false;
    console.error("❌ Erreur lors du chargement des demandes:", err);
    this.notifyService.showError('Erreur lors du chargement des demandes', 'Erreur');
  }
  private processDemandesData(): void {
    this.listDemandesAchat.forEach((demande) => {
      // Mapping des champs legacy
      if (!demande.affaireCode && (demande as any).chantier) {
        demande.affaireCode = (demande as any).chantier;
      }
      if (!demande.dateCreation && (demande as any).sysCreationDate) {
        demande.dateCreation = (demande as any).sysCreationDate;
      }
      if (!demande.createdBy && (demande as any).demandeurNom) {
        demande.createdBy = (demande as any).demandeurNom;
      }
      if (!demande.delai && (demande as any).delaiSouhaite) {
        demande.delai = (demande as any).delaiSouhaite;
      }

      // Initialiser nbLignes
      if (demande.nbLignes === undefined || demande.nbLignes === null) {
        demande.nbLignes = (demande as any).nombreLignes || 0;
      }
    });
  }
  /**
   * Télécharger un fichier avec authentification
   */
  async downloadFile(file: any): Promise<void> {
    try {
        const fileId = file.fileId || file.id;
        if (!fileId) {
        console.error("ERREUR: fileId est undefined, null ou falsy");
        this.notifyService.showError('ID de fichier manquant', 'Erreur');
        return;
      }
      const downloadUrl = `${environment.apiUrl}demandes-achat/files/${file.fileId || file.id}/download`;
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
          let filename = file.fullFileName || file.name || 'fichier';
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

  openDetailsModal(demande: IDemandeAchat): void {
    this.selectedDemandeForDetails = demande;
    this.isLoadingDetails = true;
    this.selectedDemande=demande
    // Réinitialiser les listes
    this.listLigneDemandeDchat = [];
    this.listPiecesJointes = [];

    // Charger les lignes ET les fichiers en parallèle
    const lignesPromise = demande.id ?
      this.ligneDemandeService.getLignesDemandeByDemandeId(demande.id).toPromise() :
      Promise.resolve([]);

    const filesPromise = demande.id ?
      this.loadDemandeFiles(demande.id) :
      Promise.resolve([]);

    Promise.all([lignesPromise, filesPromise])
      .then(([lignes, files]) => {
        this.listLigneDemandeDchat = lignes || [];
        this.listPiecesJointes = files || [];
        this.isLoadingDetails = false;
        this.showDetailsModal = true;

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
      });
  }

  isDemandeEnvoyee(demande: IDemandeAchat): boolean {
    return demande.statut !== null && demande.statut !== undefined && demande.statut !== 0;
  }

  /**
   * Obtenir le label du statut
   */
  getStatusLabel(statut: number | undefined | null): string {
    if (statut === null || statut === undefined || statut === 0) {
      return 'Brouillon';
    }
    switch (statut) {
      case 1:
        return 'Envoyé';
      case 2:
        return 'Reçu';
      case 3:
        return 'Approuvé';
      case -1:
        return 'Rejeté';
      default:
        return 'Inconnu';
    }
  }

  /**
   * Obtenir la classe CSS du statut
   */
  getStatusClass(statut: number | undefined | null): string {
    if (statut === null || statut === undefined || statut === 0) {
      return 'badge badge-secondary';
    }
    switch (statut) {
      case 1:
        return 'badge badge-warning';
      case 2:
        return 'badge badge-success';
      case 3:
        return 'badge badge-primary';
      case -1:
        return 'badge badge-danger';
      default:
        return 'badge badge-light';
    }
  }

  /**
   * Formater une date
   */
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

  /**
   * Gestion du clic sur une demande désactivée
   */
  onDisabledDemandeClick(event: Event, demande: IDemandeAchat): void {
    event.preventDefault();
    this.openDetailsModal(demande);
  }

  /**
   * Ouvrir le modal d'ajout
   */
  openModalForAddDemande(): void {
    this.showModal = true;
    this.cdr.detectChanges();
    setTimeout(() => {
      this.btnFakeAdd.nativeElement.click();
    }, 100);
  }

  /**
   * Ouvrir le modal de modification
   */
  openModalForUpdateDemande(demande: IDemandeAchat): void {
    this.selectedDemande = demande;
    this.showModal = true;
    this.forceModalAboveFullscreen();
    this.cdr.detectChanges();
    setTimeout(() => {
      this.btnFakeUpdate.nativeElement.click();
    }, 100);
  }

  openConfirmSendModal(demandeId: number | undefined): void {
    if (!demandeId) return;

    // ✅ VÉRIFIER S'IL Y A DES LIGNES AVANT D'OUVRIR LE MODAL
    this.ligneDemandeService.getLignesDemandeByDemandeId(demandeId).subscribe({
      next: (lignes) => {
        if (!lignes || lignes.length === 0) {
          // ✅ Message harmonisé avec consommations et réceptions
          this.notifyService.showError(
            'Impossible d\'envoyer une demande d\'achat vide. Veuillez ajouter au moins un article avant l\'envoi.',
            'Demande vide'
          );
          return;
        }

        // Si des lignes existent, ouvrir le modal de confirmation
        this.demandeIdToSend = demandeId;
        this.forceModalAboveFullscreen();
        setTimeout(() => {
          (window as any).$('#confirmSendModal').modal('show');
        }, 100);
      },
      error: (error) => {
        console.error('Erreur lors de la vérification des lignes:', error);
        this.notifyService.showError(
          'Erreur lors de la vérification du contenu de la demande',
          'Erreur'
        );
      }
    });
  }

  /**
   * Confirmer l'envoi de la demande
   */
  sendDemandeConfirmed(): void {
    if (!this.demandeIdToSend) return;

    this.demandeAchatService.updateDemandeStatut(this.demandeIdToSend, 1).subscribe({
      next: () => {
        this.notifyService.showSuccess('Demande envoyée avec succès', 'Succès');
        this.loadDemandesAchat();
        this.demandeIdToSend = null;
        (window as any).$('#confirmSendModal').modal('hide');
      },
      error: (err) => {
        console.error("Erreur lors de l'envoi:", err);
        this.notifyService.showError(
          err?.error?.message || "Erreur lors de l'envoi de la demande",
          'Erreur'
        );
        this.demandeIdToSend = null;
      }
    });
  }

  /**
   * Préparer la suppression d'une demande
   */
  SuppressionDemande(id: number | undefined): void {
    if (!id) return;
    this.demandeIdToDelete = id;
  }

  /**
   * Confirmer la suppression
   */
  suppressionDemandeConfirmed(): void {
    if (!this.demandeIdToDelete) return;
    this.demandeAchatService.deleteDemandeAchat(this.demandeIdToDelete).subscribe({
      next: () => {
        this.notifyService.showSuccess('Demande supprimée avec succès', 'Succès');
        this.loadDemandesAchat();
        this.demandeIdToDelete = null;
      },
      error: (err) => {
        console.error("Erreur lors de la suppression:", err);
        this.notifyService.showError(
          err?.error?.message || "Erreur lors de la suppression",
          'Erreur'
        );
        this.demandeIdToDelete = null;
      }
    });
  }

  sortColumn(column: string): void {

      // NOUVELLE APPROCHE - TRI CÔTÉ SERVEUR
      if (this.paginationParams.sortBy === column) {
        this.paginationParams.sortDirection =
          this.paginationParams.sortDirection === 'asc' ? 'desc' : 'asc';
      } else {
        this.paginationParams.sortBy = column;
        this.paginationParams.sortDirection = 'asc';
      }
      this.paginationParams.page = 0; // Retour à la première page
      // Synchroniser avec l'ancien système de tri pour le template
      this.sort.field = column;
      this.sort.direction = this.paginationParams.sortDirection;
      this.loadDemandesAchat();
  }

  /**
   * Gestion de la pagination
   */
  onTableDataChange(event: number): void {
    // this.page = event;
    // this.loadDemandesAchat();
    this.paginationParams.page = event - 1; // Convertir de 1-based à 0-based
    this.loadDemandesAchat();
  }

  onTableSizeChange(event: any): void {
    const newSize = parseInt(event.target.value);
    this.paginationParams.size = newSize;
    this.paginationParams.page = 0;
    this.loadDemandesAchat();
  }



  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = true;
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

    // Ajouter les fichiers valides
    this.selectedFiles = [...this.selectedFiles, ...validFiles];

    // Afficher les erreurs s'il y en a
    if (errors.length > 0) {
      this.notifyService.showError(errors.join('\n'), 'Fichiers rejetés');
    }

    // Confirmation si des fichiers ont été ajoutés
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

  /**
   * Vérifier si on peut encore ajouter des fichiers
   */
  canAddMoreFiles(): boolean {
    return this.listPiecesJointes.length < this.maxFiles &&
      !this.isDemandeEnvoyee(this.selectedDemandeForDetails!);
  }
}
