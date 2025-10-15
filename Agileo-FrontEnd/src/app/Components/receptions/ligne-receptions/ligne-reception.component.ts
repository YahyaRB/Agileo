import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {IReception} from "../../../../interfaces/ireception";
import {ReceptionService, ArticleDisponible, ValidationQuantite} from "../../../services/reception.service";
import {ILigneReception} from "../../../../interfaces/ilignereception";
import {LigneReceptionService} from "../../../services/ligne-reception.service";
import {NotificationService} from "../../../services/notification.service";
import {UserData} from "../../../../interfaces/iuser";
import {UserProfileService} from "../../../services/user-profile.service";

// ✅ Interface étendue pour les articles avec états d'édition
interface ArticleAvecEdition extends ArticleDisponible {
  editing?: boolean;
  quantiteToAdd?: number;
  isValidating?: boolean;
  validation?: ValidationQuantite;
}

// ✅ Interface étendue pour les lignes avec états d'édition
interface LigneAvecEdition extends ILigneReception {
  editing?: boolean;
  originalQuantite?: number;
  isValidating?: boolean;
  validation?: ValidationQuantite;
}

@Component({
  selector: 'app-ligne-reception',
  templateUrl: './ligne-reception.component.html',
  styleUrls: ['./ligne-reception.component.css']
})
export class LigneReceptionComponent implements OnInit {
  selectedReception!: IReception;

  // ✅ Types corrigés
  articlesDisponibles: ArticleAvecEdition[] = [];
  existingLignes: LigneAvecEdition[] = [];

  searchTerm = '';
  pfiltre: any;
  currentUser: UserData | null = null;
  isLoading = false;
  isLoadingArticles = false;

  canEdit = true;
  statusMessage = '';
  private isLoadingUser: boolean;

  constructor(
    private receptionService: ReceptionService,
    private ligneReceptionService: LigneReceptionService,
    private userProfileService: UserProfileService,
    private notifyService: NotificationService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.isLoading = true;
    this.isLoadingArticles = false;
    this.canEdit = true;
    this.statusMessage = '';
    this.loadCurrentUser();
    this.articlesDisponibles = [];
    this.existingLignes = [];

    this.route.paramMap.subscribe(params => {
      const receptionId = params.get('id');

      if (!receptionId) {
        this.notifyService.showError('ID de réception manquant', 'Erreur');
        this.router.navigate(['/receptions']);
        return;
      }

      console.log('Starting to load reception with ID:', receptionId);
      this.receptionService.getReceptionById(receptionId).subscribe({
        next: data => {
          this.selectedReception = data;
          console.log("Réception sélectionnée => ", this.selectedReception);

          this.checkReceptionStatus();
          this.loadExistingLignes(this.selectedReception.id!);
        },
        error: err => {
          console.error("Erreur chargement réception", err);
          this.notifyService.showError('Erreur lors du chargement de la réception', 'Erreur');
          this.isLoading = false;
        }
      });
    });
  }
  loadCurrentUser() {
    this.isLoadingUser = true;
    this.userProfileService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUser = {
          id: user.id,
          login: user.login,
          roles: user.roles ? Array.from(user.roles).map((role: any) =>
            typeof role === 'string' ? role : role.name) : [],
          acces: user.acces ? user.acces.map((access: any) => access.code) : []
        };
        this.isLoadingUser = false;


      },
      error: (error) => {
        console.error('Erreur chargement utilisateur:', error);
        this.isLoadingUser = false;
      }
    });
  }
  checkReceptionStatus() {
    if (this.selectedReception.statut === 'Envoyé') {
      this.canEdit = false;
      this.statusMessage = 'Cette réception a été envoyée et ne peut plus être modifiée.';
      this.notifyService.showWarning(
        'Cette réception a été envoyée. Aucune modification n\'est possible.',
        'Réception envoyée'
      );
    } else {
      this.canEdit = true;
      this.statusMessage = '';
    }
  }

  loadExistingLignes(receptionId: number) {
    this.ligneReceptionService.getLignesReceptionByReceptionId(receptionId).subscribe({
      next: data => {
        this.existingLignes = data.map(ligne => ({
          ...ligne,
          editing: false,
          originalQuantite: ligne.quantite,
          isValidating: false,
          validation: undefined
        }));
        console.log("Lignes de réception existantes => ", this.existingLignes);

        this.loadArticlesDisponibles();
      },
      error: error => {
        console.error("Erreur chargement lignes existantes", error);
        this.loadArticlesDisponibles();
      }
    });
  }

  // loadArticlesDisponibles() {
  //   if (!this.selectedReception?.id) {
  //     this.isLoadingArticles = false;
  //     this.isLoading = false;
  //     return;
  //   }
  //
  //   this.isLoadingArticles = true;
  //
  //   // Charger les articles depuis ArticleReception (Livraison_ERP)
  //   this.receptionService.getArticlesDisponibles(this.selectedReception.id).subscribe({
  //     next: data => {
  //       this.articlesDisponibles = data.map(article => ({
  //         ...article,
  //         editing: false,
  //         quantiteToAdd: 1,
  //         isValidating: false,
  //         validation: undefined
  //       }));
  //       console.log("Articles disponibles depuis ArticleReception => ", data);
  //
  //       this.isLoadingArticles = false;
  //       this.isLoading = false;
  //
  //       if (data.length === 0) {
  //         this.notifyService.showInfo(
  //           'Aucun article disponible dans le bon de commande pour cette réception',
  //           'Information'
  //         );
  //       }
  //     },
  //     error: error => {
  //       console.error("Erreur chargement articles disponibles", error);
  //       this.notifyService.showError('Erreur lors du chargement des articles disponibles', 'Erreur');
  //
  //       this.isLoadingArticles = false;
  //       this.isLoading = false;
  //     }
  //   });
  // }
  //version 2 de loadArticlesDisponibles()
  loadArticlesDisponibles() {
    if (!this.selectedReception?.id) {
      this.isLoadingArticles = false;
      this.isLoading = false;
      return;
    }

    this.isLoadingArticles = true;

    // Charger les articles depuis ArticleReception (Livraison_ERP)
    this.receptionService.getArticlesDisponibles(this.selectedReception.id).subscribe({
      next: data => {
        // ✅ FILTRER: Exclure les articles déjà dans les lignes existantes
        const referencesExistantes = this.existingLignes.map(l => l.referenceArticle);
        const articlesFiltres = data.filter(article =>
          !referencesExistantes.includes(article.reference)
        );

        this.articlesDisponibles = articlesFiltres.map(article => ({
          ...article,
          editing: false,
          quantiteToAdd: 1,
          isValidating: false,
          validation: undefined
        }));

        console.log("Articles disponibles filtrés => ", this.articlesDisponibles);

        this.isLoadingArticles = false;
        this.isLoading = false;

        if (data.length === 0) {
          this.notifyService.showInfo(
            'Aucun article disponible dans le bon de commande pour cette réception',
            'Information'
          );
        }
      },
      error: error => {
        console.error("Erreur chargement articles disponibles", error);
        this.notifyService.showError('Erreur lors du chargement des articles disponibles', 'Erreur');

        this.isLoadingArticles = false;
        this.isLoading = false;
      }
    });
  }


  filteredArticles(): ArticleAvecEdition[] {
    if (!this.searchTerm && !this.pfiltre) return this.articlesDisponibles;
    const q = (this.searchTerm || this.pfiltre || '').toLowerCase();
    return this.articlesDisponibles.filter(a =>
      a.reference.toLowerCase().includes(q) || a.designation.toLowerCase().includes(q)
    );
  }

  // ✅ Commencer l'ajout d'un article
  startAddArticle(article: ArticleAvecEdition, index: number) {
    if (!this.canEdit) {
      this.notifyService.showError('Impossible d\'ajouter des articles à une réception envoyée', 'Modification interdite');
      return;
    }

    // Vérifier si déjà en base
    const existsInDb = this.existingLignes.find(l => l.referenceArticle === article.reference);
    if (existsInDb) {
      this.notifyService.showWarning(
        `L'article ${article.reference} est déjà enregistré dans cette réception`,
        'Article existant'
      );
      return;
    }

    // Vérifier la quantité disponible
    if (article.quantiteDisponible <= 0) {
      this.notifyService.showWarning(
        `Aucune quantité disponible pour l'article ${article.reference}`,
        'Quantité insuffisante'
      );
      return;
    }

    // Activer le mode édition
    article.editing = true;
    article.quantiteToAdd = Math.min(1, article.quantiteDisponible);
    article.validation = undefined;
  }

  // ✅ Confirmer l'ajout d'un article
  // async confirmAddArticle(article: ArticleAvecEdition, index: number) {
  //   if (!article.quantiteToAdd || article.quantiteToAdd <= 0) {
  //     this.notifyService.showError('Veuillez saisir une quantité valide', 'Quantité');
  //     return;
  //   }
  //
  //   if (article.quantiteToAdd > article.quantiteDisponible) {
  //     this.notifyService.showError(
  //       `Quantité demandée (${article.quantiteToAdd}) dépasse la quantité disponible (${article.quantiteDisponible})`,
  //       'Quantité insuffisante'
  //     );
  //     return;
  //   }
  //
  //   // Validation côté backend
  //   article.isValidating = true;
  //
  //   try {
  //     const validation = await this.receptionService.validerQuantiteArticle(
  //       this.selectedReception.id!,
  //       article.reference,
  //       article.quantiteToAdd
  //     ).toPromise();
  //
  //     article.validation = validation;
  //     article.isValidating = false;
  //
  //     if (validation?.valide) {
  //       // Créer la ligne de réception
  //       const ligne: ILigneReception = {
  //         quantite: article.quantiteToAdd,
  //         designationArticle: article.designation,
  //         unite: article.unite,
  //         referenceArticle: article.reference,
  //         familleStatistique1: article.familleStatistique1,
  //         familleStatistique2: article.familleStatistique2,
  //         familleStatistique3: article.familleStatistique3,
  //         familleStatistique4: article.familleStatistique4
  //       };
  //
  //       this.ligneReceptionService.addLigneReception([ligne], this.selectedReception.id!).subscribe({
  //         next: () => {
  //           this.notifyService.showSuccess(`Article ${article.reference} ajouté avec succès`, 'Ajout');
  //
  //           // Réinitialiser l'état
  //           article.editing = false;
  //           article.quantiteToAdd = 1;
  //           article.validation = undefined;
  //
  //           // Recharger les données
  //           this.loadExistingLignes(this.selectedReception.id!);
  //         },
  //         error: error => {
  //           console.error("Erreur ajout article", error);
  //           let errorMessage = 'Erreur lors de l\'ajout de l\'article';
  //           if (error.error && error.error.message) {
  //             errorMessage = error.error.message;
  //           }
  //           this.notifyService.showError(errorMessage, 'Erreur');
  //           article.isValidating = false;
  //         }
  //       });
  //     } else {
  //       this.notifyService.showError(validation?.message || 'Quantité invalide', 'Validation');
  //     }
  //   } catch (error) {
  //     console.error('Erreur validation:', error);
  //     article.isValidating = false;
  //     this.notifyService.showError('Erreur lors de la validation de la quantité', 'Erreur');
  //   }
  // }
  //version 2 de async confirmAddArticle(article: ArticleAvecEdition, index: number)
  async confirmAddArticle(article: ArticleAvecEdition, index: number) {
    if (!article.quantiteToAdd || article.quantiteToAdd <= 0) {
      this.notifyService.showError('Veuillez saisir une quantité valide', 'Quantité');
      return;
    }

    if (article.quantiteToAdd > article.quantiteDisponible) {
      this.notifyService.showError(
        `Quantité demandée (${article.quantiteToAdd}) dépasse la quantité disponible (${article.quantiteDisponible})`,
        'Quantité insuffisante'
      );
      return;
    }

    // Validation côté backend
    article.isValidating = true;

    try {
      const validation = await this.receptionService.validerQuantiteArticle(
        this.selectedReception.id!,
        article.reference,
        article.quantiteToAdd
      ).toPromise();

      article.validation = validation;
      article.isValidating = false;

      if (validation?.valide) {
        // Créer la ligne de réception
        const ligne: ILigneReception = {
          quantite: article.quantiteToAdd,
          designationArticle: article.designation,
          unite: article.unite,
          referenceArticle: article.reference,
          familleStatistique1: article.familleStatistique1,
          familleStatistique2: article.familleStatistique2,
          familleStatistique3: article.familleStatistique3,
          familleStatistique4: article.familleStatistique4
        };

        this.ligneReceptionService.addLigneReception([ligne], this.selectedReception.id!).subscribe({
          next: () => {
            this.notifyService.showSuccess(`Article ${article.reference} ajouté avec succès`, 'Ajout');

            // ✅ NOUVEAU: Supprimer l'article du tableau des articles disponibles
            this.articlesDisponibles = this.articlesDisponibles.filter(a =>
              a.reference !== article.reference
            );

            // Réinitialiser l'état
            article.editing = false;
            article.quantiteToAdd = 1;
            article.validation = undefined;

            // Recharger les lignes existantes
            this.loadExistingLignes(this.selectedReception.id!);
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
      } else {
        this.notifyService.showError(validation?.message || 'Quantité invalide', 'Validation');
      }
    } catch (error) {
      console.error('Erreur validation:', error);
      article.isValidating = false;
      this.notifyService.showError('Erreur lors de la validation de la quantité', 'Erreur');
    }
  }

  // ✅ Annuler l'ajout d'un article
  cancelAddArticle(article: ArticleAvecEdition) {
    article.editing = false;
    article.quantiteToAdd = 1;
    article.validation = undefined;
  }

  // ✅ Commencer l'édition d'une ligne existante
  startEditLigne(index: number) {
    if (!this.canEdit) {
      this.notifyService.showError('Impossible de modifier une réception envoyée', 'Modification interdite');
      return;
    }

    const ligne = this.existingLignes[index];
    ligne.editing = true;
    ligne.originalQuantite = ligne.quantite;
    ligne.validation = undefined;
  }

  // ✅ Confirmer l'édition d'une ligne existante
  async confirmEditLigne(index: number) {
    const ligne = this.existingLignes[index];

    if (!ligne.quantite || ligne.quantite <= 0) {
      this.notifyService.showError('Veuillez saisir une quantité valide', 'Quantité');
      return;
    }
    if (ligne.quantite > ligne.reste+ligne.originalQuantite) {
      this.notifyService.showError(
        `Quantité demandée (${ligne.quantite}) dépasse la quantité totale disponible (${ligne.originalQuantite + ligne.reste})`,
        'Quantité insuffisante'
      );
      return;
    }

    // Validation côté backend
    ligne.isValidating = true;

    try {
      const validation = await this.receptionService.validerQuantiteArticle(
        this.selectedReception.id!,
        ligne.referenceArticle,
        ligne.quantite,
        ligne.id
      ).toPromise();

      ligne.validation = validation;
      ligne.isValidating = false;

      if (validation?.valide) {
        // Mettre à jour la ligne
        this.ligneReceptionService.updateLigneReception(ligne.id!, ligne).subscribe({
          next: () => {
            this.notifyService.showSuccess(`Ligne ${ligne.referenceArticle} mise à jour avec succès`, 'Modification');

            // Réinitialiser l'état
            ligne.editing = false;
            ligne.originalQuantite = ligne.quantite;
            ligne.validation = undefined;

            // Recharger les articles disponibles
            // this.loadArticlesDisponibles();
            this.loadExistingLignes(this.selectedReception.id!);
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
      } else {
        this.notifyService.showError(validation?.message || 'Quantité invalide', 'Validation');
      }
    } catch (error) {
      console.error('Erreur validation:', error);
      ligne.isValidating = false;
      this.notifyService.showError('Erreur lors de la validation de la quantité', 'Erreur');
    }
  }

  // ✅ Annuler l'édition d'une ligne existante
  cancelEditLigne(index: number) {
    const ligne = this.existingLignes[index];
    ligne.editing = false;
    ligne.quantite = ligne.originalQuantite!;
    ligne.validation = undefined;
  }

  // ✅ Supprimer une ligne de réception
  deleteLigneReception(ligneId: number, referenceArticle: string) {
    if (!this.canEdit) {
      this.notifyService.showError('Impossible de supprimer des lignes d\'une réception envoyée', 'Modification interdite');
      return;
    }

    // Désactiver les boutons pendant la suppression
    const ligne = this.existingLignes.find(l => l.id === ligneId);
    if (ligne) {
      ligne.isValidating = true;
    }

    this.ligneReceptionService.deleteLigneReception(ligneId).subscribe({
      next: () => {
        if (this.existingLignes.length <= 1) {
          this.notifyService.showSuccess(
            `Dernière ligne "${referenceArticle}" supprimée. La réception est maintenant vide.`,
            'Suppression - Réception vide'
          );
        } else {
          this.notifyService.showSuccess(
            `Ligne "${referenceArticle}" supprimée avec succès`,
            'Suppression'
          );
        }

        // Recharger les données
        this.loadExistingLignes(this.selectedReception.id!);
        this.loadArticlesDisponibles();
      },
      error: error => {
        console.error("Erreur suppression ligne", error);

        let errorMessage = 'Erreur lors de la suppression';
        if (error.error && error.error.message) {
          errorMessage = error.error.message;
        } else if (error.status === 400) {
          errorMessage = 'Cette ligne ne peut pas être supprimée. Vérifiez les contraintes côté serveur.';
        } else if (error.status === 404) {
          errorMessage = 'Ligne non trouvée. Elle a peut-être déjà été supprimée.';
        }

        this.notifyService.showError(errorMessage, 'Erreur');

        // Réactiver les boutons
        if (ligne) {
          ligne.isValidating = false;
        }
      }
    });
  }

  // ✅ Méthodes de tracking pour les listes
  trackByCode(_: number, item: ArticleDisponible) {
    return item.reference;
  }

  trackByLigneId(_: number, item: ILigneReception) {
    return item.id || item.referenceArticle;
  }

  // ✅ Vérifier si une ligne peut être supprimée
  canDeleteLigne(ligneId: number): boolean {
    if (!this.canEdit) return false;
    const ligne = this.existingLignes.find(l => l.id === ligneId);
    return ligne ? !ligne.isValidating : false;
  }

  // ✅ Obtenir le tooltip pour le bouton de suppression
  getDeleteTooltip(ligneId: number): string {
    if (!this.canEdit) {
      return 'Réception envoyée - Modification interdite';
    }
    const ligne = this.existingLignes.find(l => l.id === ligneId);
    if (ligne?.isValidating) {
      return 'Suppression en cours...';
    }
    if (this.existingLignes.length <= 1) {
      return '⚠️ Supprimer la dernière ligne (réception sera vide)';
    }
    return 'Supprimer cette ligne';
  }
  // ✅ AJOUTER cette méthode pour gérer la suppression d'articles

  private updateArticlesDisponiblesAfterDeletion(deletedReference: string) {
    // Recharger les articles disponibles pour réafficher celui qui a été supprimé
    this.receptionService.getArticlesDisponibles(this.selectedReception.id!).subscribe({
      next: data => {
        const referencesExistantes = this.existingLignes.map(l => l.referenceArticle);
        const articlesFiltres = data.filter(article =>
          !referencesExistantes.includes(article.reference)
        );

        this.articlesDisponibles = articlesFiltres.map(article => ({
          ...article,
          editing: false,
          quantiteToAdd: 1,
          isValidating: false,
          validation: undefined
        }));
      },
      error: error => {
        console.error("Erreur rechargement articles après suppression", error);
      }
    });
  }
}
