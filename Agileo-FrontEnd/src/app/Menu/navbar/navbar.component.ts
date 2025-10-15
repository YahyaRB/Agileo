import {Component, OnDestroy, OnInit} from '@angular/core';
import {KeycloakProfile} from "keycloak-js";
import {filter, Subscription} from "rxjs";
import {KeycloakService} from "keycloak-angular";
import {NavigationEnd, Router} from "@angular/router";
import {UserProfileService} from "../../services/user-profile.service";
import {NotificationService} from "../../services/notification.service";

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit, OnDestroy {
  profile?: KeycloakProfile;
  currentUser: any;
  currentLanguage: string = 'fr';
  currentRoute: string = '';
  imageLoadError: boolean = false; // Pour gérer les erreurs de chargement d'image

  private subscriptions: Subscription = new Subscription();

  constructor(
    public keycloak: KeycloakService,
    private router: Router,
    private userProfileService: UserProfileService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.initializeComponent();
    this.loadCurrentUser();
    this.subscribeToRouteChanges();
    this.loadLanguagePreference();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  // =============== MÉTHODES D'INITIALISATION ===============

  private async initializeComponent(): Promise<void> {
    if (await this.keycloak.isLoggedIn()) {
      try {
        this.profile = await this.keycloak.loadUserProfile();
        console.log("Utilisateur authentifié:", this.profile);
      } catch (error) {
        console.error("Erreur lors du chargement du profil:", error);
      }
    }
  }

  private loadCurrentUser(): void {
    const userSub = this.userProfileService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUser = {
          ...user,
          roles: user.roles ?
            Array.from(user.roles).map((role: any) =>
              typeof role === 'string' ? role : role.name
            ) : []
        };
        // Réinitialiser l'état d'erreur d'image quand on charge un nouvel utilisateur
        this.imageLoadError = false;
      },
      error: (error) => {
        console.error("Erreur lors du chargement des données utilisateur:", error);
      }
    });
    this.subscriptions.add(userSub);
  }

  private subscribeToRouteChanges(): void {
    const routeSub = this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.currentRoute = event.url;
      });
    this.subscriptions.add(routeSub);
  }


  private loadLanguagePreference(): void {
    const savedLanguage = localStorage.getItem('user-language');
    if (savedLanguage) {
      this.currentLanguage = savedLanguage;
    }
  }

  // =============== MÉTHODES DE NAVIGATION ===============

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }

  goToSettings(): void {
    this.router.navigate(['/profile'], { fragment: 'settings' });
  }

  logout(): void {
    this.keycloak.logout(window.location.origin);
  }

  // =============== MÉTHODES DE LANGUE ===============

  changeLanguage(lang: string): void {
    this.currentLanguage = lang;
    localStorage.setItem('user-language', lang);
    // Implémenter le changement de langue avec votre service de traduction
    // this.translateService.use(lang);
    console.log('Langue changée vers:', lang);
  }

  getCurrentLanguage(): string {
    const languages: { [key: string]: string } = {
      'fr': 'FR',
      'en': 'EN',
      'ar': 'AR'
    };
    return languages[this.currentLanguage] || 'FR';
  }

  // =============== MÉTHODES UTILISATEUR ===============

  get displayName(): string {
    if (this.currentUser?.prenom && this.currentUser?.nom) {
      return `${this.currentUser.prenom} ${this.currentUser.nom}`;
    }
    if (this.profile?.firstName && this.profile?.lastName) {
      return `${this.profile.firstName} ${this.profile.lastName}`;
    }
    return this.profile?.username || 'Utilisateur';
  }

  get displayNamesmall(): string {
    if (this.currentUser?.prenom && this.currentUser?.nom) {
      return `${this.currentUser.login.toUpperCase()}`;
    }
    if (this.profile?.firstName && this.profile?.lastName) {
      return `${this.profile.username?.toUpperCase() || 'UTILISATEUR'}`;
    }
    return this.profile?.username?.toUpperCase() || 'UTILISATEUR';
  }

  getUserEmail(): string {
    return this.currentUser?.email || this.profile?.email || 'email@exemple.com';
  }

  getUserPrimaryRole(): string {
    if (this.currentUser?.roles && this.currentUser.roles.length > 0) {
      return this.currentUser.roles[0];
    }
    return 'Utilisateur';
  }

  // =============== MÉTHODES AVATAR ===============

  /**
   * Vérifie si l'utilisateur a une image de profil
   */
  hasUserImage(): boolean {
    if (this.imageLoadError) {
      return false;
    }

    // Vérifier si l'utilisateur a une URL d'image dans ses données
    const imageUrl = this.getUserImageUrl();
    return !!(imageUrl && imageUrl.trim() !== ''); // Forcer le type boolean avec !!
  }


  /**
   * Récupère l'URL de l'image de profil de l'utilisateur
   */
  getUserImageUrl(): string {
    // Priorité à l'image du currentUser
    if (this.currentUser?.imageUrl) {
      return this.currentUser.imageUrl;
    }

    // Puis à l'image du profil Keycloak
    if (this.profile?.attributes && 'picture' in this.profile.attributes) {
      const picture = this.profile.attributes['picture'];
      if (Array.isArray(picture) && picture.length > 0) {
        return picture[0];
      }
    }

    // Si aucune image n'est disponible, retourner une chaîne vide
    return '';
  }

  /**
   * Génère les initiales de l'utilisateur pour l'avatar par défaut
   */
  getUserInitials(): string {
    let initials = '';

    if (this.currentUser?.prenom && this.currentUser?.nom) {
      initials = `${this.currentUser.prenom.charAt(0)}${this.currentUser.nom.charAt(0)}`;
    } else if (this.profile?.firstName && this.profile?.lastName) {
      initials = `${this.profile.firstName.charAt(0)}${this.profile.lastName.charAt(0)}`;
    } else if (this.profile?.username) {
      const username = this.profile.username;
      if (username.length >= 2) {
        initials = username.substring(0, 2);
      } else {
        initials = username.charAt(0);
      }
    } else {
      initials = 'U'; // Par défaut "User"
    }

    return initials.toUpperCase();
  }

  /**
   * Génère une couleur d'arrière-plan basée sur le nom de l'utilisateur
   */
  getUserAvatarColor(): string {
    const name = this.displayName.toLowerCase();
    const colors = [
      'linear-gradient(45deg, #4e73df, #224abe)',
      'linear-gradient(45deg, #1cc88a, #13855c)',
      'linear-gradient(45deg, #f6c23e, #dda20a)',
      'linear-gradient(45deg, #e74a3b, #c0392b)',
      'linear-gradient(45deg, #9b59b6, #8e44ad)',
      'linear-gradient(45deg, #3498db, #2980b9)',
      'linear-gradient(45deg, #e67e22, #d35400)',
      'linear-gradient(45deg, #95a5a6, #7f8c8d)',
    ];

    // Utiliser la somme des codes de caractères pour déterminer la couleur
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash += name.charCodeAt(i);
    }

    return colors[hash % colors.length];
  }

  /**
   * Gère les erreurs de chargement d'image
   */
  onImageError(event: any): void {
    console.log('Erreur de chargement de l\'image de profil, utilisation de l\'avatar par défaut');
    this.imageLoadError = true;
    // Masquer l'élément img qui a échoué
    event.target.style.display = 'none';
  }

  // =============== MÉTHODES UTILITAIRES ===============

  getPageTitle(): string {
    const routeTitles: { [key: string]: string } = {
      '/Acceuil': 'Tableau de Bord',
      '/Utilisateurs': 'Gestion des Utilisateurs',
      '/Affaires': 'Gestion des Affaires',
      '/demandes-achat': 'Demandes d\'Achat',
      '/receptions': 'Réceptions',
      '/consommations': 'Consommations',
      '/profile': 'Mon Profil'
    };

    for (const route in routeTitles) {
      if (this.currentRoute.includes(route)) {
        return routeTitles[route];
      }
    }
    return 'Tableau de Bord';
  }

  getCurrentSection(): string {
    if (this.currentRoute.includes('/Utilisateurs')) return 'Administration';
    if (this.currentRoute.includes('/Affaires')) return 'Gestion';
    if (this.currentRoute.includes('/demandes-achat')) return 'Achats';
    if (this.currentRoute.includes('/receptions')) return 'Logistique';
    if (this.currentRoute.includes('/consommations')) return 'Production';
    return 'Système';
  }

  refreshPage(): void {
    window.location.reload();
  }

  toggleRightSidebar(): void {
    // Déclencher l'affichage/masquage du sidebar droit
    const rightSidebar = document.getElementById('rightsidebar');
    if (rightSidebar) {
      rightSidebar.classList.toggle('active');
    }
  }
}
