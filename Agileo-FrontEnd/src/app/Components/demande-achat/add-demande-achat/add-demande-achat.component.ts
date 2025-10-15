import {Component, ElementRef, EventEmitter, OnInit, Output, ViewChild} from '@angular/core';
import {Affaire} from "../../../../interfaces/iaffaire";
import {IFiles} from "../../../../interfaces/ifiles";
import {IDemandeAchat} from "../../../../interfaces/idemandeAchat";
import {AffaireServiceService} from "../../../services/affaire-service.service";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {DemandeAchatService} from "../../../services/demande-achat.service";
import {HttpClient} from "@angular/common/http";
import {NotificationService} from "../../../services/notification.service";
import {UtilisateurServiceService} from "../../../services/utilisateur-service.service";
import {environment} from "../../../../environments/environment";
import {Router} from "@angular/router";

@Component({
  selector: 'app-add-demande-achat',
  templateUrl: './add-demande-achat.component.html',
  styleUrls: ['./add-demande-achat.component.css']
})
export class AddDemandeAchatComponent implements OnInit {
  @ViewChild('closebutton', {static: false}) closebutton!: ElementRef;
  @Output() ajoutEffectue = new EventEmitter<void>();

  selectedFiles: IFiles[] = [];
  affaires!: Affaire[];
  myFormRegister!: FormGroup;
  currentUser: any;
  isLoading = false;

  // Configuration des fichiers
  readonly MAX_FILES = 3; // Augmenté de 3
  readonly MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
  readonly ALLOWED_EXTENSIONS = ['.pdf', '.xls', '.xlsx', '.png', '.jpg', '.jpeg', '.doc', '.docx'];

  constructor(
    private affaireService: AffaireServiceService,
    private demandeAchatService: DemandeAchatService,
    private notifyService: NotificationService,
    private http: HttpClient,
    private router: Router,
    private formBuilder: FormBuilder,
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    this.affaireList();
    this.initMyRegisterForm();
  }

  /**
   * Récupération de l'utilisateur connecté via l'endpoint d'authentification
   */
  private loadCurrentUser() {
    this.http.get(`${environment.apiUrl}auth/user-info`).subscribe({
      next: (response: any) => {
        if (response.success && response.user) {
          this.currentUser = response.user;
        } else {
          console.error("Réponse d'authentification invalide:", response);
          this.notifyService.showError('Réponse d\'authentification invalide', 'Erreur');
        }
      },
      error: err => {
        console.error("Erreur lors du chargement de l'utilisateur:", err);
        this.notifyService.showError('Erreur lors du chargement des informations utilisateur', 'Erreur');
      }
    });
  }

  private initMyRegisterForm() {
    this.myFormRegister = this.formBuilder.group({
      affaire: [null, Validators.required],
      delai: ['', Validators.required],
      commentaire: ['']
    });
  }

  /**
   * Chargement de la liste des affaires
   */
  affaireList() {
    this.affaireService.getAffaires().subscribe({
      next: data => {
        this.affaires = data;
      },
      error: err => {
        console.error("Erreur lors du chargement des affaires:", err);
        this.notifyService.showError('Erreur lors du chargement des affaires', 'Erreur');
      }
    });
  }

  /**
   * Recalcul des noms générés pour les fichiers
   */
  private recalculateGeneratedNames() {
    const affaireName = this.myFormRegister.get('affaire')?.value?.affaire || 'DA';
    const date = new Date().toISOString().slice(0, 10).replace(/-/g, '');
    this.selectedFiles = this.selectedFiles.map((f, idx) => {
      const extension = f.name.split('.').pop();
      return {
        ...f,
        generatedName: `${affaireName}_${date}_${idx + 1}.${extension}`
      };
    });
  }

  /**
   * Validation d'un fichier
   */
  private validateFile(file: File): string | null {
    // Vérifier la taille
    if (file.size > this.MAX_FILE_SIZE) {
      return `Le fichier "${file.name}" est trop volumineux (max: 50MB)`;
    }

    // Vérifier l'extension
    const extension = '.' + file.name.split('.').pop()?.toLowerCase();
    if (!this.ALLOWED_EXTENSIONS.includes(extension)) {
      return `Le fichier "${file.name}" n'est pas d'un type autorisé`;
    }

    // Vérifier les doublons
    const isDuplicate = this.selectedFiles.some(f => f.name === file.name && f.file.size === file.size);
    if (isDuplicate) {
      return `Le fichier "${file.name}" a déjà été sélectionné`;
    }

    return null; // Pas d'erreur
  }

  /**
   * Gestion de la sélection de fichiers
   */
  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;
    const files = Array.from(input.files);
    const remainingSlots = this.MAX_FILES - this.selectedFiles.length;
    if (files.length > remainingSlots) {
      this.notifyService.showError(
        `Vous ne pouvez ajouter que ${remainingSlots} fichier(s) supplémentaire(s). Maximum: ${this.MAX_FILES} fichiers.`,
        'Limite de fichiers'
      );
      return;
    }
    // Valider chaque fichier
    const validFiles: File[] = [];
    const errors: string[] = [];
    files.forEach(file => {
      const error = this.validateFile(file);
      if (error) {
        errors.push(error);
      } else {
        validFiles.push(file);
      }
    });
    // Afficher les erreurs s'il y en a
    if (errors.length > 0) {
      this.notifyService.showError(errors.join('\n'), 'Erreurs de validation');
    }
    // Ajouter les fichiers valides
    validFiles.forEach(file => {
      this.selectedFiles.push({
        file,
        name: file.name,
        size: file.size,
        sizeFormatted: this.formatFileSize(file.size)
      });
    });
    this.recalculateGeneratedNames();
    // Réinitialiser l'input pour permettre la sélection du même fichier
    input.value = '';
  }

  /**
   * Formatage de la taille de fichier
   */
  private formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  /**
   * Suppression d'un fichier sélectionné
   */
  removeFile(index: number) {
    this.selectedFiles.splice(index, 1);
    this.recalculateGeneratedNames();
    this.notifyService.showInfo('Fichier supprimé de la sélection', 'Information');
  }

  /**
   * Enregistrement de la demande d'achat
   */
  onRegisterDemandeAchat() {
    if (this.myFormRegister.invalid) {
      this.notifyService.showError('Veuillez remplir tous les champs obligatoires', 'Formulaire invalide');
      return;
    }
    if (!this.currentUser) {
      this.notifyService.showError('Informations utilisateur manquantes', 'Erreur');
      return;
    }

    const {affaire, delai, commentaire} = this.myFormRegister.value;

    // Validation des données
    if (!affaire || !affaire.affaire) {
      this.notifyService.showError('Veuillez sélectionner une affaire valide', 'Erreur de validation');
      return;
    }

    const loginValue = this.currentUser.idAgelio;
    console.log("loginValue utilisé:", loginValue);

    if (!loginValue) {
      this.notifyService.showError('ID Agelio utilisateur non disponible. Contactez l\'administrateur.', 'Erreur');
      console.error("idAgelio manquant dans currentUser:", this.currentUser);
      return;
    }

    const loginValueAsInteger = parseInt(loginValue, 10);
    if (isNaN(loginValueAsInteger)) {
      this.notifyService.showError('ID Agelio invalide. Contactez l\'administrateur.', 'Erreur');
      console.error("idAgelio non numérique:", loginValue);
      return;
    }

    // Payload selon l'entité DemandeAchat
    const payload: IDemandeAchat = {
      chantier: affaire.affaire,
      delaiSouhaite: new Date(delai).toISOString(),
      commentaire: commentaire || null,
      login: loginValueAsInteger
    };

    console.log("Payload à envoyer:", payload);
    this.isLoading = true;

    this.demandeAchatService.addDemandeAchat(payload).subscribe({
      next: (data: IDemandeAchat) => {
        console.log("Demande Achat enregistrée avec succès", data);

        if (!data || !data.id) {
          this.notifyService.showError('Erreur: ID de demande non reçu du serveur', 'Erreur');
          this.isLoading = false;
          return;
        }

        // MODIFICATION PRINCIPALE : Appeler la nouvelle méthode
        if (this.selectedFiles.length > 0) {
          this.uploadFilesAfterCreation(data.id);
        } else {
          this.onSuccess(data.id);
        }
      },
      error: err => {
        console.error("Erreur lors d'enregistrement de la demande", err);
        this.isLoading = false;
        let errorMessage = 'Erreur lors de la création de la demande d\'achat';
        if (err.error && err.error.message) {
          errorMessage = err.error.message;
        } else if (err.message) {
          errorMessage = err.message;
        }
        this.notifyService.showError(errorMessage, 'Erreur de création');
      }
    });
  }

  /**
   * Upload des fichiers attachés (CORRIGÉ)
   */

  private uploadFilesAfterCreation(demandeId: number) {
    console.log(`Upload de ${this.selectedFiles.length} fichier(s) pour la demande ${demandeId}`);

    // Créer un tableau de File à partir des IFiles
    const files: File[] = this.selectedFiles.map(fileObj => fileObj.file);

    // UTILISER la méthode qui appelle le bon endpoint KDN_FILE
    this.demandeAchatService.uploadFilesArrayForDemande(demandeId, files).subscribe({
      next: (response) => {
        this.notifyService.showSuccess(
          `${this.selectedFiles.length} fichier(s) uploadé(s) avec succès`,
          'Upload réussi'
        );
        // CRUCIAL : Appeler onSuccess() pour débloquer l'interface
        this.onSuccess(demandeId);
      },
      error: err => {
        console.error("Erreur upload fichiers vers KDN_FILE", err);
        this.notifyService.showWarning(
          'Demande créée mais erreur lors de l\'upload des fichiers',
          'Upload partiel'
        );
        // CRUCIAL : Même en cas d'erreur, appeler onSuccess() pour débloquer l'interface
        this.onFailed();
      }
    });
  }

  /**
   * Traitement du succès
   */
  private onSuccess(idDA:number) {
    this.isLoading = false;
    this.notifyService.showSuccess(
      'Demande d\'achat ajoutée avec succès !', 'Ajout'
    );
    this.router.navigate(['/demandes-achat', idDA, 'add-ligne-demande']);
    this.ajoutEffectue.emit();
    this.closebutton.nativeElement.click();
  }
  private onFailed() {
    this.isLoading = false;
    this.notifyService.showError(
      "Erreur d'ajour du Demande d'achat !", 'Ajout'
    );
    this.ajoutEffectue.emit();
    this.closebutton.nativeElement.click();
  }


  private resetForm() {
    this.myFormRegister.reset();
    this.selectedFiles = [];
  }

  onCancel() {
    this.resetForm();
    this.closebutton.nativeElement.click();
  }

  get remainingFilesCount(): number {
    return this.MAX_FILES - this.selectedFiles.length;
  }

  get canAddMoreFiles(): boolean {
    return this.selectedFiles.length < this.MAX_FILES;
  }
}
