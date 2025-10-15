import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {IConsommation} from "../../../../interfaces/iconsommation";
import {ConsommationService} from "../../../services/consommation.service";
import {Affaire} from "../../../../interfaces/iaffaire";
import {AffaireServiceService} from "../../../services/affaire-service.service";
import {LigneConsommationService} from "../../../services/ligne-consommation.service";
import {ILigneConsommation} from "../../../../interfaces/iligneconsommation";
import {NotificationService} from "../../../services/notification.service";
import {IArticleStock} from "../../../../interfaces/iarticle-stock";
import {UserProfileService} from "../../../services/user-profile.service";

interface ArticleAvecEdition extends IArticleStock {
  editing?: boolean;
  quantiteToAdd?: number;
  isValidating?: boolean;
  validation?: { valide: boolean; message: string };
}

interface LigneAvecEdition extends ILigneConsommation {
  editing?: boolean;
  originalQuantite?: number;
  isValidating?: boolean;
  validation?: { valide: boolean; message: string };
}

interface UserData {
  id?: number;
  login?: string;
  roles?: string[];
  acces?: string[];
}

@Component({
  selector: 'app-ligne-consommation',
  templateUrl: './ligne-consommation.component.html',
  styleUrls: ['./ligne-consommation.component.css']
})
export class LigneConsommationComponent implements OnInit {
  selectedConsommation!: IConsommation;
  affaireByConsommation!: Affaire;

  currentUser: UserData | null = null;
  isLoadingUser = true;
  canEdit = true;
  statusMessage = '';

  articlesDisponibles: ArticleAvecEdition[] = [];
  existingLignes: LigneAvecEdition[] = [];

  searchTerm = '';
  pfiltre: any;
  isLoading = false;
  isLoadingArticles = false;

  constructor(
    private consommationService: ConsommationService,
    private affaireService: AffaireServiceService,
    private ligneConsommationService: LigneConsommationService,
    private userProfileService: UserProfileService,
    private notifyService: NotificationService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
  }

  loadCurrentUser() {
    this.isLoadingUser = true;
    this.userProfileService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUser = {
          id: user.id,
          login: user.login,
          roles: user.roles ? Array.from(user.roles) : [],
          acces: user.acces ? user.acces.map((access: any) => access.code) : []
        };
        this.isLoadingUser = false;
        this.loadConsommationData();
      },
      error: (error) => {
        console.error('Erreur chargement utilisateur:', error);
        this.isLoadingUser = false;
        this.loadConsommationData();
      }
    });
  }

  loadConsommationData() {
    this.route.paramMap.subscribe(params => {
      const consommationId = params.get('id');
      this.consommationService.getConsommationById(consommationId).subscribe({
        next: data => {
          this.selectedConsommation = data;
          console.log("Consommation sélectionnée => ", this.selectedConsommation);
          console.log("Code affaire:", this.selectedConsommation.affaireCode);
          console.log("ID affaire:", this.selectedConsommation.affaireId);

          this.checkConsommationPermissions();
          this.loadAffaireData();
          this.loadExistingLignes(Number(consommationId!));
        },
        error: err => {
          console.error("Erreur chargement consommation", err);
          this.notifyService.showError('Erreur lors du chargement de la consommation', 'Erreur');
        }
      });
    });
  }

  checkConsommationPermissions() {
    if (!this.currentUser) {
      this.canEdit = false;
      this.statusMessage = 'Utilisateur non authentifié';
      return;
    }

    const canModify = this.currentUser.roles?.includes('ADMIN') ||
      this.currentUser.roles?.includes('MANAGER') ||
      this.currentUser.acces?.includes('consommation');

    const isOwnConsommation = this.selectedConsommation.userLogin === this.currentUser.login?.toString();

    if (!canModify && !isOwnConsommation) {
      this.canEdit = false;
      this.statusMessage = 'Vous n\'avez pas les permissions pour modifier cette consommation';
      this.notifyService.showWarning(
        'Vous n\'avez pas les permissions nécessaires pour modifier cette consommation',
        'Accès restreint'
      );
    }
  }

  loadAffaireData() {
    const affaireCode = this.selectedConsommation.affaireCode;

    if (!affaireCode) {
      console.error('Code affaire manquant dans la consommation');
      this.notifyService.showError('Code affaire manquant', 'Erreur');
      return;
    }

    this.affaireService.getAffaireByCode(affaireCode).subscribe({
      next: data => {
        this.affaireByConsommation = data;
        console.log("Affaire récupérée => ", this.affaireByConsommation);
        this.loadArticlesDisponibles(affaireCode);
      },
      error: err => {
        console.error("Erreur chargement affaire", err);
        console.log("Tentative avec l'ID numérique...");

        if (this.selectedConsommation.affaireId) {
          this.affaireService.getAffaireById(this.selectedConsommation.affaireId).subscribe({
            next: data => {
              this.affaireByConsommation = data;
              console.log("Affaire récupérée par ID => ", this.affaireByConsommation);
              this.loadArticlesDisponibles(data.affaire || affaireCode);
            },
            error: err2 => {
              console.error("Erreur chargement affaire par ID", err2);
              this.notifyService.showError('Erreur lors du chargement de l\'affaire', 'Erreur');
              this.loadArticlesDisponibles(affaireCode);
            }
          });
        } else {
          this.notifyService.showError('Erreur lors du chargement de l\'affaire', 'Erreur');
          this.loadArticlesDisponibles(affaireCode);
        }
      }
    });
  }

  loadExistingLignes(consommationId: number) {
    this.ligneConsommationService.getLignesConsommationByConsommationId(consommationId).subscribe({
      next: data => {
        this.existingLignes = data.map(ligne => ({
          ...ligne,
          editing: false,
          originalQuantite: ligne.quantite,
          isValidating: false,
          validation: undefined
        }));
        console.log("Lignes existantes => ", this.existingLignes);
      },
      error: error => {
        console.error("Erreur chargement lignes existantes", error);
      }
    });
  }

  loadArticlesDisponibles(affaireCode: string) {
    this.isLoadingArticles = true;

    this.consommationService.getArticlesDisponiblesByCode(affaireCode).subscribe({
      next: data => {
        const referencesExistantes = this.existingLignes.map(l => l.referenceArticle);
        this.articlesDisponibles = data
          .filter(article => !referencesExistantes.includes(article.referenceArticle))
          .map(article => ({
            ...article,
            editing: false,
            quantiteToAdd: 1,
            isValidating: false,
            validation: undefined
          }));

        console.log("Articles disponibles => ", this.articlesDisponibles);
        this.isLoadingArticles = false;

        if (data.length === 0) {
          this.notifyService.showInfo(
            'Aucun article en stock pour cette affaire. Vérifiez que des réceptions ont été effectuées.',
            'Stock vide'
          );
        }
      },
      error: error => {
        console.error("Erreur chargement articles disponibles", error);

        if (this.selectedConsommation.affaireId) {
          console.log("Tentative avec l'endpoint numérique...");
          this.consommationService.getArticlesDisponibles(this.selectedConsommation.affaireId).subscribe({
            next: data => {
              const referencesExistantes = this.existingLignes.map(l => l.referenceArticle);
              this.articlesDisponibles = data
                .filter(article => !referencesExistantes.includes(article.referenceArticle))
                .map(article => ({
                  ...article,
                  editing: false,
                  quantiteToAdd: 1,
                  isValidating: false,
                  validation: undefined
                }));

              console.log("Articles disponibles (par ID) => ", this.articlesDisponibles);
              this.isLoadingArticles = false;
            },
            error: error2 => {
              console.error("Erreur double échec chargement articles", error2);
              this.isLoadingArticles = false;
            }
          });
        } else {
          this.isLoadingArticles = false;
        }
      }
    });
  }

  filteredArticles(): ArticleAvecEdition[] {
    if (!this.pfiltre) return this.articlesDisponibles;
    const q = this.pfiltre.toLowerCase();
    return this.articlesDisponibles.filter(a =>
      a.referenceArticle.toLowerCase().includes(q) ||
      a.designationArticle.toLowerCase().includes(q)
    );
  }

  startAddArticle(article: ArticleAvecEdition, index: number) {
    if (!this.canEdit) {
      this.notifyService.showError('Vous n\'avez pas les permissions pour ajouter des articles', 'Modification interdite');
      return;
    }

    if (article.stockDisponible <= 0) {
      this.notifyService.showWarning(
        `Aucun stock disponible pour l'article ${article.referenceArticle}`,
        'Stock épuisé'
      );
      return;
    }

    article.editing = true;
    article.quantiteToAdd = Math.min(1, article.stockDisponible);
    article.validation = undefined;
  }

  confirmAddArticle(article: ArticleAvecEdition, index: number) {
    if (!article.quantiteToAdd || article.quantiteToAdd <= 0) {
      this.notifyService.showError('Veuillez saisir une quantité valide', 'Quantité');
      return;
    }

    if (article.quantiteToAdd > article.stockDisponible) {
      this.notifyService.showError(
        `Stock insuffisant ! Maximum disponible: ${article.stockDisponible} ${article.unite}`,
        'Stock'
      );
      return;
    }

    article.isValidating = true;

    const ligne: ILigneConsommation = {
      quantite: article.quantiteToAdd,
      designationArticle: article.designationArticle,
      unite: article.unite,
      referenceArticle: article.referenceArticle,
      familleStatistique1: undefined,
      familleStatistique2: undefined,
      familleStatistique3: undefined,
      familleStatistique4: undefined
    };

    this.ligneConsommationService.addLigneConsommation([ligne], this.selectedConsommation.id).subscribe({
      next: () => {
        this.notifyService.showSuccess(`Article ${article.referenceArticle} ajouté avec succès`, 'Ajout');

        article.editing = false;
        article.quantiteToAdd = 1;
        article.validation = undefined;
        article.isValidating = false;

        this.loadExistingLignes(this.selectedConsommation.id);
        this.loadArticlesDisponibles(this.selectedConsommation.affaireCode);
      },
      error: error => {
        console.error("Erreur ajout article", error);
        let errorMessage = 'Erreur lors de l\'ajout de l\'article';
        if (error.error && error.error.message) {
          errorMessage = error.error.message;
        }
        this.notifyService.showError(errorMessage, 'Erreur');
        article.isValidating = false;
      }
    });
  }

  cancelAddArticle(article: ArticleAvecEdition) {
    article.editing = false;
    article.quantiteToAdd = 1;
    article.validation = undefined;
  }

  startEditLigne(index: number) {
    if (!this.canEdit) {
      this.notifyService.showError('Vous n\'avez pas les permissions pour modifier cette ligne', 'Modification interdite');
      return;
    }

    const ligne = this.existingLignes[index];
    ligne.editing = true;
    ligne.originalQuantite = ligne.quantite;
    ligne.validation = undefined;
  }

  confirmEditLigne(index: number) {
    const ligne = this.existingLignes[index];

    if (!ligne.quantite || ligne.quantite <= 0) {
      this.notifyService.showError('Veuillez saisir une quantité valide', 'Quantité');
      return;
    }

    const articleStock = this.articlesDisponibles.find(a => a.referenceArticle === ligne.referenceArticle);
    const stockTotal = articleStock ? articleStock.stockDisponible + ligne.originalQuantite! : ligne.originalQuantite!;

    if (ligne.quantite > stockTotal) {
      this.notifyService.showError(
        `Stock insuffisant ! Maximum disponible: ${stockTotal} ${ligne.unite}`,
        'Stock'
      );
      return;
    }

    ligne.isValidating = true;

    this.ligneConsommationService.updateLigneConsommation(ligne.id!, ligne).subscribe({
      next: () => {
        this.notifyService.showSuccess(`Ligne ${ligne.referenceArticle} mise à jour avec succès`, 'Modification');

        ligne.editing = false;
        ligne.originalQuantite = ligne.quantite;
        ligne.validation = undefined;
        ligne.isValidating = false;

        this.loadArticlesDisponibles(this.selectedConsommation.affaireCode);
      },
      error: error => {
        console.error("Erreur modification ligne", error);
        let errorMessage = 'Erreur lors de la modification';
        if (error.error && error.error.message) {
          errorMessage = error.error.message;
        }
        this.notifyService.showError(errorMessage, 'Erreur');
        ligne.isValidating = false;
      }
    });
  }

  cancelEditLigne(index: number) {
    const ligne = this.existingLignes[index];
    ligne.editing = false;
    ligne.quantite = ligne.originalQuantite!;
    ligne.validation = undefined;
  }

  deleteLigneConsommation(ligneId: number, referenceArticle: string) {
    if (!this.canEdit) {
      this.notifyService.showError('Vous n\'avez pas les permissions pour supprimer cette ligne', 'Modification interdite');
      return;
    }

    const ligne = this.existingLignes.find(l => l.id === ligneId);
    if (ligne) {
      ligne.isValidating = true;
    }

    this.ligneConsommationService.deleteLigneConsommation(ligneId).subscribe({
      next: () => {
        if (this.existingLignes.length <= 1) {
          this.notifyService.showSuccess(
            `Dernière ligne "${referenceArticle}" supprimée. La consommation est maintenant vide.`,
            'Suppression - Consommation vide'
          );
        } else {
          this.notifyService.showSuccess(
            `Ligne "${referenceArticle}" supprimée avec succès`,
            'Suppression'
          );
        }

        this.loadExistingLignes(this.selectedConsommation.id);
        this.loadArticlesDisponibles(this.selectedConsommation.affaireCode);
      },
      error: error => {
        console.error("Erreur suppression ligne", error);

        let errorMessage = 'Erreur lors de la suppression';
        if (error.error && error.error.message) {
          errorMessage = error.error.message;
        }

        this.notifyService.showError(errorMessage, 'Erreur');

        if (ligne) {
          ligne.isValidating = false;
        }
      }
    });
  }

  canDeleteLigne(ligneId: number): boolean {
    if (!this.canEdit) return false;
    const ligne = this.existingLignes.find(l => l.id === ligneId);
    return ligne ? !ligne.isValidating : false;
  }

  getDeleteTooltip(ligneId: number): string {
    if (!this.canEdit) {
      return 'Modification non autorisée pour votre rôle';
    }

    const ligne = this.existingLignes.find(l => l.id === ligneId);
    if (ligne?.isValidating) {
      return 'Suppression en cours...';
    }

    if (this.existingLignes.length <= 1) {
      return '⚠️ Supprimer la dernière ligne (consommation sera vide)';
    }

    return 'Supprimer cette ligne';
  }

  getTotalQuantityConsumed(): string {
    if (!this.existingLignes || this.existingLignes.length === 0) {
      return '0';
    }
    const total = this.existingLignes.reduce((sum, ligne) => sum + (ligne.quantite || 0), 0);
    return total.toFixed(2);
  }

  getDistinctUnits(): number {
    if (!this.existingLignes || this.existingLignes.length === 0) {
      return 0;
    }
    const distinctUnits = [...new Set(this.existingLignes.map(ligne => ligne.unite))];
    return distinctUnits.length;
  }

  retournerALaListe() {
    this.router.navigate(['/consommations']);
  }

  trackByReference(_: number, item: IArticleStock): string {
    return item.referenceArticle;
  }

  trackByLigneId(_: number, item: ILigneConsommation): string {
    return item.id?.toString() || item.referenceArticle;
  }

  getCurrentUserDisplayName(): string {
    if (this.currentUser) {
      return `Utilisateur: ${this.currentUser.login}`;
    }
    return 'Utilisateur non connecté';
  }

  getUserRoleDescription(): string {
    if (!this.currentUser?.roles) return 'Aucun rôle';
    if (this.currentUser.roles.includes('ADMIN')) return 'Administrateur';
    if (this.currentUser.roles.includes('MANAGER')) return 'Manager';
    if (this.currentUser.roles.includes('CONSULTEUR')) return 'Consulteur';
    return 'Utilisateur';
  }
}
