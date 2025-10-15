import { Component, OnInit } from '@angular/core';

import { Chart, ChartConfiguration, registerables } from 'chart.js';
import {DashboardService} from "../../services/dashboard.service";


Chart.register(...registerables);

interface DashboardStats {
  statistiquesGenerales: {
    totalAffaires: number;
    totalUtilisateurs: number;
    totalFournisseurs: number;
    totalArticles: number;
  };
  demandesAchat: {
    total: number;
    brouillon: number;
    envoye: number;
    recu: number;
    rejete: number;
    tauxValidation: number;
    dernierMois: number;
    evolutionPourcentage: number;
  };
  consommations: {
    total: number;
    envoye: number;
    brouillon: number;
    dernierMois: number;
    lignesTotal: number;
  };
  commandes: {
    total: number;
    enCours: number;
    livrees: number;
    dernierMois: number;
    valeurTotale: number;
  };
  stock: {
    articlesEnStock: number;
    articlesEpuises: number;
    articlesStockFaible: number;
    valeurStock: number;
    tauxRotation: number;
  };
  receptions: {
    total: number;
    enAttente: number;
    integrees: number;
    dernierMois: number;
    lignesEnAttente: number;
  };
  articlesPopulaires: Array<{
    ref: string;
    designation: string;
    nombreDemandes: number;
    quantiteTotale: number;
    unite: string;
  }>;
  evolutionDemandes: Array<{
    date: string;
    demandes: number;
    consommations: number;
    receptions: number;
  }>;
  affaires?: Array<{
    code: string;
    libelle: string;
    demandesEnCours: number;
    commandesEnCours: number;
    budget: number;
    depense: number;
    pourcentageUtilise: number;
  }>;
}
@Component({
  selector: 'app-acceuil',
  templateUrl: './acceuil.component.html',
  styleUrls: ['./acceuil.component.css']
})
export class AcceuilComponent implements OnInit {

  stats: DashboardStats | null = null;
  loading = true;
  isAdministrateur = false;
  userRole: string = '';
  accessorId: number = 0;
  // Charts
  evolutionChart: Chart | null = null;
  demandesChart: Chart | null = null;
  stockChart: Chart | null = null;
  currentDate = new Date();
  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.userRole = localStorage.getItem('userRole') || '';
    this.accessorId = parseInt(localStorage.getItem('accessorId') || '0');
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    const userRole = localStorage.getItem('userRole');
    this.isAdministrateur = userRole === 'ADMIN';

    this.dashboardService.getDashboardStats().subscribe({
      next: (data) => {
        this.stats = data;
        this.loading = false;
        setTimeout(() => this.initCharts(), 100);
      },
      error: (error) => {
        console.error('Erreur lors du chargement du dashboard:', error);
        this.loading = false;
      }
    });
  }

  initCharts(): void {
    if (!this.stats) return;

    this.createEvolutionChart();
    this.createDemandesChart();
    this.createStockChart();
  }

  createEvolutionChart(): void {
    const canvas = document.getElementById('evolutionChart') as HTMLCanvasElement;
    if (!canvas || !this.stats) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    if (this.evolutionChart) {
      this.evolutionChart.destroy();
    }

    const config: ChartConfiguration = {
      type: 'line',
      data: {
        labels: this.stats.evolutionDemandes.map(e => e.date),
        datasets: [
          {
            label: 'Demandes',
            data: this.stats.evolutionDemandes.map(e => e.demandes),
            borderColor: '#3b82f6',
            backgroundColor: 'rgba(59, 130, 246, 0.1)',
            tension: 0.4,
            fill: true
          },
          {
            label: 'Consommations',
            data: this.stats.evolutionDemandes.map(e => e.consommations),
            borderColor: '#10b981',
            backgroundColor: 'rgba(16, 185, 129, 0.1)',
            tension: 0.4,
            fill: true
          },
          {
            label: 'Réceptions',
            data: this.stats.evolutionDemandes.map(e => e.receptions),
            borderColor: '#f59e0b',
            backgroundColor: 'rgba(245, 158, 11, 0.1)',
            tension: 0.4,
            fill: true
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: true,
            position: 'top'
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              precision: 0
            }
          }
        }
      }
    };

    this.evolutionChart = new Chart(ctx, config);
  }

  createDemandesChart(): void {
    const canvas = document.getElementById('demandesChart') as HTMLCanvasElement;
    if (!canvas || !this.stats) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    if (this.demandesChart) {
      this.demandesChart.destroy();
    }

    const config: ChartConfiguration = {
      type: 'doughnut',
      data: {
        labels: ['Brouillon', 'Envoyé', 'Reçu', 'Rejeté'],
        datasets: [{
          data: [
            this.stats.demandesAchat.brouillon,
            this.stats.demandesAchat.envoye,
            this.stats.demandesAchat.recu,
            this.stats.demandesAchat.rejete
          ],
          backgroundColor: [
            '#94a3b8',
            '#3b82f6',
            '#10b981',
            '#ef4444'
          ]
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom'
          }
        }
      }
    };

    this.demandesChart = new Chart(ctx, config);
  }

  createStockChart(): void {
    const canvas = document.getElementById('stockChart') as HTMLCanvasElement;
    if (!canvas || !this.stats) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    if (this.stockChart) {
      this.stockChart.destroy();
    }

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: ['En stock', 'Stock faible', 'Épuisés'],
        datasets: [{
          label: 'Nombre d\'articles',
          data: [
            this.stats.stock.articlesEnStock,
            this.stats.stock.articlesStockFaible,
            this.stats.stock.articlesEpuises
          ],
          backgroundColor: [
            '#10b981',
            '#f59e0b',
            '#ef4444'
          ]
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              precision: 0
            }
          }
        },
        plugins: {
          legend: {
            display: false
          }
        }
      }
    };

    this.stockChart = new Chart(ctx, config);
  }

  ngOnDestroy(): void {
    if (this.evolutionChart) this.evolutionChart.destroy();
    if (this.demandesChart) this.demandesChart.destroy();
    if (this.stockChart) this.stockChart.destroy();
  }
  // Méthodes de contrôle d'accès
  canAccessConsommations(): boolean {
    // Logique pour déterminer si l'utilisateur peut accéder aux consommations
    // Adaptez selon vos besoins
    return this.userRole === 'ADMIN' ||
      this.userRole === 'CHEF_PROJET' ||
      this.userRole === 'MAGASINIER';
  }

  canAccessDemandesAchat(): boolean {
    return this.userRole === 'ADMIN' ||
      this.userRole === 'CHEF_PROJET' ||
      this.userRole === 'USER';
  }

  canAccessCommandes(): boolean {
    return this.userRole === 'ADMIN' ||
      this.userRole === 'CHEF_PROJET';
  }

  canAccessReceptions(): boolean {
    return this.userRole === 'ADMIN' ||
      this.userRole === 'MAGASINIER';
  }

  canAccessStock(): boolean {
    return this.userRole === 'ADMIN' ||
      this.userRole === 'MAGASINIER';
  }

  canAccessAffaires(): boolean {
    return this.userRole === 'ADMIN' ||
      this.userRole === 'CHEF_PROJET';
  }

  isAdmin(): boolean {
    return this.userRole === 'ADMIN';
  }

/*

  currentUser: UserData | null = null;
  isLoading = false;
  private userSubscription!: Subscription;
  constructor(
    private userProfileService: UserProfileService,
    private keycloakService: KeycloakService
  ) { }
  ngOnInit(): void {
    this.loadUserData();  // ← AJOUTEZ CETTE LIGNE
  }

  ngOnDestroy(): void {
    if (this.userSubscription) {
      this.userSubscription.unsubscribe();
    }
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
  public hasRole(roleName: string): boolean {
    if (!this.currentUser || !this.currentUser.roles) return false;
    const userRoles = this.currentUser.roles.map(role => role.toUpperCase());
    const targetRole = roleName.toUpperCase();
    return userRoles.includes(targetRole);
  }
  canAccessConsommations(): boolean {
    console.log(this.currentUser)
    if (!this.currentUser) return false;
    return this.hasRole('ADMIN') ||
      this.hasRole('CONSULTEUR') ||
      this.hasRole('CHEF DE PROJET') ||
      this.hasAccess('consommation');
  }

  canAccessReceptions(): boolean {
    if (!this.currentUser) return false;
    return this.hasRole('ADMIN') ||
      this.hasRole('CONSULTEUR') ||
      this.hasRole('CHEF DE PROJET') ||
      this.hasAccess('reception');
  }

  canAccessDemandesAchat(): boolean {
    if (!this.currentUser) return false;
    return this.hasRole('ADMIN') ||
      this.hasRole('CONSULTEUR') ||
      this.hasRole('CHEF DE PROJET') ||
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
  public hasAccess(accessCode: string): boolean {
    if (!this.currentUser || !this.currentUser.acces) return false;
    const userAccess = this.currentUser.acces.map(access => access.toLowerCase());
    const targetAccess = accessCode.toLowerCase();

    return userAccess.includes(targetAccess);
  }
*/

}
