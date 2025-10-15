import {AfterViewInit, ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit} from '@angular/core';
import {AffaireServiceService} from "../../../services/affaire-service.service";
import {ActivatedRoute, Router} from "@angular/router";
import {MatDialog} from "@angular/material/dialog";
import {ConsommationService} from "../../../services/consommation.service";
import {LigneConsommationService} from "../../../services/ligne-consommation.service";
import {UserProfileService} from "../../../services/user-profile.service";
import {NotificationService} from "../../../services/notification.service";
import {IConsommation} from "../../../../interfaces/iconsommation";
import {ILigneConsommation} from "../../../../interfaces/iligneconsommation";
import {Affaire} from "../../../../interfaces/iaffaire";
import {SortService} from "../../../services/sort.service";
import {UserData} from "../../../../interfaces/iuser";
import {SharedAffaireService} from "../../../services/shared-affaire.service";

@Component({
  selector: 'app-list-consommation',
  templateUrl: './list-consommation.component.html',
  styleUrls: ['./list-consommation.component.css']
})
export class ListConsommationComponent implements OnInit, OnDestroy, AfterViewInit {
  showModal = false;
  showDetailsModal = false;
  listConsommations!: IConsommation[];
  selectedConsommation: IConsommation | null = null;
  affairesMap: Map<String, Affaire> = new Map();
  lignesConsommation: ILigneConsommation[] = [];
  loadingLignes = false;
  errorLignes = false;
  isCollapsed = false;
  isFullscreen = false;
  sort = { field: '', direction: 'asc' as 'asc' | 'desc' };
  consommationHasLines = false; // Pour tracker si la consommation a des lignes
  checkingLines = false; // Pour l'état de chargement lors de la vérification
  showUpdateModal = false;
  consommationToUpdate: IConsommation | null = null;
  selectedAffaire: any = null;
  availableAffaires: Affaire[] = [];

  consommationEnCoursDeSuppression: IConsommation | null = null;
  consommationEnCoursDEnvoi: IConsommation | null = null;

  currentUser: UserData | null = null;
  isLoadingUser = true;

  pfiltre: any;
  page: number = 1;
  count: number = 0;
  tableSize: number = 10;
  tableSizes: any = [5, 10, 15, 20];

  constructor(
    private affaireService: AffaireServiceService,
    private consommationService: ConsommationService,
    private sharedAffaireService: SharedAffaireService, // NOUVEAU SERVICE
    private ligneConsommationService: LigneConsommationService,
    private userProfileService: UserProfileService,
    private notificationService: NotificationService,
    private elementRef: ElementRef,
    private cdr: ChangeDetectorRef,
    private sortService: SortService,
    private route: ActivatedRoute,
    private dialog: MatDialog,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadAffaires();
  }
  ngOnDestroy() {
    document.querySelectorAll('.modal-backdrop').forEach(el => el.remove());
    document.querySelectorAll('.ng-dropdown-panel').forEach(el => el.remove());
    document.body.classList.remove('modal-open');
    document.body.style.overflow = '';
    document.body.style.paddingRight = '';
  }
  loadCurrentUser() {
    this.isLoadingUser = true;
    this.userProfileService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUser = {
          id: user.id,
          email: user.email,
          matricule: user.matricule,
          nom: user.nom,
          prenom: user.prenom,
          statut: user.statut,
          login: user.login,
          roles: user.roles ?
            Array.from(user.roles).map((role: any) =>
              typeof role === 'string' ? role : role.name
            ) : [],
          acces: user.acces ? user.acces.map((access: any) => access.code) : [],
          affaires: user.affaires || []
        };
        this.isLoadingUser = false;
        this.loadConsommations();
      },
      error: (error) => {
        console.error('Erreur chargement utilisateur:', error);
        this.isLoadingUser = false;
        this.loadConsommations();
      }
    });
  }

  toggleCollapse() {
    this.isCollapsed = !this.isCollapsed;
  }

  toggleFullscreen() {
    this.isFullscreen = !this.isFullscreen;

    if (this.isFullscreen) {
      let backdrop = document.querySelector('.fullscreen-backdrop') as HTMLElement;
      if (!backdrop) {
        backdrop = document.createElement('div');
        backdrop.className = 'fullscreen-backdrop';
        backdrop.addEventListener('click', () => this.toggleFullscreen());
        document.body.appendChild(backdrop);
      }

      backdrop.classList.add('show');
      document.body.classList.add('fullscreen-active');
      const modals = document.querySelectorAll('.modal');
      modals.forEach(modal => {
        (modal as HTMLElement).style.zIndex = '10050';
      });
    } else {
      const backdrop = document.querySelector('.fullscreen-backdrop') as HTMLElement;
      if (backdrop) {
        backdrop.classList.remove('show');
      }
      document.body.classList.remove('fullscreen-active');

      const modals = document.querySelectorAll('.modal');
      modals.forEach(modal => {
        (modal as HTMLElement).style.zIndex = '';
      });
    }

    this.cdr.detectChanges();
  }

  hasRole(roleName: string): boolean {
    return this.currentUser?.roles?.includes(roleName) || false;
  }

  hasAccess(accessCode: string): boolean {
    return this.currentUser?.acces?.includes(accessCode) || false;
  }

  canViewAllConsommations(): boolean {
    return this.hasRole('ADMIN') || this.hasRole('MANAGER') || this.hasRole('CONSULTEUR');
  }

  canCreateConsommations(): boolean {
    return this.hasRole('ADMIN') ||
      this.hasRole('MANAGER') ||
      this.hasRole('CHEF DE PROJET') ||
      this.hasAccess('consommation');
  }

  canModifyConsommations(): boolean {
    return this.hasRole('ADMIN') ||
      this.hasRole('MANAGER') ||
      this.hasRole('CHEF DE PROJET') ||
      this.hasAccess('consommation');
  }

  canDeleteConsommations(): boolean {
    return this.hasRole('ADMIN') ||
      this.hasRole('MANAGER') ||
      this.hasRole('CHEF DE PROJET') ||
      this.hasAccess('consommation');
  }

  canSendConsommations(): boolean {
    return this.hasRole('ADMIN') ||
      this.hasRole('MANAGER') ||
      this.hasRole('CHEF DE PROJET') ||
      this.hasAccess('consommation');
  }

  canViewConsommationLines(): boolean {
    return this.hasRole('ADMIN') ||
      this.hasRole('MANAGER') ||
      this.hasRole('CONSULTEUR') ||
      this.hasAccess('consommation');
  }

  canModifyConsommationLines(): boolean {
    return this.hasRole('ADMIN') ||
      this.hasRole('MANAGER') ||
      this.hasRole('CHEF DE PROJET') ||
      this.hasAccess('consommation');
  }

  loadConsommations() {
    if (this.canViewAllConsommations()) {
      this.consommationService.getAllConsommations().subscribe({
        next: data => {
          this.listConsommations = data.sort((a, b) => {
            const dateA = new Date(a.createdDate || 0);
            const dateB = new Date(b.createdDate || 0);
            return dateB.getTime() - dateA.getTime();
          });
        },
        error: err => {
          console.log('Erreur chargement toutes consommations:', err);
          this.listConsommations = [];
        }
      });
    } else if (this.currentUser?.login) {
      this.consommationService.getCurrentUserConsommations().subscribe({
        next: data => {
          this.listConsommations = data.sort((a, b) => {
            const dateA = new Date(a.createdDate || 0);
            const dateB = new Date(b.createdDate || 0);
            return dateB.getTime() - dateA.getTime();
          });
        },
        error: err => {
          console.log('Erreur consommations utilisateur:', err);
          if (this.currentUser?.login) {
            this.consommationService.getConsommationsByUser(this.currentUser.id).subscribe({
              next: data => {
                this.listConsommations = data.filter(consommation =>
                  consommation.userLogin === this.currentUser?.login?.toString()
                ).sort((a, b) => {
                  const dateA = new Date(a.createdDate || 0);
                  const dateB = new Date(b.createdDate || 0);
                  return dateB.getTime() - dateA.getTime();
                });
              },
              error: err2 => {
                console.log('Erreur fallback:', err2);
                this.listConsommations = [];
              }
            });
          }
        }
      });
    } else {
      this.listConsommations = [];
      console.warn('Utilisateur non identifié - aucune consommation chargée');
    }
  }

  loadAffaires() {
    // Utiliser le service partagé pour charger toutes les affaires
    this.sharedAffaireService.getAllAffaires().subscribe({
      next: (affaires) => {
        // Mettre à jour la map locale pour compatibilité
        this.affairesMap.clear();
        affaires.forEach(affaire => {
          this.affairesMap.set(affaire.code!, affaire);
        });
        this.loadConsommations();
      },
      error: (err) => {
        console.error("Erreur chargement affaires", err);
        this.loadConsommations();
      }
    });
  }

  getAffaireLibelle(affaireCode: string): string {
    const affaire = this.affairesMap.get(affaireCode);
    return affaire ? affaire.libelle || affaire.nom || '' : '';
  }

  isUserOwnConsommation(consommation: IConsommation): boolean {
    return consommation.userLogin === this.currentUser?.login?.toString();
  }

  ouvrirModalAvecDelay() {
    if (!this.canCreateConsommations()) {
      this.notificationService.showError(
        'Vous n\'avez pas les permissions nécessaires pour créer une consommation',
        'Accès refusé'
      );
      return;
    }

    this.showModal = true;
    this.cdr.detectChanges();

    setTimeout(() => {
      this.ouvrirModalBootstrap('addModal');
    }, 100);
  }


  ouvrirModalDetails(consommation: IConsommation) {
    this.selectedConsommation = consommation;
    this.showDetailsModal = true;

    this.lignesConsommation = [];
    this.loadingLignes = false;
    this.errorLignes = false;

    this.loadLignesConsommation(consommation.id!);

    this.cdr.detectChanges();
    setTimeout(() => {
      this.ouvrirModalBootstrap('detailsModal');
    }, 100);
  }




  loadLignesConsommation(consommationId: number) {
    this.loadingLignes = true;
    this.errorLignes = false;
    this.lignesConsommation = [];

    this.ligneConsommationService.getLignesConsommationByConsommationId(consommationId).subscribe({
      next: (lignes) => {

        this.lignesConsommation = lignes || [];
        this.loadingLignes = false;
        this.errorLignes = false;
      },
      error: (error) => {

        this.errorLignes = true;
        this.loadingLignes = false;
        this.lignesConsommation = [];

        // Notification d'erreur
        this.notificationService.showError(
          'Erreur lors du chargement des articles consommés: ' + (error.error?.message || error.message || 'Erreur inconnue'),
          'Erreur de chargement'
        );
      }
    });
  }

  getTotalQuantity(): string {
    if (!this.lignesConsommation || this.lignesConsommation.length === 0) {
      return '0';
    }
    const total = this.lignesConsommation.reduce((sum, ligne) => sum + (ligne.quantite || 0), 0);
    return total.toFixed(2);
  }

  getDistinctUnits(): number {
    if (!this.lignesConsommation || this.lignesConsommation.length === 0) {
      return 0;
    }
    const distinctUnits = [...new Set(this.lignesConsommation.map(ligne => ligne.unite))];
    return distinctUnits.length;
  }

  canAccessConsommationLines(consommation: IConsommation): boolean {
    return this.canViewConsommationLines() || this.isUserOwnConsommation(consommation);
  }

  canAccessConsommationLinesReadOnly(consommation: IConsommation): boolean {
    return this.hasRole('CONSULTEUR') && !this.canModifyConsommationLines();
  }

  navigateToConsommationLines(consommation: IConsommation) {
    if (this.canAccessConsommationLines(consommation)) {
      this.router.navigate(['/consommations', consommation.id, 'add-ligne-consommation']);
    } else {
      this.notificationService.showError(
        'Vous n\'avez pas les permissions nécessaires pour accéder aux lignes de cette consommation',
        'Accès refusé'
      );
    }
  }

  changerStatutConsommation(consommation: IConsommation, nouveauStatut: string) {
    if (nouveauStatut === 'Envoyé' && !this.canSendConsommations()) {
      this.notificationService.showError(
        'Vous n\'avez pas les permissions nécessaires pour envoyer une consommation',
        'Accès refusé'
      );
      return;
    }

    if (nouveauStatut === 'Envoyé') {
      this.ligneConsommationService.getLignesConsommationByConsommationId(consommation.id!).subscribe({
        next: (lignes) => {
          if (!lignes || lignes.length === 0) {
            this.notificationService.showError(
              'Impossible d\'envoyer une consommation vide. Veuillez ajouter au moins un article avant l\'envoi.',
              'Consommation vide'
            );
            return;
          }

          this.consommationEnCoursDEnvoi = { ...consommation, statut: nouveauStatut };
          this.ouvrirModalConfirmationEnvoi();
        },
        error: (error) => {
          console.error('Erreur lors de la vérification des lignes:', error);
          this.notificationService.showError(
            'Erreur lors de la vérification du contenu de la consommation',
            'Erreur'
          );
        }
      });
    } else {
      this.procederEnvoiConsommation(consommation, nouveauStatut);
    }
  }

  private procederEnvoiConsommation(consommation: IConsommation, nouveauStatut?: string) {
    if (nouveauStatut === 'Envoyé' || !nouveauStatut) {
      this.consommationService.envoyerConsommation(consommation.id).subscribe({
        next: () => {
          this.notificationService.showSuccess(
            `Consommation #${consommation.id} envoyée avec succès`,
            'Envoi confirmé'
          );
          this.loadConsommations();

          if (this.selectedConsommation && this.selectedConsommation.id === consommation.id) {
            this.selectedConsommation.statut = 'Envoyé';
          }

          this.fermerModalConfirmationEnvoi();
          this.fermerModalDetails();
        },
        error: (err) => {
          console.error('Erreur envoi consommation:', err);
          let errorMessage = 'Erreur lors de l\'envoi de la consommation';
          if (err.error && err.error.message) {
            errorMessage = err.error.message;
          }
          this.notificationService.showError(errorMessage, 'Erreur d\'envoi');
          this.fermerModalConfirmationEnvoi();
        }
      });
    }
  }

  deleteConsommation(consommation: IConsommation) {
    if (!this.canDeleteConsommations()) {
      this.notificationService.showError(
        'Vous n\'avez pas les permissions nécessaires pour supprimer une consommation',
        'Accès refusé'
      );
      return;
    }

    if (consommation.statut === 'Envoyé') {
      this.notificationService.showError(
        'Impossible de supprimer une consommation envoyée',
        'Suppression interdite'
      );
      return;
    }

    this.consommationEnCoursDeSuppression = consommation;
    this.ouvrirModalConfirmationSuppression();
  }
  private ouvrirModalBootstrap(modalId: string) {
    const modalElement = document.getElementById(modalId);
    if (modalElement) {
      if (typeof (window as any).$ !== 'undefined') {
        (window as any).$(`#${modalId}`).modal('show');
      } else {
        const bsModal = new (window as any).bootstrap.Modal(modalElement);
        bsModal.show();
      }
    }
  }
  private fermerModalBootstrap(modalId: string) {
    const modalElement = document.getElementById(modalId);
    if (modalElement) {
      if (typeof (window as any).$ !== 'undefined') {
        (window as any).$(`#${modalId}`).modal('hide');
      } else {
        const bsModal = (window as any).bootstrap.Modal.getInstance(modalElement);
        if (bsModal) {
          bsModal.hide();
        }
      }
    }
  }
  ouvrirModalConfirmationEnvoi() {
    this.cdr.detectChanges();
    setTimeout(() => {
      this.ouvrirModalBootstrap('confirmSendModal');
    }, 100);
  }

  ouvrirModalConfirmationSuppression() {
    this.cdr.detectChanges();
    setTimeout(() => {
      this.ouvrirModalBootstrap('confirmDeleteModal');
    }, 100);
  }

  envoyerConsommationConfirmed() {
    if (this.consommationEnCoursDEnvoi) {
      const originalConsommation = { ...this.consommationEnCoursDEnvoi };
      this.procederEnvoiConsommation(originalConsommation, 'Envoyé');
    }
  }

  suppressionConsommationConfirmed() {
    if (this.consommationEnCoursDeSuppression) {
      this.consommationService.deleteConsommation(this.consommationEnCoursDeSuppression.id).subscribe({
        next: () => {
          this.notificationService.showSuccess(
            `Consommation #${this.consommationEnCoursDeSuppression!.id} supprimée avec succès`,
            'Suppression confirmée'
          );
          this.loadConsommations();
          this.fermerModalConfirmationSuppression();
        },
        error: (err) => {
          console.error('Erreur suppression consommation:', err);
          let errorMessage = 'Erreur lors de la suppression de la consommation';
          if (err.error && err.error.message) {
            errorMessage = err.error.message;
          }
          this.notificationService.showError(errorMessage, 'Erreur');
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

  fermerModalDetails() {
    this.fermerModalBootstrap('detailsModal');
    this.selectedConsommation = null;
  }

  fermerModalConfirmationEnvoi() {
    this.fermerModalBootstrap('confirmSendModal');
    this.consommationEnCoursDEnvoi = null;
  }

  fermerModalConfirmationSuppression() {
    this.fermerModalBootstrap('confirmDeleteModal');
    this.consommationEnCoursDeSuppression = null;
  }

  onTableDataChange(event: any) {
    this.page = event;
    this.loadConsommations();
  }

  sortColumn(column: string) {
    if (this.sort.field === column) {
      this.sort.direction = this.sort.direction === 'asc' ? 'desc' : 'asc';
    } else {
      this.sort.field = column;
      this.sort.direction = 'asc';
    }
    this.sortService.sortColumn(this.listConsommations, column);
  }




  ouvrirModalUpdateAvecDelay(consommation: IConsommation) {
    if (!this.canModifyConsommations()) {
      this.notificationService.showError('Modification non autorisée', 'Permissions insuffisantes');
      return;
    }

    if (consommation.statut === 'Envoyé') {
      this.notificationService.showError('Consommation envoyée non modifiable', 'Action interdite');
      return;
    }

    this.consommationToUpdate = { ...consommation };

    if (this.consommationToUpdate.dateConsommation) {
      const date = new Date(this.consommationToUpdate.dateConsommation);
      if (!isNaN(date.getTime())) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        this.consommationToUpdate.dateConsommation = `${year}-${month}-${day}`;
      }
    }

    const currentAffaire = this.createCurrentAffaire(consommation);
    this.selectedAffaire = currentAffaire;

    this.loadAvailableAffairesForUpdateOptimized().then(() => {
      this.ensureCurrentAffaireInList(currentAffaire);
      this.checkIfConsommationHasLines(consommation.id!);
      this.showUpdateModal = true;
      this.cdr.detectChanges();

      // Utiliser Bootstrap pour ouvrir le modal au lieu de la gestion manuelle
      setTimeout(() => {
        const modalElement = document.getElementById('updateModal');
        if (modalElement) {
          // Utiliser jQuery ou Bootstrap natif
          if (typeof (window as any).$ !== 'undefined') {
            (window as any).$('#updateModal').modal('show');
          } else {
            const bsModal = new (window as any).bootstrap.Modal(modalElement);
            bsModal.show();
          }
        }
      }, 100);
    });
  }
  fermerModalUpdate() {
    this.fermerModalBootstrap('updateModal');

    this.consommationToUpdate = null;
    this.selectedAffaire = null;
    this.availableAffaires = [];
    this.consommationHasLines = false;
    this.checkingLines = false;
    this.showUpdateModal = false;

    this.cdr.detectChanges();
  }

  private loadAvailableAffaires() {
    if (this.isAdminOrManager()) {
      this.availableAffaires = Array.from(this.affairesMap.values());
    } else {
      this.loadUserSpecificAffaires();
    }
  }

  private isAdminOrManager(): boolean {
    if (!this.currentUser?.roles) return false;
    const roles = Array.isArray(this.currentUser.roles) ? this.currentUser.roles : [this.currentUser.roles];
    return roles.some((role: any) => {
      const roleName = typeof role === 'string' ? role : role.name;
      return roleName === 'ADMIN' || roleName === 'MANAGER';
    });
  }

  private loadUserSpecificAffaires() {
    this.affaireService.getCurrentAccessorAffaires().subscribe({
      next: (data) => {
        if (data && data.length > 0) {
          this.availableAffaires = this.normalizeAffaires(data);
        } else {
          this.loadAffairesByUserId();
        }
      },
      error: (err) => {
        console.warn("getCurrentAccessorAffaires échoué:", err);
        this.loadAffairesByUserId();
      }
    });
  }
  debugModalState() {
    console.log('=== DEBUG MODAL STATE ===');
    console.log('consommationToUpdate:', this.consommationToUpdate);
    console.log('selectedAffaire:', this.selectedAffaire);
    console.log('availableAffaires:', this.availableAffaires);
    console.log('consommationHasLines:', this.consommationHasLines);
    console.log('checkingLines:', this.checkingLines);
    console.log('========================');
  }
  private loadAffairesByUserId() {
    if (this.currentUser?.id) {
      this.affaireService.getAccessorAffaires(this.currentUser.id).subscribe({
        next: (data) => {
          if (data && data.length > 0) {
            this.availableAffaires = this.normalizeAffaires(data);
          } else {
            this.loadAndFilterAllAffaires();
          }
        },
        error: (err) => {
          console.warn("getAccessorAffaires échoué:", err);
          this.loadAndFilterAllAffaires();
        }
      });
    } else {
      this.loadAndFilterAllAffaires();
    }
  }

  private loadAndFilterAllAffaires() {
    this.affaireService.getAffaires().subscribe({
      next: (data) => {
        const allAffaires = this.normalizeAffaires(data || []);

        if (this.currentUser?.affaires && Array.isArray(this.currentUser.affaires)) {
          const userAffaireCodes = this.extractUserAffaireCodes(this.currentUser.affaires);

          if (userAffaireCodes.length > 0) {
            this.availableAffaires = allAffaires.filter(affaire =>
              userAffaireCodes.includes(affaire.code) ||
              userAffaireCodes.includes(affaire.affaire)
            );
          } else {
            this.availableAffaires = [];
          }
        } else {
          this.availableAffaires = [];
        }
      },
      error: (err) => {
        console.error("Erreur chargement affaires pour filtrage:", err);
        this.availableAffaires = [];
      }
    });
  }

  private extractUserAffaireCodes(userAffaires: any[]): string[] {
    if (!Array.isArray(userAffaires) || userAffaires.length === 0) {
      return [];
    }

    const firstItem = userAffaires[0];

    if (typeof firstItem === 'string') {
      return userAffaires;
    } else if (typeof firstItem === 'object' && firstItem !== null) {
      if (firstItem.code) {
        return userAffaires.map(aff => aff.code);
      } else if (firstItem.affaire) {
        return userAffaires.map(aff => aff.affaire);
      } else if (firstItem.id) {
        return userAffaires.map(aff => aff.id.toString());
      }
    }

    return [];
  }
// Nouvelle méthode pour créer l'objet affaire actuelle
  private createCurrentAffaire(consommation: IConsommation): any {
    // D'abord chercher dans le service partagé
    const affaireFromService = this.sharedAffaireService.getAffaireByCode(consommation.affaireCode!);

    if (affaireFromService) {
      return affaireFromService;
    }

    // Si pas trouvé, créer un objet temporaire avec les données de la consommation
    return this.sharedAffaireService.createAffaireFromConsommation(consommation);
  }


// Nouvelle méthode pour s'assurer que l'affaire actuelle est dans la liste
  private ensureCurrentAffaireInList(currentAffaire: any) {
    // Vérifier si l'affaire actuelle est déjà dans availableAffaires
    const existsInList = this.availableAffaires.some(affaire =>
      (affaire.code === currentAffaire.code) ||
      (affaire.affaire === currentAffaire.affaire)
    );

    // Si elle n'y est pas, l'ajouter au début de la liste
    if (!existsInList) {
      this.availableAffaires.unshift(currentAffaire);
    }

    // Mettre à jour selectedAffaire pour pointer vers l'affaire dans la liste
    this.selectedAffaire = this.availableAffaires.find(affaire =>
      (affaire.code === currentAffaire.code) ||
      (affaire.affaire === currentAffaire.affaire)
    ) || currentAffaire;
  }
  private normalizeAffaires(affaires: any[]): Affaire[] {
    return affaires.map(affaire => {
      const normalized = {
        id: affaire.id || affaire.numero,
        code: affaire.code || affaire.affaire,
        affaire: affaire.affaire || affaire.code,
        nom: affaire.nom || affaire.libelle,
        libelle: affaire.libelle || affaire.nom,
        ...affaire
      };

      // Ajouter une propriété displayLabel pour l'affichage
      normalized.displayLabel = `${normalized.code || normalized.affaire} - ${normalized.libelle || normalized.nom}`;

      return normalized;
    });
  }
  private loadAvailableAffairesForUpdate(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.isAdminOrManager()) {
        this.availableAffaires = Array.from(this.affairesMap.values());
        resolve();
      } else {
        this.loadUserSpecificAffairesAsync().then(() => {
          resolve();
        }).catch(error => {
          console.error('Erreur chargement affaires:', error);
          this.availableAffaires = [];
          resolve(); // On résout quand même pour ne pas bloquer
        });
      }
    });
  }
  private loadAvailableAffairesForUpdateOptimized(): Promise<void> {
    return new Promise((resolve, reject) => {
      // CORRECTION: Utiliser le service partagé au lieu de this.affairesMap
      this.sharedAffaireService.getAffairesForUser(this.currentUser).subscribe({
        next: (affaires) => {
          this.availableAffaires = affaires;
          console.log(`Affaires chargées pour modification: ${affaires.length} affaires`);
          resolve();
        },
        error: (error) => {
          console.error('Erreur chargement affaires pour modification:', error);
          this.availableAffaires = [];
          resolve(); // On résout quand même pour ne pas bloquer
        }
      });
    });
  }
  forceReloadAffaires() {
    this.sharedAffaireService.forceReload();
    this.loadAffaires();
  }
  private loadUserSpecificAffairesAsync(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.affaireService.getCurrentAccessorAffaires().subscribe({
        next: (data) => {
          if (data && data.length > 0) {
            this.availableAffaires = this.normalizeAffaires(data);
            resolve();
          } else {
            this.loadAffairesByUserIdAsync().then(resolve).catch(reject);
          }
        },
        error: (err) => {
          console.warn("getCurrentAccessorAffaires échoué:", err);
          this.loadAffairesByUserIdAsync().then(resolve).catch(reject);
        }
      });
    });
  }

  private loadAffairesByUserIdAsync(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.currentUser?.id) {
        this.affaireService.getAccessorAffaires(this.currentUser.id).subscribe({
          next: (data) => {
            if (data && data.length > 0) {
              this.availableAffaires = this.normalizeAffaires(data);
              resolve();
            } else {
              this.loadAndFilterAllAffairesAsync().then(resolve).catch(reject);
            }
          },
          error: (err) => {
            console.warn("getAccessorAffaires échoué:", err);
            this.loadAndFilterAllAffairesAsync().then(resolve).catch(reject);
          }
        });
      } else {
        this.loadAndFilterAllAffairesAsync().then(resolve).catch(reject);
      }
    });
  }

  private loadAndFilterAllAffairesAsync(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.affaireService.getAffaires().subscribe({
        next: (data) => {
          const allAffaires = this.normalizeAffaires(data || []);

          if (this.currentUser?.affaires && Array.isArray(this.currentUser.affaires)) {
            const userAffaireCodes = this.extractUserAffaireCodes(this.currentUser.affaires);

            if (userAffaireCodes.length > 0) {
              this.availableAffaires = allAffaires.filter(affaire =>
                userAffaireCodes.includes(affaire.code) ||
                userAffaireCodes.includes(affaire.affaire)
              );
            } else {
              this.availableAffaires = [];
            }
          } else {
            this.availableAffaires = [];
          }
          resolve();
        },
        error: (err) => {
          console.error("Erreur chargement affaires pour filtrage:", err);
          this.availableAffaires = [];
          resolve(); // On résout pour ne pas bloquer
        }
      });
    });
  }

  onAffaireChangeUpdate(selectedAffaire: any) {
    console.log('Changement affaire:', selectedAffaire);

    if (this.consommationHasLines) {
      this.notificationService.showWarning(
        'Impossible de changer l\'affaire car cette consommation contient déjà des articles',
        'Modification limitée'
      );
      // Remettre l'affaire précédente
      setTimeout(() => {
        this.selectedAffaire = this.availableAffaires.find(affaire =>
          affaire.code === this.consommationToUpdate?.affaireCode
        );
        this.cdr.detectChanges();
      }, 100);
      return;
    }

    if (selectedAffaire && this.consommationToUpdate) {
      this.selectedAffaire = selectedAffaire;
      this.consommationToUpdate.affaireCode = selectedAffaire.code || selectedAffaire.affaire;
      this.consommationToUpdate.affaireId = selectedAffaire.id || this.generateNumericId(selectedAffaire.code || selectedAffaire.affaire);

      console.log('Affaire mise à jour:', this.selectedAffaire);
    }

    this.cdr.detectChanges();
  }


  private generateNumericId(affaireCode: string): number | undefined {
    if (!affaireCode) return undefined;

    const numericPart = affaireCode.replace(/[^0-9]/g, '');
    if (numericPart) {
      return parseInt(numericPart, 10);
    }

    let hash = 0;
    for (let i = 0; i < affaireCode.length; i++) {
      const char = affaireCode.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash;
    }
    return Math.abs(hash);
  }
  private checkIfConsommationHasLines(consommationId: number) {
    this.checkingLines = true;
    this.consommationHasLines = false;

    this.ligneConsommationService.getLignesConsommationByConsommationId(consommationId).subscribe({
      next: (lignes) => {
        this.consommationHasLines = lignes && lignes.length > 0;
        this.checkingLines = false;
      },
      error: (error) => {
        console.error('Erreur lors de la vérification des lignes:', error);
        this.consommationHasLines = false; // En cas d'erreur, on permet la modification
        this.checkingLines = false;
      }
    });
  }
// Dans votre méthode updateConsommationInfo(), remplacez cette partie :
  updateConsommationInfo() {
    console.log('Tentative de mise à jour:', {
      consommationToUpdate: this.consommationToUpdate,
      selectedAffaire: this.selectedAffaire
    });

    if (!this.consommationToUpdate) {
      this.notificationService.showError('Données de consommation manquantes', 'Erreur');
      return;
    }

    if (!this.selectedAffaire) {
      this.notificationService.showError('Veuillez sélectionner une affaire', 'Formulaire incomplet');
      return;
    }

    if (!this.consommationToUpdate.dateConsommation) {
      this.notificationService.showError('Veuillez sélectionner une date', 'Formulaire incomplet');
      return;
    }

    let dateConsommation = this.consommationToUpdate.dateConsommation;

    // CORRECTION : Convertir la date au format LocalDateTime attendu par le backend
    if (typeof dateConsommation === 'string' && dateConsommation.length === 10) {
      // Le backend attend un LocalDateTime, donc ajouter l'heure
      // Utiliser midi pour éviter les problèmes de fuseau horaire
      dateConsommation = dateConsommation + 'T12:00:00';

      // Alternative si le backend attend un ISO string complet :
      // const [year, month, day] = dateConsommation.split('-').map(Number);
      // const localDate = new Date(year, month - 1, day, 12, 0, 0);
      // dateConsommation = localDate.toISOString();
    }

    const updateData = {
      affaireId: this.selectedAffaire.code || this.selectedAffaire.affaire,
      dateConsommation: dateConsommation,
      statut: this.consommationToUpdate.statut || 'Brouillon',
      userLogin: this.consommationToUpdate.userLogin
    };

    console.log('Données à envoyer:', updateData);

    const consommationId = this.consommationToUpdate.id;

    this.consommationService.updateConsommation(consommationId!, updateData).subscribe({
      next: (response) => {
        this.notificationService.showSuccess(
          `Consommation #${consommationId} modifiée avec succès`,
          'Modification réussie'
        );
        this.loadConsommations();
        this.fermerModalUpdate();
      },
      error: (error) => {
        console.error('Erreur modification consommation:', error);
        let errorMessage = 'Erreur lors de la modification';
        if (error?.error?.message) {
          errorMessage = error.error.message;
        }
        this.notificationService.showError(errorMessage, 'Erreur');
      }
    });
  }
  isUpdateFormValid(): boolean {
    return !!(this.consommationToUpdate &&
      this.selectedAffaire &&
      this.consommationToUpdate.dateConsommation &&
      !this.isLoadingUser &&
      !this.checkingLines);
  }
  ngAfterViewInit() {
    // Nettoyer les backdrops orphelins
    const orphanBackdrops = document.querySelectorAll('.modal-backdrop');
    orphanBackdrops.forEach(backdrop => backdrop.remove());
  }
}
