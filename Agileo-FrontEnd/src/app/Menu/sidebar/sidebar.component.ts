import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { UserProfileService } from '../../services/user-profile.service';
import { KeycloakService } from 'keycloak-angular';
import { UserData } from "../../../interfaces/iuser";

type KeycloakUser = NonNullable<UserData['keycloakUser']>;

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent  implements OnInit, OnDestroy {

  private userSubscription!: Subscription;
  currentUser: UserData | null = null;
  isLoading = true;
  isCollapsed = false;
  isHidden = true;
  constructor(
    private userProfileService: UserProfileService,
    private keycloakService: KeycloakService
  ) { }

  ngOnInit(): void {
    this.loadUserData();
    window.addEventListener('sidebarToggle', () => {
      this.isCollapsed = !this.isCollapsed;
    });
  }

  ngOnDestroy(): void {
    if (this.userSubscription) {
      this.userSubscription.unsubscribe();
    }
    window.removeEventListener('sidebarToggle', () => {});
  }

  private loadUserData(): void {
    this.isLoading = true;

    this.userSubscription = this.userProfileService.getCurrentUser().subscribe({
      next: async (backendUser) => {
        const keycloakUser = await this.getKeycloakUserData();

        const userData: UserData = {
          id: backendUser.id,
          email: backendUser.email,
          matricule: backendUser.matricule,
          nom: backendUser.nom,
          prenom: backendUser.prenom,
          statut: backendUser.statut,
          login: backendUser.login,
          roles: backendUser.roles ?
            Array.from(backendUser.roles).map((role: any) =>
              typeof role === 'string' ? role : role.name
            ) : [],
          acces: backendUser.acces ? backendUser.acces.map((access: any) => access.code) : [],
          keycloakUser: keycloakUser
        };

        this.currentUser = userData;
        this.isLoading = false;

        console.log('Sidebar - Backend user data loaded:', {
          roles: this.currentUser.roles,
          acces: this.currentUser.acces,
          keycloak: this.currentUser.keycloakUser
        });
      },
      error: async (error) => {
        console.error('Erreur lors du chargement des données utilisateur:', error);
        this.isLoading = false;

        const keycloakData = await this.getKeycloakUserData();
        if (keycloakData) {
          const roles = await this.getKeycloakRoles();
          this.currentUser = {
            keycloakUser: keycloakData,
            roles: roles,
            acces: []
          };
          console.log('Sidebar - Fallback to Keycloak data:', this.currentUser);
        }
      }
    });
  }


  private async getKeycloakUserData(): Promise<any> {
    try {
      const isLoggedIn = await this.keycloakService.isLoggedIn();
      if (isLoggedIn) {
        const keycloakInstance = this.keycloakService.getKeycloakInstance();
        const tokenParsed = keycloakInstance.tokenParsed;

        if (tokenParsed) {
          return {
            id: tokenParsed['sub'],
            username: tokenParsed['preferred_username'] || tokenParsed['name'],
            firstName: tokenParsed['given_name'],
            lastName: tokenParsed['family_name'],
            email: tokenParsed['email'],
            emailVerified: tokenParsed['email_verified'] || false
          };
        }
      }

      return null;
    } catch (error) {
      return null;
    }
  }

  private async getKeycloakRoles(): Promise<string[]> {
    try {
      const isLoggedIn = await this.keycloakService.isLoggedIn();
      if (isLoggedIn) {
        // Récupérer les rôles client et realm
        const realmRoles = this.keycloakService.getUserRoles() || [];
        const clientRoles = this.keycloakService.getUserRoles(true) || [];

        const allRoles = [...realmRoles, ...clientRoles];
        return allRoles;
      }
      return [];
    } catch (error) {
      console.error(error);
      return [];
    }
  }

  // =============== MÉTHODES DE VÉRIFICATION DES PERMISSIONS ===============
  public hasRole(roleName: string): boolean {
    if (!this.currentUser || !this.currentUser.roles) return false;
    const userRoles = this.currentUser.roles.map(role => role.toUpperCase());
    const targetRole = roleName.toUpperCase();
    return userRoles.includes(targetRole);
  }

  public hasAccess(accessCode: string): boolean {
    if (!this.currentUser || !this.currentUser.acces) return false;
    const userAccess = this.currentUser.acces.map(access => access.toLowerCase());
    const targetAccess = accessCode.toLowerCase();

    return userAccess.includes(targetAccess);
  }

  canAccessConsommations(): boolean {
    if (!this.currentUser) return false;
    return this.hasRole('ADMIN') ||
      this.hasRole('CONSULTEUR') ||
      this.hasAccess('consommation');
  }

  canAccessReceptions(): boolean {
    if (!this.currentUser) return false;
    return this.hasRole('ADMIN') ||
      this.hasRole('CONSULTEUR') ||
      this.hasAccess('reception');
  }

  canAccessDemandesAchat(): boolean {

    if (!this.currentUser) return false;
    return this.hasRole('ADMIN') ||
      this.hasRole('CONSULTEUR') ||
      this.hasAccess('demande achat');
  }

  canAccessUtilisateurs(): boolean {
    if (!this.currentUser) return false;
    return this.hasRole('ADMIN') ||
      this.hasRole('CONSULTEUR') ||
      this.hasRole('MANAGER');
  }

  canAccessAdminSection(): boolean {
    if (!this.currentUser) return false;
    return this.hasRole('ADMIN') ||
      this.hasRole('CONSULTEUR');
  }

}
