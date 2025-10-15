import {Component, Input, OnChanges, OnInit, SimpleChanges, ViewChild} from '@angular/core';
import { Output } from '@angular/core';
import { EventEmitter } from '@angular/core';
import { ChangeDetectorRef } from '@angular/core';
import { UtilisateurServiceService } from 'src/app/services/utilisateur-service.service';
import { Iuser } from 'src/interfaces/iuser';
import {Subscription, forkJoin, of} from "rxjs";
import {AffectationUserAccessComponent} from "../affectation-user-access/affectation-user-access.component";
import {AffectationUserRoleComponent} from "../affectation-user-role/affectation-user-role.component";
import {AccessService} from "../../../services/access.service";
import {RoleService} from "../../../services/role.service";
import { Access } from 'src/interfaces/iaccess';
import { Role } from 'src/interfaces/irole';
import {SortService} from "../../../services/sort.service";
import {catchError} from "rxjs/operators";

declare var $: any; // Pour utiliser jQuery/Bootstrap

@Component({
  selector: 'app-list-utilisateurs',
  templateUrl: './list-utilisateurs.component.html',
  styleUrls: ['./list-utilisateurs.component.css' ]
})
export class ListUtilisateursComponent implements OnInit, OnChanges{
  @ViewChild(AffectationUserAccessComponent) affectationChild!: AffectationUserAccessComponent;
  @ViewChild(AffectationUserRoleComponent) affectationRoleChild!: AffectationUserRoleComponent;

  @Output() ajoutEffectue = new EventEmitter<void>();
  showModal = false;
  openDropdownIndex: number | null = null;
  selectedRowIndex: number | null = null;
  isCollapsed = false;
  isFullscreen = false;
  sort = { field: '', direction: 'asc' as 'asc' | 'desc' };
  // État de chargement
  isLoading: boolean = false;

  editingIdAgelio: boolean = false;
  editingUserId: number | null = null;
  tempIdAgelio: string = ''

  private subscriptions: Subscription = new Subscription();

  private userAccessorMap: Map<number, number> = new Map();

  // listeRoles: {id: string, name: string}[] = [
  //   { id: 'admin', name: 'Admin' },
  //   { id: 'utilisateur', name: 'Utilisateur' },
  //   { id: 'direction', name: 'Direction' },
  //   { id: 'consulteur', name: 'Consulteur' }
  // ]
  listeRoles: Role[] = [];

  listeStatus: {id: string, name: string}[] = [
    { id: 'actif', name: 'Actif' },
    { id: 'inactif', name: 'Inactif' }
  ]

  listeAccess: Access[] = [];
  listeRolesComplete: Role[] = [];
  nouvelAccess = { code: '', description: '' };
  nouveauRole = { name: '', description: '' };

  selectedRole!: string;
  selectedStatus!: string;
  users! :Iuser[];
  pfiltre: any;

  //for pagination
  page: number = 1;
  count: number = 0;
  tableSize: number = 6;
  tableSizes: any = [5, 10, 15, 20];
  userSelected!: Iuser;

  accessorSelected: any;

  constructor(
    private cdr: ChangeDetectorRef,
    private utilisateurServicd: UtilisateurServiceService,
    private sortService:SortService,
    private accessService: AccessService,
    private roleService: RoleService
  ) {}

  postList(): void {
    this.utilisateurServicd.getAllUsers().subscribe(users => {
      this.users = users;
      // ✅ NOUVEAU: Charger les mappings User -> Accessor
      this.loadUserAccessorMappings();
      console.log(this.users);
    });
  }

  private loadUserAccessorMappings(): void {
    if (!this.users || this.users.length === 0) return;

    const mappingObservables = this.users.map(user =>
      this.utilisateurServicd.getAccessorIdByUserId(user.id).pipe(
        catchError(error => {
          console.warn(`Could not load accessor for user ${user.id}:`, error);
          return of(null);
        })
      )
    );

    // Remplacez forkJoin par combineLatest ou traitez individuellement
    mappingObservables.forEach((obs, index) => {
      obs.subscribe({
        next: (accessorId) => {
          if (accessorId !== null) {
            this.userAccessorMap.set(this.users[index].id, accessorId);
          }
        },
        error: (error) => {
          console.warn(`Error loading accessor for user ${this.users[index].id}:`, error);
        }
      });
    });
  }

  private getAccessorIdForUser(userId: number): number | null {
    return this.userAccessorMap.get(userId) || null;
  }

  toggleCollapse() {
    this.isCollapsed = !this.isCollapsed;
  }

  toggleFullscreen() {
    this.isFullscreen = !this.isFullscreen;
  }

  ngOnInit(): void {
    this.postList();
    this.loadRoles();
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.postList();
  }
  loadRoles() {
    this.roleService.getAllRoles().subscribe({
      next: result => {
        this.listeRoles = result;
        console.log("Liste des roles disponible est : ", this.listeRoles)
      }
    })
  }
  onTableDataChange(event: any) {
    this.page = event;
    this.postList();
  }

  private normalizeBoolean(value: any): boolean {
    if (typeof value === 'boolean') return value;
    if (typeof value === 'number') return value !== 0;
    if (typeof value === 'string') {
      const v = value.toLowerCase().trim();
      // ✅ CORRECTION IMPORTANTE : traiter explicitement "false" comme false
      if (v === 'false' || v === '0' || v === 'non' || v === 'inactif') return false;
      if (v === 'true' || v === '1' || v === 'oui' || v === 'actif') return true;
      return false;
    }
    return !!value;
  }

  protected isUserActive(user: Iuser): boolean {
    // user.statut can be boolean, number, or string; normalize to boolean
    // Prefer boolean if present on user
    // @ts-ignore
    return this.normalizeBoolean(user.statut);
  }

  // getFilteredUsers(): Iuser[] {
  //   if (!this.users) return [];
  //
  //   let filteredUsers = [...this.users];
  //
  //   if (this.selectedStatus) {
  //     const wantActive = this.selectedStatus === 'actif';
  //     filteredUsers = filteredUsers.filter(user => {
  //       const isActive = this.isUserActive(user);
  //       console.log(`User ${user.nom}: statut=${user.statut}, isActive=${isActive}, wantActive=${wantActive}`);
  //       return isActive === wantActive;
  //     });
  //   }
  //
  //   return filteredUsers;
  // }
  getFilteredUsers(): Iuser[] {
    if (!this.users) return [];
    if (!this.selectedStatus && !this.selectedRole) {
      return this.users;
    }
    return this.users.filter(u => {
      if (this.selectedRole) {
        const hasRole = (u.roles || []).some(
          r => r.name.toLowerCase() === this.selectedRole.toLowerCase()
        );
        if (!hasRole) return false;
      }
      if (this.selectedStatus) {
        const wantActive = this.normalizeBoolean(this.selectedStatus);
        if (this.isUserActive(u) !== wantActive) return false;
      }
      return true;
    });
  }

  isDropdownOpen(index: number): boolean {
    return this.openDropdownIndex === index;
  }


  affectAccess(user: any) {
    this.userSelected = user;
    if (this.affectationChild) {
      this.affectationChild.resetModal();
    }
    this.showModal = true;
    this.cdr.detectChanges();
    setTimeout(() => {
      const modalBtn = document.querySelector('[data-target="#affectAccessModal"]') as HTMLElement;
      if (modalBtn) {
        modalBtn.click();
      }
    }, 100);
    this.closeDropdown();
  }

  affectRole(user: any) {
    this.userSelected = user;
    this.showModal = true;
    this.cdr.detectChanges();

    setTimeout(() => {
      if (this.affectationRoleChild) {
        this.affectationRoleChild.resetModal();
      }
      const modalBtn = document.querySelector('[data-target="#affectRoleModal"]') as HTMLElement;
      if (modalBtn) {
        modalBtn.click();
      }
    }, 150);

    this.closeDropdown();
  }


  openGestionAccess() {
    this.chargerTousLesAccess();
    setTimeout(() => {
      $('#gestionAccessModal').modal('show');
    }, 100);
  }

  openGestionRoles() {
    this.chargerTousLesRoles();
    setTimeout(() => {
      $('#gestionRolesModal').modal('show');
    }, 100);
  }

  openGestionLiaisons() {
    setTimeout(() => {
      $('#gestionLiaisonsModal').modal('show');
    }, 100);
  }


  chargerTousLesAccess() {
    this.accessService.getAllAccess().subscribe({
      next: (data) => {
        this.listeAccess = data;
        console.log('Accès chargés:', this.listeAccess);
      },
      error: (err) => console.error('Erreur chargement accès:', err)
    });
  }

  chargerTousLesRoles() {
    this.roleService.getAllRoles().subscribe({
      next: (data) => {
        this.listeRolesComplete = data;
        console.log('Rôles chargés:', this.listeRolesComplete);
      },
      error: (err) => console.error('Erreur chargement rôles:', err)
    });
  }

  ajouterAccess() {
    if (!this.nouvelAccess.code.trim()) {
      alert('Le code est obligatoire');
      return;
    }

    this.accessService.createAccess(this.nouvelAccess).subscribe({
      next: (response) => {
        console.log('Accès créé:', response);
        this.nouvelAccess = { code: '', description: '' }; // Reset form
        this.chargerTousLesAccess(); // Recharger la liste
        alert('Accès ajouté avec succès');
      },
      error: (err) => {
        console.error('Erreur création accès:', err);
        alert('Erreur lors de la création de l\'accès');
      }
    });
  }

  ajouterRole() {
    if (!this.nouveauRole.name.trim()) {
      alert('Le nom est obligatoire');
      return;
    }

    this.roleService.createRole(this.nouveauRole).subscribe({
      next: (response) => {
        console.log('Rôle créé:', response);
        this.nouveauRole = { name: '', description: '' }; // Reset form
        this.chargerTousLesRoles(); // Recharger la liste
        alert('Rôle ajouté avec succès');
      },
      error: (err) => {
        console.error('Erreur création rôle:', err);
        alert('Erreur lors de la création du rôle');
      }
    });
  }

  supprimerAccess(accessId: number | undefined) {
    if (confirm('Êtes-vous sûr de vouloir supprimer cet accès ?')) {
      this.accessService.deleteAccess(accessId).subscribe({
        next: (response) => {
          console.log('Accès supprimé:', response);
          this.chargerTousLesAccess(); // Recharger la liste
          alert('Accès supprimé avec succès');
        },
        error: (err) => {
          console.error('Erreur suppression accès:', err);
          alert('Erreur lors de la suppression de l\'accès');
        }
      });
    }
  }

  supprimerRole(roleId: number) {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce rôle ?')) {
      this.roleService.deleteRole(roleId).subscribe({
        next: (response) => {
          console.log('Rôle supprimé:', response);
          this.chargerTousLesRoles(); // Recharger la liste
          alert('Rôle supprimé avec succès');
        },
        error: (err) => {
          console.error('Erreur suppression rôle:', err);
          alert('Erreur lors de la suppression du rôle');
        }
      });
    }
  }

  activerUtilisateur(user: Iuser) {
    this.isLoading = true;

    this.utilisateurServicd.activateUser(user.id).subscribe({
      next: (response) => {
        user.statut = "true";
        this.closeDropdown();
        this.isLoading = false;

        this.cdr.detectChanges();

        console.log('Utilisateur activé, nouveau statut:', user.statut);
      },
      error: (err) => {
        console.error('Erreur lors de l\'activation:', err);
        this.isLoading = false;
      }
    });
  }

  desactiverUtilisateur(user: Iuser) {
    this.isLoading = true;

    this.utilisateurServicd.deactivateUser(user.id).subscribe({
      next: (response) => {
        user.statut = "false";
        this.closeDropdown();
        this.isLoading = false;

        // ✅ Forcer la mise à jour de l'affichage
        this.cdr.detectChanges();

        console.log('Utilisateur désactivé, nouveau statut:', user.statut);
      },
      error: (err) => {
        console.error('Erreur lors de la désactivation:', err);
        this.isLoading = false;
      }
    });
  }

  sortColumn(column: string) {
    if (this.sort.field === column) {
      // même colonne → on inverse la direction
      this.sort.direction = this.sort.direction === 'asc' ? 'desc' : 'asc';
    } else {
      // nouvelle colonne → tri ascendant par défaut
      this.sort.field = column;
      this.sort.direction = 'asc';
    }
    this.sortService.sortColumn(this.users, column);
  }

  toggleDropdown(event: Event, index: number): void {
    event.stopPropagation();

    // Sélectionner la ligne
    this.selectedRowIndex = index;

    // Si le même dropdown est déjà ouvert, le fermer
    if (this.openDropdownIndex === index) {
      this.openDropdownIndex = null;
      this.selectedRowIndex = null;
    } else {
      // Fermer tous les autres et ouvrir celui-ci
      this.openDropdownIndex = index;
    }
  }

  private closeDropdown(): void {
    this.openDropdownIndex = null;
    this.selectedRowIndex = null;
  }

  editIdAgelio(user: Iuser) {
    this.userSelected = user;
    this.tempIdAgelio = user.idAgelio || '';
    this.closeDropdown();

    setTimeout(() => {
      $('#gestionIdAgelioModal').modal('show');

      // Focus sur le champ après ouverture du modal
      setTimeout(() => {
        const input = document.querySelector('#modalIdAgelio') as HTMLInputElement;
        if (input) {
          input.focus();
          input.select();
        }
      }, 300);
    }, 100);
  }

  updateIdAgelioFromModal() {
    if (!this.userSelected) return;

    this.isLoading = true;

    this.utilisateurServicd.updateUserIdAgelio(this.userSelected.id, this.tempIdAgelio.trim()).subscribe({
      next: (response) => {
        // Mettre à jour l'utilisateur dans la liste
        const user = this.users.find(u => u.id === this.userSelected.id);
        if (user) {
          user.idAgelio = this.tempIdAgelio.trim();
        }
        this.userSelected.idAgelio = this.tempIdAgelio.trim();

        this.isLoading = false;

        // Fermer le modal
        $('#gestionIdAgelioModal').modal('hide');

        // Message de succès
        this.showSuccessMessage('ID Agelio mis à jour avec succès');

        console.log('ID Agelio mis à jour avec succès');
      },
      error: (err) => {
        console.error('Erreur lors de la mise à jour de l\'ID Agelio:', err);
        this.showErrorMessage('Erreur lors de la mise à jour de l\'ID Agelio: ' + (err.error?.message || err.message));
        this.isLoading = false;
      }
    });
  }


  clearIdAgelio() {
    this.tempIdAgelio = '';

    setTimeout(() => {
      const input = document.querySelector('#modalIdAgelio') as HTMLInputElement;
      if (input) {
        input.focus();
      }
    }, 100);
  }

  private showSuccessMessage(message: string) {
    alert(message);

  }

  private showErrorMessage(message: string) {
    alert(message);

  }

  hasAccessorLink(userId: number): boolean {
    return this.userAccessorMap.has(userId);
  }

  getAccessorIdDisplay(userId: number): string {
    const accessorId = this.userAccessorMap.get(userId);
    return accessorId ? accessorId.toString() : 'Non lié';
  }


  affectAffaire(user: Iuser) {
    this.userSelected = user;
    console.log('affectAffaire called for user:', user);

    this.utilisateurServicd.getAccessorIdByUserId(user.id).subscribe({
      next: (accessorId) => {
        console.log('Accessor ID récupéré pour user', user.id, ':', accessorId);

        if (accessorId !== null && accessorId !== undefined) {
          this.utilisateurServicd.getAccessorById(accessorId).subscribe({
            next: (accessor) => {
              console.log('Accessor trouvé:', accessor);
              this.accessorSelected = accessor;

              // IMPORTANT: S'assurer que les données sont bien définies avant d'ouvrir le modal
              this.cdr.detectChanges();

              // Vérifier que les inputs sont bien passés
              console.log('Données à passer au modal:', {
                userSelected: this.userSelected,
                accessorSelected: this.accessorSelected
              });

              this.showModal = true;
              this.cdr.detectChanges();

              setTimeout(() => {
                const modalBtn = document.querySelector('[data-target="#affectAffaireModal"]') as HTMLElement;
                if (modalBtn) {
                  modalBtn.click();
                }
              }, 100);
              this.closeDropdown();
            },
            error: (error) => {
              console.error('Erreur lors de la récupération de l\'accessor:', error);
              alert('Erreur lors de la récupération des données accessor.');
            }
          });
        } else {
          console.warn('Aucun accessor trouvé pour user', user.id);
          alert('Aucun accessor lié à cet utilisateur. Veuillez d\'abord effectuer la liaison.');
        }
      },
      error: (error) => {
        console.error('Erreur lors de la récupération de l\'ID accessor:', error);
        alert('Erreur lors de la récupération des informations de liaison.');
      }
    });
  }

   lierUtilisateurAccessor(userId: number, accessorId: number) {
    this.utilisateurServicd.linkUserToAccessor(userId, accessorId).subscribe({
      next: (response) => {
        this.userAccessorMap.set(userId, accessorId);
        alert('Liaison créée avec succès');
        console.log('Liaison User-Accessor créée:', response);
      },
      error: (error) => {
        console.error('Erreur lors de la liaison:', error);
        alert('Erreur lors de la création de la liaison');
      }
    });
  }

  delierUtilisateurAccessor(userId: number) {
    this.utilisateurServicd.unlinkUserFromAccessor(userId).subscribe({
      next: (response) => {
        this.userAccessorMap.delete(userId);
        alert('Liaison supprimée avec succès');
        console.log('Liaison User-Accessor supprimée:', response);
      },
      error: (error) => {
        console.error('Erreur lors de la suppression de la liaison:', error);
        alert('Erreur lors de la suppression de la liaison');
      }
    });
  }
}
