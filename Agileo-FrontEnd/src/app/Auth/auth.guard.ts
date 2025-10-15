import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot
} from '@angular/router';
import { KeycloakAuthGuard, KeycloakService } from 'keycloak-angular';
import { UserProfileService } from '../services/user-profile.service';
import {UserData} from '../../interfaces/iuser'



@Injectable({
  providedIn: 'root'
})
export class AuthGuard extends KeycloakAuthGuard {

  private currentUser: UserData | null = null;

  constructor(
    protected override readonly router: Router,
    protected readonly keycloak: KeycloakService,
    private userProfileService: UserProfileService
  ) {
    super(router, keycloak);
  }

  public async isAccessAllowed(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Promise<boolean> {
    // Vérifier d'abord l'authentification Keycloak
    if (!this.authenticated) {
      await this.keycloak.login({
        redirectUri: window.location.origin + state.url
      });
      return false;
    }

    // Charger les données utilisateur du backend
    try {
      await this.loadBackendUserData();
    } catch (error) {
      console.error('Erreur lors du chargement des données utilisateur:', error);
      // Continuer avec les rôles Keycloak seulement
    }

    // Récupérer les rôles requis et les accès depuis la route
    const requiredRoles = route.data['roles'] || [];
    const requiredAccess = route.data['access'] || [];

    // Si aucun rôle/accès requis, autoriser l'accès
    if (requiredRoles.length === 0 && requiredAccess.length === 0) {
      return true;
    }

    // Vérifier les permissions
    return this.hasRequiredPermissions(requiredRoles, requiredAccess);
  }

  private async loadBackendUserData(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.userProfileService.getCurrentUser().subscribe({
        next: (backendUser) => {
          this.currentUser = {
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
            acces: backendUser.acces ?
              backendUser.acces.map((access: any) => access.code) : []
          };

          console.log('Backend user data loaded for AuthGuard:', this.currentUser);
          resolve();
        },
        error: (error) => {
          console.error('Erreur chargement données backend:', error);
          reject(error);
        }
      });
    });
  }

  private hasRequiredPermissions(requiredRoles: string[], requiredAccess: string[]): boolean {
    // Vérifier les rôles backend en priorité
    if (this.currentUser && this.currentUser.roles) {
      // Normaliser la casse pour la comparaison
      const userRoles = this.currentUser.roles.map(role => role.toUpperCase());
      const normalizedRequiredRoles = requiredRoles.map(role => role.toUpperCase());

      const hasBackendRole = normalizedRequiredRoles.some(role =>
        userRoles.includes(role)
      );

      if (hasBackendRole) {
        console.log('Access granted via backend role:', this.currentUser.roles);
        return true;
      }
    }

    // Vérifier les accès backend
    if (this.currentUser && this.currentUser.acces && requiredAccess.length > 0) {
      // Normaliser la casse pour la comparaison
      const userAccess = this.currentUser.acces.map(access => access.toLowerCase());
      const normalizedRequiredAccess = requiredAccess.map(access => access.toLowerCase());

      const hasBackendAccess = normalizedRequiredAccess.some(access =>
        userAccess.includes(access)
      );

      if (hasBackendAccess) {
        console.log('Access granted via backend access:', this.currentUser.acces);
        return true;
      }
    }

    // Fallback : vérifier les rôles Keycloak
    if (requiredRoles.length > 0) {
      const normalizedKeycloakRoles = this.roles.map(role => role.toUpperCase());
      const normalizedRequiredRoles = requiredRoles.map(role => role.toUpperCase());

      const hasKeycloakRole = normalizedRequiredRoles.some(role =>
        normalizedKeycloakRoles.includes(role)
      );

      if (hasKeycloakRole) {
        console.log('Access granted via Keycloak role:', this.roles);
        return true;
      }
    }

    // Rôles spéciaux qui donnent accès à tout
    const superAdminRoles = ['ADMIN', 'CONSULTEUR'];
    if (this.currentUser && this.currentUser.roles) {
      const userRoles = this.currentUser.roles.map(role => role.toUpperCase());
      const isSuperAdmin = superAdminRoles.some(role =>
        userRoles.includes(role)
      );

      if (isSuperAdmin) {
        console.log('Access granted via super admin role:', this.currentUser.roles);
        return true;
      }
    }

    console.log('Access denied. Required:', { requiredRoles, requiredAccess });
    console.log('User has:', {
      backendRoles: this.currentUser?.roles,
      backendAccess: this.currentUser?.acces,
      keycloakRoles: this.roles
    });

    return false;
  }

  // Méthodes utilitaires pour vérifier les permissions dans les composants
  public hasRole(roleName: string): boolean {
    if (this.currentUser?.roles) {
      const userRoles = this.currentUser.roles.map(role => role.toUpperCase());
      return userRoles.includes(roleName.toUpperCase());
    }

    const normalizedKeycloakRoles = this.roles.map(role => role.toUpperCase());
    return normalizedKeycloakRoles.includes(roleName.toUpperCase());
  }

  public hasAccess(accessCode: string): boolean {
    if (this.currentUser?.acces) {
      const userAccess = this.currentUser.acces.map(access => access.toLowerCase());
      return userAccess.includes(accessCode.toLowerCase());
    }
    return false;
  }

  public getCurrentUser(): UserData | null {
    return this.currentUser;
  }

  // Méthode pour rafraîchir les données utilisateur
  public async refreshUserData(): Promise<void> {
    try {
      await this.loadBackendUserData();
    } catch (error) {
      console.error('Erreur lors du rafraîchissement des données utilisateur:', error);
    }
  }
}
