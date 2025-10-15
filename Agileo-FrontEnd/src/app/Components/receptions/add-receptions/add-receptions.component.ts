import {Component, ElementRef, EventEmitter, OnInit, Output, ViewChild} from '@angular/core';
import {Affaire} from "../../../../interfaces/iaffaire";
import {AffaireServiceService} from "../../../services/affaire-service.service";
import {FormArray, FormBuilder, FormGroup, Validators} from "@angular/forms";
import {ReceptionService} from "../../../services/reception.service";
import {TempDataService} from "../../../services/temp-data.service";
import {IFiles} from "../../../../interfaces/ifiles";
import {HttpClient} from "@angular/common/http";
import {NotificationService} from "../../../services/notification.service";
import {UserProfileService} from "../../../services/user-profile.service";
import {Router} from "@angular/router";
import {BonCommandeCacheService} from "../../../services/bon-commande-cache.service";

@Component({
  selector: 'app-add-reception',
  templateUrl: './add-receptions.component.html',
  styleUrls: ['./add-receptions.component.css']
})
export class AddReceptionComponent implements OnInit {
  @ViewChild('closebutton', {static: false}) closebutton!: ElementRef;
  @Output() ajoutEffectue = new EventEmitter<void>();

  // Step Management - NOW 4 STEPS
  currentStep: number = 1;

  isDragOver = false;
  private dragCounter = 0;
  selectedFiles: IFiles[] = [];
  affaires: Affaire[] = [];
  bonCommandes: any[] = [];
  filteredBonCommandes: any[] = [];
  availableAffaires: any[] = [];
  availableFournisseurs: any[] = [];
  fournisseurs: string[] = [];
  myFormRegister!: FormGroup;
  currentUser: any = null;
  userAffaires: string[] = [];
  isLoadingUser = true;
  isSubmitting = false;
  readonly MAX_FILES = 3;
  readonly MAX_FILE_SIZE = 50 * 1024 * 1024;
  readonly ALLOWED_EXTENSIONS = ['.pdf', '.xls', '.xlsx', '.png', '.jpg', '.jpeg', '.doc', '.docx'];

  constructor(
    private affaireService: AffaireServiceService,
    private receptionService: ReceptionService,
    private tempDataService: TempDataService,
    private userProfileService: UserProfileService,
    private notifyService: NotificationService,
    private bonCommandeCacheService: BonCommandeCacheService,
    private formBuilder: FormBuilder,
    private router: Router
  ) {}

  private createdReceptionId: number | null = null;

  ngOnInit(): void {
    this.initMyRegisterForm();
    this.loadCurrentUser();
  }

  // ========== GESTION DES STEPS (4 STEPS) ==========

  nextStep(): void {
    if (this.canGoToNextStep() && this.currentStep < 4) {
      this.currentStep++;
      console.log('Navigation vers step', this.currentStep);
    }
  }

  previousStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
      console.log('Retour vers step', this.currentStep);
    }
  }

  canGoToNextStep(): boolean {
    switch (this.currentStep) {
      case 1:
        // Step 1: Affaire ET Bon de commande obligatoires
        const affaireFilterValid = this.myFormRegister.get('affaireFilter')?.valid &&
          this.myFormRegister.get('affaireFilter')?.value !== null;
        const bonCommandeValid = this.myFormRegister.get('bonCommande')?.valid &&
          this.myFormRegister.get('bonCommande')?.value !== null;
        return affaireFilterValid && bonCommandeValid;

      case 2:
        // Step 2: Détails du BC - toujours valide (juste consultation)
        return true;

      case 3:
        // Step 3: Date de bl obligatoire
        return this.myFormRegister.get('dateBl')?.valid;

      case 4:
        // Step 4: Pas de validation obligatoire (fichiers optionnels)
        return true;

      default:
        return false;
    }
  }

  // Variables pour les lignes du bon de commande
  lignesBonCommande: any[] = [];
  isLoadingLignesBC = false;

  // Méthode pour obtenir le BC sélectionné pour affichage dans step 2
  getSelectedBonCommande(): any {
    return this.myFormRegister.get('bonCommande')?.value;
  }

  // Charger les lignes du bon de commande sélectionné
  loadLignesBonCommande(bonCommande: any) {
    if (!bonCommande || !bonCommande.commande) {
      this.lignesBonCommande = [];
      return;
    }

    this.isLoadingLignesBC = true;
    console.log('Chargement des lignes pour BC:', bonCommande.commande);

    // Utiliser le service pour récupérer les lignes du BC
    this.bonCommandeCacheService.getLignesBonCommande(bonCommande.commande).subscribe({
      next: (lignes) => {
        this.lignesBonCommande = lignes || [];
        console.log('Lignes BC chargées:', this.lignesBonCommande);
        this.isLoadingLignesBC = false;
      },
      error: (err) => {
        console.error('Erreur chargement lignes BC:', err);
        this.lignesBonCommande = [];
        this.isLoadingLignesBC = false;
        this.notifyService.showError('Erreur lors du chargement des lignes du bon de commande', 'Erreur');
      }
    });
  }

  // ========== MÉTHODES EXISTANTES ==========

  private isAdmin(): boolean {
    return this.currentUser?.roles?.includes('ADMIN') || this.currentUser?.roles?.includes('MANAGER');
  }

  private loadCurrentUser() {
    this.isLoadingUser = true;
    this.userProfileService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUser = user;
        console.log('Utilisateur chargé:', {
          nom: user.nom,
          roles: user.roles
        });
        this.isLoadingUser = false;
        this.loadInitialData();
      },
      error: (error) => {
        console.error('Erreur chargement utilisateur:', error);
        this.isLoadingUser = false;
        this.loadInitialData();
      }
    });
  }

  private loadInitialData() {
    console.log('=== DÉBUT CHARGEMENT DONNÉES INITIALES ===');

    this.affaireService.getAffaires().subscribe({
      next: data => {
        this.affaires = data || [];
        console.log(`🏢 Affaires chargées: ${this.affaires.length}`);
        if (this.affaires.length === 0) {
          console.warn('⚠️ Aucune affaire disponible pour cet utilisateur');
        }
      },
      error: err => {
        console.error("❌ Erreur chargement affaires", err);
        this.affaires = [];
      }
    });

    this.loadBonCommandesForAffaires();

    this.tempDataService.getAllFournisseurs().subscribe({
      next: data => {
        this.fournisseurs = data || [];
        console.log(`🏭 Fournisseurs chargés: ${this.fournisseurs.length}`);
      },
      error: err => {
        console.error("❌ Erreur chargement fournisseurs", err);
        this.fournisseurs = [];
      }
    });

    console.log('=== FIN CHARGEMENT DONNÉES INITIALES ===');
  }

  private loadBonCommandesForAffaires() {
    console.log('=== CHARGEMENT BONS DE COMMANDE ===');
    this.bonCommandeCacheService.getBonCommandes().subscribe({
      next: data => {
        this.bonCommandes = data || [];
        console.log(`📋 Bons de commande chargés: ${this.bonCommandes.length}`);

        // ✅ NOUVEAU : Charger et filtrer les affaires réelles
        this.loadAndFilterAffairesFromBonCommandes();
      },
      error: err => {
        console.error("❌ Erreur chargement bons de commandes", err);
        this.bonCommandes = [];
        this.availableAffaires = [];
      }
    });
  }
  /**
   * Charge les affaires RÉELLES depuis le backend et filtre les BC
   */
  private loadAndFilterAffairesFromBonCommandes(): void {
    console.log('=== CHARGEMENT AFFAIRES RÉELLES (ADD) ===');

    this.affaireService.getAffaires().subscribe({
      next: (affaires) => {
        console.log(`✅ ${affaires.length} affaires chargées depuis la base`);

        // Créer un Set des codes d'affaires réelles
        const affairesReellesSet = new Set(
          affaires.map(aff => aff.code || aff.affaire)
        );

        console.log('Affaires réelles:', Array.from(affairesReellesSet));

        // Filtrer les bons de commandes : garder SEULEMENT ceux avec des affaires réelles
        const bcAvant = this.bonCommandes.length;
        this.bonCommandes = this.bonCommandes.filter(bc => {
          const affaireExiste = affairesReellesSet.has(bc.affaireCode);

          if (!affaireExiste) {
            console.warn(`⚠️ BC ${bc.commande} ignoré : affaire "${bc.affaireCode}" inexistante`);
          }

          return affaireExiste;
        });

        console.log(`📊 Filtrage BC (ADD): ${bcAvant} → ${this.bonCommandes.length}`);

        // Extraire les affaires disponibles APRÈS filtrage
        this.extractAvailableAffaires();
      },
      error: (err) => {
        console.error('❌ Erreur chargement affaires:', err);
        this.notifyService.showError(
          'Erreur lors du chargement des affaires',
          'Erreur'
        );

        // En cas d'erreur, continuer avec tous les BC
        this.extractAvailableAffaires();
      }
    });
  }
  private extractAvailableAffaires() {
    console.log('=== EXTRACTION DES AFFAIRES DISPONIBLES (ADD) ===');
    console.log('Nombre de bons de commande filtrés:', this.bonCommandes.length);

    const affairesMap = new Map<string, any>();

    this.bonCommandes.forEach(bc => {
      if (bc.affaireCode && !affairesMap.has(bc.affaireCode)) {
        affairesMap.set(bc.affaireCode, {
          code: bc.affaireCode,
          name: bc.affaireName || bc.affaireCode
        });
      }
    });

    this.availableAffaires = Array.from(affairesMap.values())
      .sort((a, b) => a.code.localeCompare(b.code)); // Tri alphabétique

    console.log(`✅ ${this.availableAffaires.length} affaires disponibles (ADD):`,
      this.availableAffaires.map(a => a.code));
  }

  onAffaireFilterChange(selectedAffaire: any) {
    console.log('=== CHANGEMENT AFFAIRE ===');
    console.log('Affaire sélectionnée:', selectedAffaire);

    if (selectedAffaire) {
      const bonCommandesByAffaire = this.bonCommandes.filter(
        bc => bc.affaireCode === selectedAffaire.code
      );

      this.extractFournisseursFromBonCommandes(bonCommandesByAffaire);
      this.filteredBonCommandes = bonCommandesByAffaire;

      console.log(`📋 ${this.filteredBonCommandes.length} bons de commande pour l'affaire ${selectedAffaire.code}`);

      this.myFormRegister.patchValue({
        fournisseurFilter: null,
        bonCommande: null,
        nomFournisseur: '',
        refFournisseur: '',
        affaire: null
      });
    } else {
      this.filteredBonCommandes = [];
      this.availableFournisseurs = [];
      this.myFormRegister.patchValue({
        fournisseurFilter: null,
        bonCommande: null,
        nomFournisseur: '',
        refFournisseur: '',
        affaire: null
      });
    }
  }

  private extractFournisseursFromBonCommandes(bonCommandes: any[]) {
    const fournisseursMap = new Map<string, { name: string, id: string, count: number }>();

    bonCommandes.forEach(bc => {
      if (bc.fournisseur) {
        const key = bc.fournisseur.trim();
        if (fournisseursMap.has(key)) {
          const existing = fournisseursMap.get(key)!;
          existing.count++;
        } else {
          fournisseursMap.set(key, {
            name: bc.fournisseur.trim(),
            id: bc.fournisseurId || '',
            count: 1
          });
        }
      }
    });

    this.availableFournisseurs = Array.from(fournisseursMap.values())
      .sort((a, b) => a.name.localeCompare(b.name));

    console.log(`🏭 Fournisseurs extraits: ${this.availableFournisseurs.length}`, this.availableFournisseurs);
  }

  onFournisseurFilterChange(selectedFournisseur: any) {
    console.log('=== CHANGEMENT FOURNISSEUR ===');
    console.log('Fournisseur sélectionné:', selectedFournisseur);

    const selectedAffaire = this.myFormRegister.get('affaireFilter')?.value;

    if (!selectedAffaire) {
      console.log('❌ Aucune affaire sélectionnée');
      return;
    }

    // Log pour debug
    console.log('Affaire sélectionnée:', selectedAffaire);
    console.log('Type de selectedAffaire:', typeof selectedAffaire);
    console.log('selectedAffaire est un string?', typeof selectedAffaire === 'string');
    console.log('Total BC dans bonCommandes:', this.bonCommandes.length);

    // Déterminer le code d'affaire à utiliser
    const affaireCodeToMatch = typeof selectedAffaire === 'string' ? selectedAffaire : (selectedAffaire.code || selectedAffaire);
    console.log('Code affaire à matcher:', affaireCodeToMatch);

    // Filtrer par affaire - essayer plusieurs formats
    let filtered = this.bonCommandes.filter(bc => {
      // Utiliser le code déterminé plus haut
      const match = (bc.affaireCode || '').trim() === (affaireCodeToMatch || '').trim();

      if (!match) {
        console.log(`BC #${bc.commande}: "${bc.affaireCode}" !== "${affaireCodeToMatch}"`);
      }

      return match;
    });

    console.log(`Après filtre affaire: ${filtered.length} BC`);

    if (selectedFournisseur && selectedFournisseur.name) {
      console.log(`✅ Application filtre fournisseur: "${selectedFournisseur.name}"`);

      filtered = filtered.filter(bc => {
        const bcFournisseur = (bc.fournisseur || '').trim().toLowerCase();
        const selectedName = selectedFournisseur.name.trim().toLowerCase();
        const match = bcFournisseur === selectedName;

        console.log(`  BC #${bc.commande}: "${bc.fournisseur}" (normalized: "${bcFournisseur}") === "${selectedName}" ? ${match}`);
        return match;
      });

      console.log(`✅ Résultat: ${filtered.length} BC pour le fournisseur ${selectedFournisseur.name}`);
    } else {
      console.log('⭕ Pas de filtre fournisseur - tous les BC de l\'affaire');
    }

    this.filteredBonCommandes = filtered;
    console.log('filteredBonCommandes après traitement:', this.filteredBonCommandes);

    this.myFormRegister.patchValue({
      bonCommande: null,
      nomFournisseur: '',
      refFournisseur: '',
      affaire: null
    });
  }

  onFournisseurClear() {
    console.log('=== CLEAR FOURNISSEUR ===');

    const selectedAffaire = this.myFormRegister.get('affaireFilter')?.value;

    if (selectedAffaire) {
      this.filteredBonCommandes = this.bonCommandes.filter(
        bc => bc.affaireCode === selectedAffaire.code
      );

      console.log(`⭕ Affichage de tous les BC de l'affaire: ${this.filteredBonCommandes.length}`);
    }

    this.myFormRegister.patchValue({
      bonCommande: null,
      nomFournisseur: '',
      refFournisseur: '',
      affaire: null
    });
  }

  private initMyRegisterForm() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const currentDate = `${year}-${month}-${day}`;

    this.myFormRegister = this.formBuilder.group({
      affaireFilter: [null, Validators.required],
      fournisseurFilter: [null],
      affaire: [{value: '', disabled: true}],
      dateReception: [currentDate, Validators.required],
      bonCommande: [[], Validators.required],
      dateBl: ['', Validators.required],
      nomFournisseur: [{value: '', disabled: true}],
      refFournisseur: [{value: '', disabled: true}],
      idAgelio: [''],
      fichiers: this.formBuilder.array([])
    });
  }

  onBonCommandeChange(selectedBonCommande: any) {
    console.log('=== CHANGEMENT BON DE COMMANDE ===');
    console.log('BC sélectionné:', selectedBonCommande);

    if (selectedBonCommande) {
      this.myFormRegister.patchValue({
        nomFournisseur: selectedBonCommande.fournisseur || '',
        refFournisseur: selectedBonCommande.fournisseurId || ''
      });

      if (selectedBonCommande.affaireCode) {
        this.affaireService.getAffaireByCode(selectedBonCommande.affaireCode).subscribe({
          next: (affaire) => {
            console.log('✅ Affaire trouvée:', affaire);
            this.myFormRegister.patchValue({
              affaire: affaire
            });
          },
          error: (err) => {
            console.warn(`⚠️ Aucune affaire trouvée pour le code: ${selectedBonCommande.affaireCode}`);
            this.myFormRegister.patchValue({
              affaire: null
            });
          }
        });
      }

      // Charger les lignes du bon de commande
      this.loadLignesBonCommande(selectedBonCommande);
    } else {
      this.myFormRegister.patchValue({
        affaire: null,
        nomFournisseur: '',
        refFournisseur: ''
      });
      this.lignesBonCommande = [];
    }
  }

  // ========== GESTION DES FICHIERS ==========

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

  private validateFile(file: File): string | null {
    if (file.size > this.MAX_FILE_SIZE) {
      return `Le fichier "${file.name}" est trop volumineux (max: ${this.formatFileSize(this.MAX_FILE_SIZE)})`;
    }
    if (file.size === 0) {
      return `Le fichier "${file.name}" est vide`;
    }

    const extension = '.' + file.name.split('.').pop()?.toLowerCase();
    if (!this.ALLOWED_EXTENSIONS.includes(extension)) {
      return `Le fichier "${file.name}" n'est pas d'un type autorisé`;
    }

    const isDuplicate = this.selectedFiles.some(f =>
      f.name === file.name && f.file.size === file.size
    );
    if (isDuplicate) {
      return `Le fichier "${file.name}" a déjà été sélectionné`;
    }

    const invalidChars = /[<>:"/\\|?*]/;
    if (invalidChars.test(file.name)) {
      return `Le fichier "${file.name}" contient des caractères non autorisés`;
    }

    return null;
  }

  private processFiles(files: File[]): void {
    const remainingSlots = this.MAX_FILES - this.selectedFiles.length;

    if (files.length > remainingSlots) {
      this.notifyService.showError(
        `Vous ne pouvez ajouter que ${remainingSlots} fichier(s) supplémentaire(s)`,
        'Limite de fichiers'
      );
      return;
    }

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

    if (errors.length > 0) {
      this.notifyService.showError(errors.join('\n'), 'Erreurs de validation');
    }

    validFiles.forEach(file => {
      this.selectedFiles.push({
        file,
        name: file.name,
        size: file.size,
        sizeFormatted: this.formatFileSize(file.size),
        generatedName: ''
      });
    });

    if (validFiles.length > 0) {
      this.recalculateGeneratedNames();
      this.notifyService.showSuccess(
        `${validFiles.length} fichier(s) ajouté(s) avec succès`,
        'Fichiers ajoutés'
      );
    }
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;
    const files = Array.from(input.files);
    this.processFiles(files);
    input.value = '';
  }

  onDragEnter(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragCounter++;
    if (this.canAddMoreFiles) {
      this.isDragOver = true;
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragCounter--;
    if (this.dragCounter === 0) {
      this.isDragOver = false;
    }
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
    this.dragCounter = 0;

    if (!this.canAddMoreFiles) {
      this.notifyService.showWarning('Limite de fichiers atteinte', 'Limite atteinte');
      return;
    }

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.processFiles(Array.from(files));
    }
  }

  removeFile(index: number): void {
    if (index >= 0 && index < this.selectedFiles.length) {
      const fileName = this.selectedFiles[index].name;
      this.selectedFiles.splice(index, 1);
      this.recalculateGeneratedNames();
      this.notifyService.showInfo(`Fichier "${fileName}" supprimé`, 'Fichier supprimé');
    }
  }

  clearAllFiles(): void {
    if (this.selectedFiles.length === 0) return;
    this.selectedFiles = [];
    this.recalculateGeneratedNames();
    this.notifyService.showInfo('Tous les fichiers ont été supprimés', 'Fichiers supprimés');
  }

  // ========== SOUMISSION ==========

  onRegisterReception() {
    console.log("=== DÉBUT onRegisterReception ===");

    if (this.myFormRegister.invalid) {
      console.log('Formulaire invalide:', this.getInvalidControls());
      this.notifyService.showError('Veuillez remplir tous les champs obligatoires', 'Formulaire invalide');
      return;
    }

    this.isSubmitting = true;

    try {
      const formValue = this.myFormRegister.value;
      const selectedBonCommande = formValue.bonCommande;
      const affaire = formValue.affaire;
      const affaireCode = affaire?.affaire || selectedBonCommande?.affaireCode;

      if (!selectedBonCommande || !affaireCode) {
        this.notifyService.showError('Données manquantes', 'Erreur');
        this.isSubmitting = false;
        return;
      }

      const payload = {
        affaireId: affaireCode,
        commandeCode: selectedBonCommande?.commande,
        dateReception: this.formatDateForBackend(formValue.dateReception),
        referenceBl: formValue.idAgelio || selectedBonCommande?.commande || selectedBonCommande?.votreReference,
        dateBl: this.formatDateForBackend(formValue.dateBl),
        refFournisseur: selectedBonCommande?.fournisseurId,
        nomFournisseur: formValue.nomFournisseur || '',
        idAgelio: formValue.idAgelio ? formValue.idAgelio : null,
        statut: 'Brouillon'
      };

      this.receptionService.addReception(payload).subscribe({
        next: response => {
          console.log("Réception créée avec succès:", response);
          this.createdReceptionId = response.id;

          if (this.hasValidFilesForUpload() && response.id) {
            this.uploadFiles(response.id);
          } else {
            this.completeSubmission();
          }
        },
        error: err => {
          console.error("Erreur lors de l'enregistrement:", err);
          this.isSubmitting = false;
          this.notifyService.showError(
            err.error?.message || 'Erreur lors de l\'enregistrement',
            'Erreur'
          );
        }
      });

    } catch (error) {
      console.error("Erreur inattendue:", error);
      this.isSubmitting = false;
      this.notifyService.showError('Erreur inattendue', 'Erreur');
    }
  }

  uploadFiles(receptionId: number) {
    if (!this.selectedFiles || this.selectedFiles.length === 0) {
      this.completeSubmission();
      return;
    }

    const files: File[] = this.selectedFiles
      .map(iFile => iFile.file)
      .filter(file => file instanceof File);

    if (files.length === 0) {
      this.completeSubmission();
      return;
    }

    this.receptionService.uploadFilesForReception(receptionId, files).subscribe({
      next: (response) => {
        console.log("Upload réussi:", response);
        this.notifyService.showSuccess(
          `Réception créée avec ${files.length} fichier(s) uploadé(s)`,
          'Upload terminé'
        );
        this.completeSubmission();
      },
      error: (err) => {
        console.error("Erreur upload:", err);
        this.notifyService.showError(
          err.error?.message || 'Erreur lors de l\'upload',
          'Erreur'
        );
        this.completeSubmission();
      }
    });
  }

  private completeSubmission() {
    this.notifyService.showSuccess('Réception ajoutée avec succès !', 'Ajout réussi');
    this.ajoutEffectue.emit();
    this.closebutton.nativeElement.click();
    this.resetForms();
    this.isSubmitting = false;

    if (this.createdReceptionId) {
      this.router.navigate(['/receptions', this.createdReceptionId, 'add-ligne-reception']);
    }
  }

  private hasValidFilesForUpload(): boolean {
    if (!this.selectedFiles || this.selectedFiles.length === 0) {
      return false;
    }
    return this.selectedFiles.some(iFile =>
      iFile && iFile.file && iFile.file instanceof File
    );
  }

  // ========== UTILITAIRES ==========

  private formatDateForBackend(dateValue: any): string {
    if (!dateValue) return null;

    try {
      let dateString: string;

      // Si c'est déjà une Date
      if (dateValue instanceof Date) {
        // ✅ CORRECTION: Utiliser les méthodes locales au lieu de toISOString()
        const year = dateValue.getFullYear();
        const month = String(dateValue.getMonth() + 1).padStart(2, '0');
        const day = String(dateValue.getDate()).padStart(2, '0');
        dateString = `${year}-${month}-${day}`;
      }
      // Si c'est une string au format YYYY-MM-DD
      else if (typeof dateValue === 'string') {
        // Si déjà au format ISO avec timestamp, retourner tel quel
        if (dateValue.includes('T')) {
          return dateValue;
        }
        dateString = dateValue;
      }
      else {
        const d = new Date(dateValue);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        dateString = `${year}-${month}-${day}`;
      }

      // ✅ Utiliser midi pour éviter les problèmes de timezone
      return `${dateString}T12:00:00`;

    } catch (error) {
      console.error("Erreur formatage date:", error);
      return null;
    }
  }


  private formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  private getInvalidControls(): string[] {
    const invalidControls: string[] = [];
    const controls = this.myFormRegister.controls;
    for (const name in controls) {
      if (controls[name].invalid) {
        invalidControls.push(name);
      }
    }
    return invalidControls;
  }

  getFileIconClass(filename: string): string {
    const ext = filename.split('.').pop()?.toLowerCase();
    switch(ext) {
      case 'pdf': return 'fa-file-pdf-o text-danger';
      case 'xls':
      case 'xlsx': return 'fa-file-excel-o text-success';
      case 'png':
      case 'jpg':
      case 'jpeg': return 'fa-file-image-o text-primary';
      default: return 'fa-file-o text-secondary';
    }
  }

  getAffaireDisplay(): string {
    const affaire = this.myFormRegister.get('affaire')?.value;
    if (affaire) {
      return `${affaire.affaire} - ${affaire.libelle}`;
    }
    return '';
  }

  // Calculer le montant total des lignes du BC
  calculateTotalBC(): number {
    if (!this.lignesBonCommande || this.lignesBonCommande.length === 0) {
      return 0;
    }
    return this.lignesBonCommande.reduce((sum, ligne) => {
      return sum + (ligne.montant || 0);
    }, 0);
  }

  getTotalFilesSize(): string {
    const totalBytes = this.selectedFiles.reduce((sum, file) => sum + file.size, 0);
    return this.formatFileSize(totalBytes);
  }

  resetForms() {
    this.myFormRegister.reset();
    this.selectedFiles = [];
    this.lignesBonCommande = [];
    this.isSubmitting = false;
    this.currentStep = 1;
    this.initMyRegisterForm();
  }

  get canAddMoreFiles(): boolean {
    return this.selectedFiles.length < this.MAX_FILES;
  }

  get remainingFilesCount(): number {
    return this.MAX_FILES - this.selectedFiles.length;
  }

  get fichiers(): FormArray {
    return this.myFormRegister.get('fichiers') as FormArray;
  }
}
