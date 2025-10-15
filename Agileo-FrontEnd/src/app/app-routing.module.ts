import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AcceuilComponent } from "./Components/acceuil/acceuil.component";
import { AuthGuard } from "./Auth/auth.guard";
import { ListUtilisateursComponent } from './Components/utilisateurs/list-utilisateurs/list-utilisateurs.component';
import { ListeAffairesComponent } from './Components/affaires/liste-affaires/liste-affaires.component';
import { ListReceptionsComponent } from './Components/receptions/list-receptions/list-receptions.component';
import { LigneReceptionComponent } from './Components/receptions/ligne-receptions/ligne-reception.component';
import { AffaireDemandeAchatComponent } from "./Components/affaires/affaire-demande-achat/affaire-demande-achat.component";
import { AffaireReceptionComponent } from "./Components/affaires/affaire-reception/affaire-reception.component";
import { AffaireConsommationComponent } from "./Components/affaires/affaire-consommation/affaire-consommation.component";
import { ListDemandeAchatComponent } from "./Components/demande-achat/list-demande-achat/list-demande-achat.component";
import { LigneDemandeComponent } from "./Components/demande-achat/ligne-demande/ligne-demande.component";
import { ListConsommationComponent } from "./Components/consommations/list-consommation/list-consommation.component";
import { LigneConsommationComponent } from "./Components/consommations/ligne-consommation/ligne-consommation.component";

const routes: Routes = [
  // Route par défaut - Accueil
  {
    path: '',
    component: AcceuilComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'Acceuil',
    component: AcceuilComponent
  },

  // ===================== GESTION DES UTILISATEURS =====================
  {
    path: 'Utilisateurs',
    component: ListUtilisateursComponent,
    canActivate: [AuthGuard],
    data: {
      roles: ['ADMIN', 'CONSULTEUR', 'MANAGER'],
      breadcrumb: 'Utilisateurs'
    }
  },

  // ===================== GESTION DES AFFAIRES =====================
  {
    path: 'Affaires',
    component: ListeAffairesComponent
  },

  // Routes liées aux affaires
  {
    path: 'affaires/:id/demandes-achat',
    component: AffaireDemandeAchatComponent,
    canActivate: [AuthGuard],
    data: {
      roles: ['ADMIN', 'CONSULTEUR', 'chef de projet'],
      breadcrumb: 'Demandes d\'achat de l\'affaire'
    }
  },
  {
    path: 'affaires/:id/receptions',
    component: AffaireReceptionComponent,
    canActivate: [AuthGuard],
    data: {
      roles: ['ADMIN', 'CONSULTEUR', 'chef de projet'],
      breadcrumb: 'Réceptions de l\'affaire'
    }
  },
  {
    path: 'affaires/:id/consommations',
    component: AffaireConsommationComponent,
    canActivate: [AuthGuard],
    data: {
      roles: ['ADMIN', 'CONSULTEUR', 'chef de projet'],
      breadcrumb: 'Consommations de l\'affaire'
    }
  },

  // ===================== DEMANDES D'ACHAT =====================
  {
    path: 'demandes-achat',
    component: ListDemandeAchatComponent,
    canActivate: [AuthGuard],
    data: {
      roles: ['ADMIN', 'CONSULTEUR', 'chef de projet'],
      access: ['demande-achat'],
      breadcrumb: 'Demandes d\'achat'
    }
  },
  {
    path: 'demandes-achat/:id/add-ligne-demande',
    component: LigneDemandeComponent,
    canActivate: [AuthGuard],
    data: {
      roles: ['ADMIN', 'CONSULTEUR', 'chef de projet'],
      access: ['demande-achat'],
      breadcrumb: 'Lignes de demande'
    }
  },

  // ===================== RÉCEPTIONS =====================
  {
    path: 'receptions',
    component: ListReceptionsComponent,
    canActivate: [AuthGuard],
    data: {
      roles: ['ADMIN', 'CONSULTEUR', 'chef de projet'],
      access: ['reception'],
      breadcrumb: 'Réceptions'
    }
  },
  {
    path: 'receptions/:id/add-ligne-reception',
    component: LigneReceptionComponent,
    canActivate: [AuthGuard],
    data: {
      roles: ['ADMIN', 'CONSULTEUR', 'chef de projet'],
      access: ['reception'],
      breadcrumb: 'Lignes de réception'
    }
  },

  // ===================== CONSOMMATIONS =====================
  {
    path: 'consommations',
    component: ListConsommationComponent,
    canActivate: [AuthGuard],
    data: {
      roles: ['ADMIN', 'CONSULTEUR', 'chef de projet'],
      access: ['consommation'],
      breadcrumb: 'Consommations'
    }
  },
  {
    path: 'consommations/:id/add-ligne-consommation',
    component: LigneConsommationComponent,
    canActivate: [AuthGuard],
    data: {
      roles: ['ADMIN', 'CONSULTEUR', 'chef de projet'],
      access: ['consommation'],
      breadcrumb: 'Lignes de consommation'
    }
  },


];

@NgModule({
  imports: [RouterModule.forRoot(routes, {
    useHash: true,
    // Options supplémentaires pour améliorer les performances
    preloadingStrategy: undefined,
    enableTracing: false, // Mettre à true pour déboguer le routing
    // Stratégie de préchargement personnalisée si nécessaire
    // preloadingStrategy: PreloadAllModules
  })],
  exports: [RouterModule]
})
export class AppRoutingModule { }
