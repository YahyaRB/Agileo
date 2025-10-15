import { Component, OnInit } from '@angular/core';
import { DashboardService, ConsommationStatsDTO, DaStatsDTO } from "../../services/dashboard.service";
import {UserProfileService} from "../../services/user-profile.service";
import {Subscription} from "rxjs";
import {AffaireServiceService} from "../../services/affaire-service.service";
import {UtilisateurServiceService} from "../../services/utilisateur-service.service";

export interface UserData {
  id?: number;
  email?: string;
  matricule?: string;
  nom?: string;
  prenom?: string;
  statut?: string;
  login?: string;
  roles?: string[];
  affaires?:string[];
  acces?: string[];
  idAgelio?: string;
  keycloakUser?: {
    id: string;
    username: string;
    firstName: string;
    lastName: string;
    email: string;
    emailVerified: boolean;
  }
}
@Component({
  selector: 'app-right-tab',
  templateUrl: './right-tab.component.html',
  styleUrls: ['./right-tab.component.css']
})
export class RightTabComponent implements OnInit {
    ngOnInit(): void {
        throw new Error('Method not implemented.');
    }
/*  private subscriptions: Subscription = new Subscription();
  // Propriétés pour l'utilisateur et les données
  currentUser: UserData | null = null;
  userRoles: string[] = [];
  isChefDeProjet = false;
  isMagasinier = false;
  isAutresRoles = false;
  countDA:number;
  countConsommation:number;
  countReception:number
  // Modèles de données réelles
  consommationStats: ConsommationStatsDTO ;
  daStats: DaStatsDTO ;
  nbAffaires:number;

  constructor(
    private dashboardService: DashboardService,
    private userProfileService: UserProfileService,
    private affaireService:AffaireServiceService,
    private utilisateurService:UtilisateurServiceService
  ) {
    this.dashboardService.getTotalConsommationStatsByUser().subscribe({
      next: (stats) => this.countConsommation = stats,
      error: (err) => console.error('Erreur lors du chargement des stats :', err)
    });
    this.dashboardService.getTotalDaStatsByUser().subscribe({
      next: (stats) => this.countDA = stats,
      error: (err) => console.error('Erreur lors du chargement des stats :', err)
    });
  this.dashboardService.getTotalReceptionStatsByUser().subscribe({
    next: (stats) => this.countReception = stats,
  error: (err) => console.error('Erreur lors du chargement des stats :', err)
});

this.affaireService.getAffaires().subscribe(data=>
this.nbAffaires=data.length
)
  }

  ngOnInit(): void {
    // Récupérer l'utilisateur depuis le localStorage ou un service d'authentification
    this.loadCurrentUser();

    if (this.currentUser) {

      this.userRoles = this.currentUser.roles;
      this.checkRoles();
      this.loadDashboardData();
    }
  }

  /!**
   * Charge l'utilisateur connecté
   * TODO: Remplacer par votre service d'authentification réel
   *!/
  loadCurrentUser() {
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
          roles: user.roles
        };

        // ✅ Déplacez toute la logique ICI
        if (this.currentUser) {
          this.userRoles = this.currentUser.roles || [];
          this.checkRoles();
          this.loadDashboardData();
        }
      },
      error: (error) => {
        console.error('Erreur chargement utilisateur:', error);
      }
    });
  }

  /!**
   * Détermine les rôles de l'utilisateur connecté
   *!/
  checkRoles(): void {
let roles=[];
this.utilisateurService.getUserRoles(this.currentUser.id).subscribe(data=>
roles=data
)
    this.isChefDeProjet = roles.includes('CHEF DE PROJET');
    this.isMagasinier = roles.includes('MAGASINIER');

    // Si l'utilisateur a d'autres rôles que les deux précédents (ADMIN, etc.)
    this.isAutresRoles = roles.some(role =>
      role !== 'CHEF DE PROJET' && role !== 'MAGASINIER' && role !== 'UTILISATEUR'
    );
  }

  /!**
   * Charge les données du tableau de bord en fonction du rôle
   *!/
  loadDashboardData(): void {

    const login = this.currentUser?.login;

    const isGlobal =  this.isAutresRoles; // Magasinier et autres voient tout
    const isUserSpecific = this.isChefDeProjet || this.isMagasinier; // Chef de projet voit ses affaires/DA

    // Si on est Chef de Projet, on utilise la version filtrée (login)
    if (isUserSpecific && login) {
      alert('specifique')
      this.dashboardService.getConsommationStatsByLogin(login).subscribe({
        next: (stats) => this.consommationStats = stats,
        error: (err) => console.error('Erreur lors du chargement des stats de consommation:', err)
      });

      this.dashboardService.getDaStatsByLogin(login).subscribe({
        next: (stats) => this.daStats = stats,
        error: (err) => console.error('Erreur lors du chargement des stats DA:', err)
      });
    }
    // Si on est Magasinier ou autres rôles (Admin), on utilise la version globale
    else if (isGlobal) {

      this.dashboardService.getConsommationStatsGlobal().subscribe({
        next: (stats) => this.consommationStats = stats,
        error: (err) => console.error('Erreur lors du chargement des stats de consommation globales:', err)
      });

      this.dashboardService.getDaStatsGlobal().subscribe({
        next: (stats) => this.daStats = stats,
        error: (err) => console.error('Erreur lors du chargement des stats DA globales:', err)
      });
    }

    // Les 'CONSOMMATEUR' sans rôle spécifique ne voient que leurs propres DA
    else if (login) {
      this.dashboardService.getConsommationStatsByLogin(login).subscribe({
        next: (stats) => this.consommationStats = stats,
        error: (err) => console.error('Erreur lors du chargement des stats de consommation:', err)
      });

      this.dashboardService.getDaStatsByLogin(login).subscribe({
        next: (stats) => this.daStats = stats,
        error: (err) => console.error('Erreur lors du chargement des stats DA:', err)
      });
    }
  }*/
}
