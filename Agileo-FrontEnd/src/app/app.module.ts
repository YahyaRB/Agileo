import { NgModule, APP_INITIALIZER } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { SidebarComponent } from './Menu/sidebar/sidebar.component';
import { FooterComponent } from './Menu/footer/footer.component';
import { NavbarComponent } from './Menu/navbar/navbar.component';
import { AcceuilComponent } from './Components/acceuil/acceuil.component';
import { RightSidebarComponent } from './Menu/right-sidebar/right-sidebar.component';
import { RightTabComponent } from './Menu/right-tab/right-tab.component';
import { ManTopHeaderComponent } from './Menu/man-top-header/man-top-header.component';
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import { ListUtilisateursComponent } from './Components/utilisateurs/list-utilisateurs/list-utilisateurs.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatDialogModule } from '@angular/material/dialog';
import { NgSelectModule } from '@ng-select/ng-select';
import {  KeycloakAngularModule, KeycloakService } from 'keycloak-angular';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { NgxPaginationModule } from 'ngx-pagination';
import { PsearchPipe } from './Pipes/psearch.pipe';
import { ListeAffairesComponent } from './Components/affaires/liste-affaires/liste-affaires.component';
import { AddAffaireComponent } from './Components/affaires/add-affaire/add-affaire.component';
import { UpdateAffaireComponent } from './Components/affaires/update-affaire/update-affaire.component';
import { DeleteAffaireComponent } from './Components/affaires/delete-affaire/delete-affaire.component';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { AffaireDemandeAchatComponent } from './Components/affaires/affaire-demande-achat/affaire-demande-achat.component';
import { AffaireReceptionComponent } from './Components/affaires/affaire-reception/affaire-reception.component';
import { AffaireConsommationComponent } from './Components/affaires/affaire-consommation/affaire-consommation.component';
import {RouterModule} from "@angular/router";
import { ListDemandeAchatComponent } from './Components/demande-achat/list-demande-achat/list-demande-achat.component';
import { AddDemandeAchatComponent } from './Components/demande-achat/add-demande-achat/add-demande-achat.component';
import { UpdateDemandeAchatComponent } from './Components/demande-achat/update-demande-achat/update-demande-achat.component';
import { DeleteDemandeAchatComponent } from './Components/demande-achat/delete-demande-achat/delete-demande-achat.component';
import { AffectationUserAffaireComponent } from './Components/utilisateurs/affectation-user-affaire/affectation-user-affaire.component';
import { AffectationUserAccessComponent } from './Components/utilisateurs/affectation-user-access/affectation-user-access.component';
import { AffectationUserRoleComponent } from './Components/utilisateurs/affectation-user-role/affectation-user-role.component';
import { LigneDemandeComponent } from './Components/demande-achat/ligne-demande/ligne-demande.component';
import { ListReceptionsComponent } from './Components/receptions/list-receptions/list-receptions.component';
import { AddReceptionComponent } from './Components/receptions/add-receptions/add-receptions.component';
import { LigneReceptionComponent } from './Components/receptions/ligne-receptions/ligne-reception.component';
import { ListConsommationComponent } from "./Components/consommations/list-consommation/list-consommation.component";
import { AddConsommationComponent } from './Components/consommations/add-consommation/add-consommation.component';
import { DeleteConsommationComponent } from './Components/consommations/delete-consommation/delete-consommation.component';
import { LigneConsommationComponent } from './Components/consommations/ligne-consommation/ligne-consommation.component';
import {ToastrModule} from "ngx-toastr";
import {
  DetailsFilesReceptionComponent
} from "./Components/receptions/detals-files-reception.component/detals-files-reception.component";


// Import du service FileService et de l'intercepteur
import { FileService } from './services/file-service.service';
import {DashboardService} from "./services/dashboard.service";

function initializeKeycloak(keycloak: KeycloakService) {
  return () =>
    keycloak.init({
      config: {
        url: 'http://192.168.77.21:8081',
        realm: 'RB-realm',
        clientId: 'Client_Agileo'
      },

      initOptions: {
        onLoad: 'login-required',
        silentCheckSsoRedirectUri:
          window.location.origin + '/assets/silent-check-sso.html'
      },
    });
}

@NgModule({
  declarations: [
    AppComponent,
    SidebarComponent,
    FooterComponent,
    NavbarComponent,
    AcceuilComponent,
    RightSidebarComponent,
    RightTabComponent,
    ManTopHeaderComponent,
    ListUtilisateursComponent,
    PsearchPipe,
    ListeAffairesComponent,
    AddAffaireComponent,
    UpdateAffaireComponent,
    DeleteAffaireComponent,
    AffaireDemandeAchatComponent,
    AffaireReceptionComponent,
    AffaireConsommationComponent,
    ListDemandeAchatComponent,
    AddDemandeAchatComponent,
    UpdateDemandeAchatComponent,
    DeleteDemandeAchatComponent,
    AffectationUserAffaireComponent,
    AffectationUserAccessComponent,
    AffectationUserRoleComponent,
    LigneDemandeComponent,
    ListReceptionsComponent,
    AddReceptionComponent,
    LigneReceptionComponent,
    DetailsFilesReceptionComponent,
    ListConsommationComponent,
    AddConsommationComponent,
    DeleteConsommationComponent,
    LigneConsommationComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    ReactiveFormsModule,
    BrowserAnimationsModule,
    MatDialogModule,
    NgSelectModule,
    FormsModule,
    KeycloakAngularModule,
    HttpClientModule,
    ToastrModule.forRoot({
      timeOut: 3000,
      positionClass: 'toast-top-right',
      preventDuplicates: true,
      progressBar: true,
      closeButton: true
    }),
    NgxPaginationModule,
    MatMenuModule,
    MatIconModule,
    MatButtonModule,
    RouterModule,
  ],
  providers: [
    DashboardService,
    FileService,
    {
      provide: APP_INITIALIZER,
      useFactory: initializeKeycloak,
      multi: true,
      deps: [KeycloakService]
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {

}
