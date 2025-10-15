import {Component, ElementRef, Input, OnInit, ViewChild} from '@angular/core';
import {Role} from "../../../../interfaces/irole";
import {RoleService} from "../../../services/role.service";
import {UtilisateurServiceService} from "../../../services/utilisateur-service.service";
import {Iuser} from "../../../../interfaces/iuser";

type RoleSelectable = Role & {
  selected?: boolean;
  fromDatabase?: boolean;
  toBeDeleted?: boolean;
};

@Component({
  selector: 'app-affectation-user-role',
  templateUrl: './affectation-user-role.component.html',
  styleUrls: ['./affectation-user-role.component.css']
})
export class AffectationUserRoleComponent implements OnInit{
  @Input() userSelected!: Iuser;
  listRole: RoleSelectable[] = [];
  listUserRole: RoleSelectable[] = [];
  @ViewChild('roleModal', { static: true }) modalElement!: ElementRef;

  constructor(
    private roleService: RoleService,
    private utilisateurService: UtilisateurServiceService
  ) {}

  ngOnInit(): void {
    if (this.modalElement?.nativeElement) {
      this.modalElement.nativeElement.addEventListener('hidden.bs.modal', () => {
        this.refreshLists();
      });
    }
    this.initializeData();
  }

  // Méthode de tracking pour optimiser les performances *ngFor
  trackByRoleId(index: number, role: RoleSelectable): any {
    return role?.id || index;
  }

  // Vérifier s'il y a des rôles disponibles sélectionnés
  hasSelectedAvailableRoles(): boolean {
    return this.listRole.some(role => role.selected);
  }

  // Vérifier s'il y a des rôles attribués sélectionnés
  hasSelectedAssignedRoles(): boolean {
    return this.listUserRole.some(role => role.selected && !role.toBeDeleted);
  }

  // Vérifier s'il y a des changements à sauvegarder
  hasChangesToSave(): boolean {
    const hasNewRoles = this.listUserRole.some(role => !role.fromDatabase && !role.toBeDeleted);
    const hasRolesToDelete = this.listUserRole.some(role => role.toBeDeleted);
    return hasNewRoles || hasRolesToDelete;
  }

  private initializeData(): void {
    if (!this.userSelected?.id) {
      return;
    }
    this.loadUserRoles().then(() => {
      this.loadAvailableRoles();
    });
  }

  private loadAvailableRoles(): void {
    this.roleService.getAllRoles().subscribe({
      next: response => {
        const roles = this.extractDataFromResponse(response);
        if (roles) {
          this.updateAvailableRoles(roles);
        }
      },
      error: err => {
        this.listRole = [];
      }
    });
  }

  private loadUserRoles(): Promise<void> {
    return new Promise((resolve) => {
      if (!this.userSelected?.id) {
        this.listUserRole = [];
        resolve();
        return;
      }

      this.utilisateurService.getUserRoles(this.userSelected.id).subscribe({
        next: response => {
          const userRoles = this.extractDataFromResponse(response);
          if (userRoles) {
            this.listUserRole = userRoles.map(role => ({
              ...role,
              selected: false,
              fromDatabase: true,
              toBeDeleted: false
            }));
          } else {
            this.listUserRole = [];
          }
          resolve();
        },
        error: err => {
          this.listUserRole = [];
          resolve();
        }
      });
    });
  }

  private extractDataFromResponse(response: any): any[] | null {
    if (!response) {
      return null;
    }

    // Si c'est directement un tableau
    if (Array.isArray(response)) {
      return response;
    }

    // Si c'est un objet avec propriété data
    if (response.data && Array.isArray(response.data)) {
      return response.data;
    }

    // Si c'est un objet avec d'autres propriétés possibles
    if (typeof response === 'object') {
      const possibleArrayKeys = ['roles', 'items', 'results', 'content'];
      for (const key of possibleArrayKeys) {
        if (response[key] && Array.isArray(response[key])) {
          return response[key];
        }
      }
    }

    return null;
  }

  private updateAvailableRoles(allRoles: any[]): void {
    const userRoleIds = new Set(this.listUserRole.map(r => r.id));
    this.listRole = allRoles
      .filter(role => role && role.id && !userRoleIds.has(role.id))
      .map(role => ({
        ...role,
        selected: false,
        fromDatabase: false,
        toBeDeleted: false
      }));
  }

  attribuerRole(): void {
    const selectedRoles = this.listRole.filter(r => r.selected);
    if (selectedRoles.length === 0) {
      return;
    }

    selectedRoles.forEach(role => {
      this.listUserRole.push({
        ...role,
        selected: false,
        fromDatabase: false,
        toBeDeleted: false
      });
    });

    this.listRole = this.listRole.filter(r => !r.selected);
  }

  retirerRole(): void {
    const selectedRoles = this.listUserRole.filter(r => r.selected);
    if (selectedRoles.length === 0) {
      return;
    }

    selectedRoles.forEach(role => {
      if (role.fromDatabase) {
        // Marquer pour suppression mais garder dans la liste des rôles attribués
        role.toBeDeleted = true;
        role.selected = false;
      } else {
        // Remettre dans les rôles disponibles
        const existingRole = this.listRole.find(r => r.id === role.id);
        if (!existingRole) {
          this.listRole.push({
            ...role,
            selected: false,
            fromDatabase: false,
            toBeDeleted: false
          });
        }
        // Retirer de la liste des rôles attribués
        this.listUserRole = this.listUserRole.filter(r => r.id !== role.id);
      }
    });
  }

  refreshLists(): void {
    this.initializeData();
  }

  public resetModal(): void {
    this.refreshLists();
  }

  OnValideAffectation(): void {
    if (!this.userSelected?.id) {
      return;
    }

    // Gérer les nouveaux rôles à ajouter
    const nouveauxRoles = this.listUserRole.filter(r => !r.fromDatabase && !r.toBeDeleted);

    // Gérer les rôles à supprimer
    const rolesASupprimer = this.listUserRole.filter(r => r.toBeDeleted);

    let operationsCount = nouveauxRoles.length + rolesASupprimer.length;

    if (operationsCount === 0) {
      return;
    }

    // Ajouter les nouveaux rôles
    nouveauxRoles.forEach(role => {
      this.utilisateurService.addRoleToUser(this.userSelected.id, role.id).subscribe({
        next: () => {
          role.fromDatabase = true;
          role.toBeDeleted = false;
          operationsCount--;
          if (operationsCount === 0) {
            this.refreshLists();
          }
        },
        error: err => {
          operationsCount--;
          if (operationsCount === 0) {
            this.refreshLists();
          }
        }
      });
    });

    // Supprimer les rôles marqués
    rolesASupprimer.forEach(role => {
      this.utilisateurService.removeRoleFromUser(this.userSelected.id, role.id).subscribe({
        next: () => {
          operationsCount--;
          if (operationsCount === 0) {
            this.refreshLists();
          }
        },
        error: err => {
          operationsCount--;
          if (operationsCount === 0) {
            this.refreshLists();
          }
        }
      });
    });
  }

  onTableDataChange($event: number): void {
    this.loadAvailableRoles();
  }
}
