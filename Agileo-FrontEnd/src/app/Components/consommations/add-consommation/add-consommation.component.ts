import {Component, ElementRef, EventEmitter, OnInit, Output, ViewChild} from '@angular/core';
import {Affaire} from "../../../../interfaces/iaffaire";
import {AffaireServiceService} from "../../../services/affaire-service.service";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {ConsommationService} from "../../../services/consommation.service";
import {UserProfileService} from "../../../services/user-profile.service";
import {NotificationService} from "../../../services/notification.service";
import {SharedAffaireService} from "../../../services/shared-affaire.service";
import {Router} from "@angular/router";

@Component({
  selector: 'app-add-consommation',
  templateUrl: './add-consommation.component.html',
  styleUrls: ['./add-consommation.component.css']
})
export class AddConsommationComponent implements OnInit {
  @ViewChild('closebutton', {static: false}) closebutton!: ElementRef;
  @Output() ajoutEffectue = new EventEmitter<void>();

  affaires: Affaire[] = [];
  myFormRegister!: FormGroup;

  currentUser: any = null;
  isLoadingUser = true;

  constructor(
    private affaireService: AffaireServiceService,
    private consommationService: ConsommationService,
    private userProfileService: UserProfileService,
    private sharedAffaireService: SharedAffaireService,
    private notifyService: NotificationService,
    private router: Router,
    private formBuilder: FormBuilder
  ) {}


  ngOnInit(): void {
    this.initMyRegisterForm();
    this.loadCurrentUser();
  }

  private initMyRegisterForm() {
    // CORRECTION 1: Format date simple (yyyy-MM-dd) pour correspondre à input type="date"
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const todayDate = `${year}-${month}-${day}`;

    this.myFormRegister = this.formBuilder.group({
      affaire: [null, Validators.required],
      dateConsommation: [todayDate, Validators.required]
    });
  }

  private loadCurrentUser() {
    this.isLoadingUser = true;
    this.userProfileService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUser = user;
        this.isLoadingUser = false;
        this.loadAffaires();
      },
      error: (error) => {
        console.error('Erreur chargement utilisateur:', error);
        this.isLoadingUser = false;
        this.loadAffaires();
      }
    });
  }



  private loadAffaires() {
    this.sharedAffaireService.getAffairesForUser(this.currentUser).subscribe({
      next: (affaires) => {
        this.affaires = affaires;
        console.log(`Affaires chargées pour ajout: ${affaires.length} affaires`);
      },
      error: (err) => {
        console.error("Erreur chargement affaires pour ajout:", err);
        this.affaires = [];
      }
    });
  }


  private isAdminOrManager(): boolean {
    if (!this.currentUser?.roles) return false;

    const roles = Array.isArray(this.currentUser.roles)
      ? this.currentUser.roles
      : [this.currentUser.roles];

    return roles.some((role: any) => {
      const roleName = typeof role === 'string' ? role : role.name;
      return roleName === 'ADMIN' || roleName === 'MANAGER';
    });
  }



  private loadUserSpecificAffaires() {
    this.affaireService.getCurrentAccessorAffaires().subscribe({
      next: (data) => {
        if (data && data.length > 0) {
          this.affaires = this.normalizeAffaires(data);
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

  private loadAffairesByUserId() {
    if (this.currentUser?.id) {
      this.affaireService.getAccessorAffaires(this.currentUser.id).subscribe({
        next: (data) => {
          if (data && data.length > 0) {
            this.affaires = this.normalizeAffaires(data);
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
            this.affaires = allAffaires.filter(affaire =>
              userAffaireCodes.includes(affaire.code) ||
              userAffaireCodes.includes(affaire.affaire)
            );
          } else {
            this.affaires = [];
          }
        } else {
          this.affaires = [];
        }
      },
      error: (err) => {
        console.error("Erreur chargement affaires pour filtrage:", err);
        this.affaires = [];
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

  private normalizeAffaires(affaires: any[]): Affaire[] {
    return affaires.map(affaire => ({
      id: affaire.id || affaire.numero,
      code: affaire.code || affaire.affaire,
      affaire: affaire.affaire || affaire.code,
      nom: affaire.nom || affaire.libelle,
      libelle: affaire.libelle || affaire.nom,
      ...affaire
    }));
  }

  onRegisterConsommation() {
    if (this.myFormRegister.invalid) {
      this.notifyService.showError('Formulaire invalide', 'Erreur');
      return;
    }

    const selectedAffaire = this.myFormRegister.get('affaire')?.value;
    if (!selectedAffaire) {
      this.notifyService.showError('Sélectionnez une affaire', 'Erreur');
      return;
    }

    const dateValue = this.myFormRegister.get('dateConsommation')?.value;

    // CORRECTION 2: Convertir la date au format attendu par le backend
    let formattedDate;
    if (typeof dateValue === 'string') {
      if (dateValue.length === 10) {
        // Format yyyy-MM-dd, ajouter l'heure pour LocalDateTime
        formattedDate = dateValue + 'T12:00:00';
      } else {
        formattedDate = dateValue;
      }
    } else if (dateValue instanceof Date) {
      // Si c'est un objet Date
      const year = dateValue.getFullYear();
      const month = String(dateValue.getMonth() + 1).padStart(2, '0');
      const day = String(dateValue.getDate()).padStart(2, '0');
      formattedDate = `${year}-${month}-${day}T12:00:00`;
    }

    // CORRECTION 3: Utiliser affaire.code comme identifiant principal
    const affaireId = selectedAffaire.code || selectedAffaire.affaire;

    const payload = {
      affaireId: affaireId,
      dateConsommation: formattedDate
    };

    this.consommationService.addConsommation(payload).subscribe({
      next: (data) => {
        this.notifyService.showSuccess('Consommation ajoutée avec succès', 'Succès');

        // ✅ Redirection vers la page ligne-consommation avec l'ID retourné
        this.router.navigate(['/consommations', data.id, 'add-ligne-consommation']);
      },
      error: (err) => {
        console.error('❌ Erreur création consommation:', err);
        console.error('Détails erreur:', err.error);

        let message = 'Erreur lors de la création';

        // CORRECTION 4: Meilleure gestion des erreurs
        if (err.error) {
          if (typeof err.error === 'string') {
            message = err.error;
          } else if (err.error.message) {
            message = err.error.message;
          } else if (err.error.error) {
            message = err.error.error;
          }
        }

        this.notifyService.showError(message, 'Erreur');
      }
    });
  }

  resetForms() {
    this.myFormRegister.reset();
    this.initMyRegisterForm();
  }
  canViewAllAffaires(): boolean {
    return this.sharedAffaireService.isAdminOrManager(this.currentUser);
  }

}
