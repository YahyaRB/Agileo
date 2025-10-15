import { Component, OnInit, ViewChild, Output, ElementRef, OnDestroy } from '@angular/core';
import { EventEmitter } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { AffaireServiceService } from "../../../services/affaire-service.service";
import { Affaire } from "../../../../interfaces/iaffaire";
import { NotificationService } from "../../../services/notification.service";
import { TempDataService } from "../../../services/temp-data.service";
import { UtilisateurServiceService } from "../../../services/utilisateur-service.service";
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-add-affaire',
  templateUrl: './add-affaire.component.html',
  styleUrls: ['./add-affaire.component.css']
})
export class AddAffaireComponent implements OnInit, OnDestroy {
  @ViewChild('closebutton', { static: false }) closebutton!: ElementRef;
  @Output() ajoutEffectue = new EventEmitter<void>();

  myFormAdd!: FormGroup;
  isLoading = false;

  // Listes pour les sélections
  availableAffaires: any[] = []; // Affaires depuis l'API temp (vue Affaires)
  availableAccessors: any[] = []; // KdnsAccessors disponibles

  private subscriptions: Subscription = new Subscription();

  constructor(
    private formBuilder: FormBuilder,
    private notifyService: NotificationService,
    private affaireService: AffaireServiceService,
    private tempDataService: TempDataService,
    private utilisateurService: UtilisateurServiceService
  ) {}

  ngOnInit(): void {
    this.initFormAdd();
    this.loadData();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  private initFormAdd(): void {
    this.myFormAdd = this.formBuilder.group({
      selectedAffaire: ['', Validators.required], // Code d'affaire à sélectionner
      selectedAccessor: ['', Validators.required], // Accessor à assigner
      commentaire: [''], // Commentaire optionnel
      statut: [1, Validators.required] // Statut système (1 = actif par défaut)
    });
  }

  private loadData(): void {
    this.isLoading = true;

    // Charger les affaires disponibles depuis l'API temp
    const affairesSubscription = this.tempDataService.getAllAffaires().subscribe({
      next: (affaires) => {
        this.availableAffaires = affaires;
        console.log("Affaires chargées depuis l'API temp =>", this.availableAffaires);
        this.checkLoadingComplete();
      },
      error: (error) => {
        console.error("Erreur lors du chargement des affaires temp", error);
        this.notifyService.showError('Erreur lors du chargement des affaires disponibles', 'Erreur');
        this.isLoading = false;
      }
    });

    // Charger les utilisateurs/accessors disponibles
    const usersSubscription = this.utilisateurService.getAllUsers().subscribe({
      next: (users) => {
        this.availableAccessors = users;
        console.log("Accessors chargés =>", this.availableAccessors);
        this.checkLoadingComplete();
      },
      error: (error) => {
        console.error("Erreur lors du chargement des utilisateurs", error);
        this.notifyService.showError('Erreur lors du chargement des utilisateurs', 'Erreur');
        this.isLoading = false;
      }
    });

    this.subscriptions.add(affairesSubscription);
    this.subscriptions.add(usersSubscription);
  }

  private checkLoadingComplete(): void {
    if (this.availableAffaires.length > 0 && this.availableAccessors.length > 0) {
      this.isLoading = false;
    }
  }

  onAddAffaire(): void {
    if (this.myFormAdd.invalid) {
      this.notifyService.showError('Veuillez remplir tous les champs obligatoires', 'Formulaire invalide');
      return;
    }

    const formValues = this.myFormAdd.value;
    console.log("Données du formulaire =>", formValues);

    const selectedAffaire = this.availableAffaires.find(
      affaire => affaire.code === formValues.selectedAffaire
    );
    const selectedAccessor = this.availableAccessors.find(
      accessor => accessor.id === parseInt(formValues.selectedAccessor)
    );

    if (!selectedAffaire) {
      this.notifyService.showError('Veuillez sélectionner une affaire valide', 'Erreur');
      return;
    }

    if (!selectedAccessor) {
      this.notifyService.showError('Veuillez sélectionner un utilisateur valide', 'Erreur');
      return;
    }

    // Vérifier d'abord si l'assignation est possible
    const validationSubscription = this.affaireService.canAssignAccessorToAffaire(
      selectedAffaire.code,
      selectedAccessor.id
    ).subscribe({
      next: (canAssign) => {
        if (canAssign) {
          this.proceedWithAssignment(selectedAffaire.code, selectedAccessor.id, formValues.commentaire);
        } else {
          this.notifyService.showError(
            'Cet utilisateur ne peut pas être assigné à cette affaire (déjà assigné ou utilisateur inactif)',
            'Assignation impossible'
          );
        }
      },
      error: (error) => {
        console.error("Erreur lors de la validation", error);
        // Procéder quand même si la validation échoue
        this.proceedWithAssignment(selectedAffaire.code, selectedAccessor.id, formValues.commentaire);
      }
    });

    this.subscriptions.add(validationSubscription);
  }

  private proceedWithAssignment(affaireCode: string, accessorId: number, commentaire: string): void {
    this.isLoading = true;

    // Utiliser la nouvelle méthode pour assigner un accessor à une affaire
    const assignSubscription = this.affaireService.addAccessorToAffaire(affaireCode, accessorId).subscribe({
      next: (response) => {
        console.log("Assignation réussie =>", response);
        this.isLoading = false;
        this.initFormAdd();
        this.notifyService.showSuccess(
          `Utilisateur assigné à l'affaire ${affaireCode} avec succès !`,
          'Assignation réussie'
        );
        this.ajoutEffectue.emit();
        this.closebutton.nativeElement.click();
      },
      error: (error) => {
        console.error("Erreur lors de l'assignation", error);
        this.isLoading = false;

        let errorMessage = 'Erreur lors de l\'assignation à l\'affaire';
        if (error.message && error.message.includes('déjà assigné')) {
          errorMessage = 'Cet utilisateur est déjà assigné à cette affaire';
        } else if (error.message && error.message.includes('non trouvé')) {
          errorMessage = 'Affaire ou utilisateur non trouvé';
        }

        this.notifyService.showError(errorMessage, 'Erreur');
      }
    });

    this.subscriptions.add(assignSubscription);
  }

  // Méthode pour obtenir le nom complet d'un accessor
  getAccessorDisplayName(accessor: any): string {
    if (accessor.nom && accessor.prenom) {
      return `${accessor.prenom} ${accessor.nom} (${accessor.login})`;
    }
    return `${accessor.login}`;
  }

  // Méthode pour obtenir l'affichage d'une affaire
  getAffaireDisplayName(affaire: any): string {
    return `${affaire.code} - ${affaire.nom || affaire.libelle}`;
  }

  // Méthode pour fermer le modal sans sauvegarder
  onCancel(): void {
    this.initFormAdd();
    this.closebutton.nativeElement.click();
  }

  // Getters pour faciliter l'accès aux contrôles du formulaire
  get selectedAffaire() {
    return this.myFormAdd.get('selectedAffaire');
  }

  get selectedAccessor() {
    return this.myFormAdd.get('selectedAccessor');
  }

  get commentaire() {
    return this.myFormAdd.get('commentaire');
  }

  get statut() {
    return this.myFormAdd.get('statut');
  }
}
