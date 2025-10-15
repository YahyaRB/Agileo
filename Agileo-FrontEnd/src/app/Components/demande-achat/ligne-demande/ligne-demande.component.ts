import { Component, OnInit, OnDestroy } from '@angular/core';
import { TempDataService } from "../../../services/temp-data.service";
import { Article } from "../../../../interfaces/iarticle";
import { ActivatedRoute, Router } from "@angular/router";
import { IDemandeAchat } from "../../../../interfaces/idemandeAchat";
import { DemandeAchatService } from "../../../services/demande-achat.service";
import { Affaire } from "../../../../interfaces/iaffaire";
import { AffaireServiceService } from "../../../services/affaire-service.service";
import { LigneDemandeAchatService } from "../../../services/ligne-demande-achat.service";
import { NotificationService } from "../../../services/notification.service";
import { ILigneDemande } from "../../../../interfaces/ilignedemande";
import { debounceTime, distinctUntilChanged, Observable, Subject, Subscription } from 'rxjs';
import { PagedResponse } from "../../../../interfaces/PagedResponse";
import {IFamilleStatistique} from "../../../../interfaces/ifamille-statistique";

@Component({
  selector: 'app-ligne-demande',
  templateUrl: './ligne-demande.component.html',
  styleUrls: ['./ligne-demande.component.css']
})
export class LigneDemandeComponent implements OnInit, OnDestroy {

  // ==================== PROPRIÉTÉS PRINCIPALES ====================
  selectedDemandeAchat: IDemandeAchat | null = null;
  affaireByDemandeAchat: Affaire | null = null;
  articles: Article[] = [];
  lignesExistantes: ILigneDemande[] = [];
  familleOptions: IFamilleStatistique[] = [];
  // ==================== RECHERCHE ====================
  searchTerm: string = '';
  private searchSubject = new Subject<string>();

  // ==================== PAGINATION ET TRI ====================
  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;
  sortBy: string = 'ref';
  sortDir: string = 'asc';

  // ==================== FAMILLE ====================
  searchMode: 'general' | 'famille' = 'general';
  selectedFamilleType: number = 1;
  selectedFamilleCode: string = '';

  isLoadingFamilles: boolean = false;

  // ==================== ÉTATS DE CHARGEMENT ====================
  isLoadingDemande = false;
  isLoadingArticles = false;
  isLoadingLignes = false;

  // ==================== GESTION DES ÉDITIONS ====================
  articleEditStates: { [key: number]: boolean } = {};
  articleQuantities: { [key: number]: number } = {};
  ligneEditStates: { [key: string]: boolean } = {};
  ligneQuantities: { [key: string]: number } = {};
  private savingArticles: Set<number> = new Set();

  // ==================== SUBSCRIPTIONS ====================
  private subscriptions: Subscription[] = [];

  constructor(
    private tempService: TempDataService,
    private demandeAchatService: DemandeAchatService,
    private affaireService: AffaireServiceService,
    private ligneDemandeService: LigneDemandeAchatService,
    private notifyService: NotificationService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    console.log('🚀 Initialisation du composant LigneDemandeComponent');
    this.setupSearchDebounce();
    this.initializeComponent();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.searchSubject.complete();
    console.log('🧹 Component détruit et subscriptions nettoyées');
  }

  // ==================== INITIALISATION ====================

  private initializeComponent(): void {
    // ✅ CORRECTION : Charger les familles dès le démarrage
    this.loadAllFamillesStatistiques();
    this.loadDemandeData();
  }

  // ✅ NOUVELLE MÉTHODE : Charger TOUTES les familles depuis le backend
  private loadAllFamillesStatistiques(): void {
    console.log('🔄 Chargement de TOUTES les familles avec désignations, type:', this.selectedFamilleType);
    this.isLoadingFamilles = true;

    // ✅ Utilise la nouvelle méthode avec désignations
    const sub = this.tempService.getFamilleStatistiquesWithDesignation(this.selectedFamilleType).subscribe({
      next: (familles: IFamilleStatistique[]) => {
        this.familleOptions = familles;
        this.isLoadingFamilles = false;

        console.log('✅ Familles chargées avec désignations:', this.familleOptions.length);
        console.log('📋 Exemple:', familles.slice(0, 3));
      },
      error: (error) => {
        console.error('❌ Erreur chargement familles:', error);
        this.isLoadingFamilles = false;
        this.familleOptions = [];

        if (error.status === 404) {
          this.notifyService.showError(
            'Endpoint des familles non trouvé. Vérifiez le backend.',
            'Erreur'
          );
        } else if (error.status === 500) {
          this.notifyService.showError(
            'Erreur serveur lors du chargement des familles',
            'Erreur'
          );
        }
      }
    });

    this.subscriptions.push(sub);
  }


  private loadDemandeData(): void {
    const sub = this.route.paramMap.subscribe(params => {
      const demandeIdParam = params.get('id');
      const demandeId = demandeIdParam ? Number(demandeIdParam) : undefined;

      if (!demandeId || Number.isNaN(demandeId)) {
        console.error('❌ Paramètre id de demande invalide:', demandeIdParam);
        this.notifyService.showError('ID de demande invalide', 'Erreur');
        this.router.navigate(['/demandes-achat']);
        return;
      }

      console.log('🔍 Chargement des détails pour la demande ID:', demandeId);
      this.loadDemandeDetails(demandeId);
    });
    this.subscriptions.push(sub);
  }

  private loadDemandeDetails(demandeId: number): void {
    this.isLoadingDemande = true;

    const sub = this.demandeAchatService.getDemandesAchatById(String(demandeId)).subscribe({
      next: (data) => {
        this.selectedDemandeAchat = data;
        this.isLoadingDemande = false;

        console.log('✅ Demande d\'achat récupérée:', this.selectedDemandeAchat);

        this.loadAffaireByCode();
        this.loadLignesExistantes();
        this.loadArticlesPaginated();
      },
      error: (err) => {
        this.isLoadingDemande = false;
        console.error('❌ Erreur lors du chargement de la demande:', err);
        this.notifyService.showError('Erreur lors du chargement de la demande', 'Erreur');
        this.router.navigate(['/demandes-achat']);
      }
    });
    this.subscriptions.push(sub);
  }

  private loadAffaireByCode(): void {
    if (!this.selectedDemandeAchat?.chantier) {
      console.warn('⚠️ Aucun code chantier disponible');
      return;
    }

    const sub = this.affaireService.getAffaires().subscribe({
      next: (affaires) => {
        const affaireTrouvee = affaires.find(a =>
          a.affaire === this.selectedDemandeAchat?.chantier ||
          a.code === this.selectedDemandeAchat?.chantier
        );

        if (affaireTrouvee) {
          this.affaireByDemandeAchat = affaireTrouvee;
          console.log('✅ Affaire trouvée par code:', this.affaireByDemandeAchat);
        }
      },
      error: (err) => {
        console.error('❌ Erreur lors du chargement des affaires:', err);
      }
    });
    this.subscriptions.push(sub);
  }

  // ==================== CHARGEMENT DES ARTICLES ====================

  loadArticlesPaginated(): void {
    console.log('🔄 Chargement articles avec filtres...');
    console.log('📝 Recherche:', this.searchTerm || '(vide)');
    console.log('🏷️ Famille:', this.selectedFamilleCode || '(aucune)');
    console.log('📄 Page:', this.currentPage, '/', this.totalPages);

    this.isLoadingArticles = true;

    // ✅ SIMPLIFICATION : Un seul appel qui gère TOUS les cas
    const sub = this.tempService.getArticlesFiltered(
      this.searchTerm || undefined,        // Recherche (optionnelle)
      this.selectedFamilleCode || undefined, // Famille (optionnelle)
      this.currentPage,
      this.pageSize,
      this.sortBy,
      this.sortDir
    ).subscribe({
      next: (data: PagedResponse<Article>) => {
        this.articles = data.content || [];
        this.currentPage = data.pageNumber;
        this.totalPages = data.totalPages;
        this.totalElements = data.totalElements;
        this.isLoadingArticles = false;

        console.log(`✅ ${this.articles.length} articles chargés`);
        console.log(`📊 Total: ${this.totalElements} articles`);

        // Afficher le mode actif
        if (this.searchTerm && this.selectedFamilleCode) {
          console.log('🔍 Mode: RECHERCHE + FAMILLE');
        } else if (this.searchTerm) {
          console.log('🔍 Mode: RECHERCHE SEULE');
        } else if (this.selectedFamilleCode) {
          console.log('🏷️ Mode: FAMILLE SEULE');
        } else {
          console.log('📋 Mode: LISTE COMPLÈTE');
        }
      },
      error: (error) => {
        console.error('❌ Erreur chargement articles:', error);
        this.isLoadingArticles = false;
        this.articles = [];
        this.totalElements = 0;

        this.notifyService.showError(
          'Erreur lors du chargement des articles',
          'Erreur'
        );
      }
    });

    this.subscriptions.push(sub);
  }
  // ==================== GESTION DE LA PAGINATION ====================

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadArticlesPaginated();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadArticlesPaginated();
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadArticlesPaginated();
    }
  }

  changePageSize(newSize: number): void {
    this.pageSize = newSize;
    this.currentPage = 0;
    this.loadArticlesPaginated();
  }

  changeSorting(sortBy: string): void {
    if (this.sortBy === sortBy) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = sortBy;
      this.sortDir = 'asc';
    }
    this.currentPage = 0;
    this.loadArticlesPaginated();
  }

  // ==================== GESTION DES RECHERCHES ====================

  onSearchTermChange(): void {
    this.searchSubject.next(this.searchTerm);
  }



  private setupSearchDebounce(): void {
    const sub = this.searchSubject.pipe(
      debounceTime(500),
      distinctUntilChanged()
    ).subscribe(searchTerm => {
      console.log('🔍 Recherche déclenchée:', searchTerm);
      this.currentPage = 0; // ✅ Toujours revenir à la page 1
      this.loadArticlesPaginated(); // ✅ Un seul appel
    });

    this.subscriptions.push(sub);
  }

  onFamilleSearch(): void {
    console.log('🏷️ Filtre famille appliqué:', this.selectedFamilleCode);

    this.currentPage = 0; // ✅ Revenir à la page 1
    this.loadArticlesPaginated(); // ✅ Un seul appel
  }

  onFamilleTypeChange(): void {
    console.log('🔄 Changement de type de famille vers:', this.selectedFamilleType);

    // Réinitialiser la sélection
    this.selectedFamilleCode = '';
    this.searchMode = 'general';

    // ✅ CORRECTION : Recharger les familles depuis le backend
    this.loadAllFamillesStatistiques();

    // Recharger les articles
    this.currentPage = 0;
    this.loadArticlesPaginated();
  }

  clearSearch(): void {
    console.log('🧹 Réinitialisation des filtres');

    this.searchTerm = '';
    this.selectedFamilleCode = '';
    this.currentPage = 0;

    this.loadArticlesPaginated(); // ✅ Un seul appel
  }

  // ✅ SUPPRESSION de extractFamilleOptions() - on ne l'utilise plus !

  // ==================== CHARGEMENT DES LIGNES ====================

  loadLignesExistantes(): Promise<void> {
    return new Promise((resolve) => {
      if (!this.selectedDemandeAchat?.id) {
        this.isLoadingLignes = false;
        resolve();
        return;
      }

      this.isLoadingLignes = true;

      const sub = this.ligneDemandeService.getLignesDemandeByDemandeId(this.selectedDemandeAchat.id).subscribe({
        next: (data) => {
          this.lignesExistantes = data || [];
          this.isLoadingLignes = false;
          this.initializeLigneStates();
          resolve();
        },
        error: () => {
          this.isLoadingLignes = false;
          this.lignesExistantes = [];
          resolve();
        }
      });
      this.subscriptions.push(sub);
    });
  }

  private initializeLigneStates(): void {
    this.ligneEditStates = {};
    this.ligneQuantities = {};

    this.lignesExistantes.forEach(ligne => {
      const key = this.getLigneKey(ligne);
      this.ligneEditStates[key] = false;
      this.ligneQuantities[key] = ligne.qte || ligne.quantite || 0;
    });
  }

  // ==================== GESTION DES ARTICLES - AJOUT ====================

  startEditQuantity(articleId: number): void {
    this.articleEditStates[articleId] = true;
    this.articleQuantities[articleId] = 1;
  }

  cancelEdit(articleId: number): void {
    this.articleEditStates[articleId] = false;
    this.articleQuantities[articleId] = 0;
  }

  isEditing(articleId: number): boolean {
    return this.articleEditStates[articleId] || false;
  }

  isQuantityValid(articleId: number): boolean {
    return this.articleQuantities[articleId] > 0;
  }

  async validateQuantity(article: Article): Promise<void> {
    if (!article.id || !this.articleQuantities[article.id] || this.articleQuantities[article.id] <= 0) {
      this.notifyService.showError('Veuillez saisir une quantité valide', 'Erreur');
      return;
    }

    if (!this.selectedDemandeAchat?.id) {
      this.notifyService.showError('Demande d\'achat non trouvée', 'Erreur');
      return;
    }

    if (this.savingArticles.has(article.id)) {
      return;
    }

    if (this.isArticleUsed(article)) {
      this.notifyService.showError('Cet article est déjà dans la demande', 'Erreur');
      this.articleEditStates[article.id] = false;
      this.articleQuantities[article.id] = 0;
      return;
    }

    try {
      this.savingArticles.add(article.id);

      const ligneDto = {
        ref: article.reference,
        designation: article.designation,
        qte: this.articleQuantities[article.id],
        unite: article.unite,
        fam0001: article.familleStatistique1,
        fam0002: article.familleStatistique2,
        fam0003: article.familleStatistique3,
        da: this.selectedDemandeAchat.id
      };

      await this.ligneDemandeService.addLigneDemande(ligneDto, this.selectedDemandeAchat.id).toPromise();
      await this.loadLignesExistantes();

      this.articleEditStates[article.id] = false;
      this.articleQuantities[article.id] = 0;

      this.notifyService.showSuccess('Ligne ajoutée avec succès', 'Succès');

    } catch (error) {
      console.error('❌ Erreur lors de l\'ajout:', error);
      this.notifyService.showError('Erreur lors de l\'ajout de la ligne', 'Erreur');
    } finally {
      this.savingArticles.delete(article.id);
    }
  }

  // ==================== GESTION DES LIGNES - MODIFICATION/SUPPRESSION ====================

  getLigneKey(ligne: ILigneDemande): string {
    return `ligne_${ligne.idArt || ligne.id || 'new'}`;
  }

  startEditLigneQuantite(ligne: ILigneDemande): void {
    const key = this.getLigneKey(ligne);
    this.ligneEditStates[key] = true;
    this.ligneQuantities[key] = ligne.qte || ligne.quantite || 0;
  }

  cancelEditLigne(ligne: ILigneDemande): void {
    const key = this.getLigneKey(ligne);
    this.ligneEditStates[key] = false;
    this.ligneQuantities[key] = ligne.qte || ligne.quantite || 0;
  }

  isLigneEditing(ligne: ILigneDemande): boolean {
    const key = this.getLigneKey(ligne);
    return this.ligneEditStates[key] || false;
  }

  isLigneQuantityValid(ligne: ILigneDemande): boolean {
    const key = this.getLigneKey(ligne);
    return (this.ligneQuantities[key] || 0) > 0;
  }

  async validateLigneQuantite(ligne: ILigneDemande): Promise<void> {
    const key = this.getLigneKey(ligne);
    const nouvelleQuantite = this.ligneQuantities[key];

    if (!nouvelleQuantite || nouvelleQuantite <= 0) {
      this.notifyService.showError('Veuillez saisir une quantité valide', 'Erreur');
      return;
    }

    const ligneId = ligne.idArt || ligne.id;
    if (!ligneId) {
      this.notifyService.showError('ID de ligne manquant', 'Erreur');
      return;
    }

    try {
      const ligneDto = {
        ref: ligne.ref || ligne.referenceArticle,
        designation: ligne.designation || ligne.designationArticle,
        qte: nouvelleQuantite,
        unite: ligne.unite,
        fam0001: ligne.familleStatistique1,
        fam0002: ligne.familleStatistique2,
        fam0003: ligne.familleStatistique3,
      };

      await this.ligneDemandeService.updateLigneDemande(ligneDto, ligneId).toPromise();
      await this.loadLignesExistantes();

      this.ligneEditStates[key] = false;
      this.notifyService.showSuccess('Quantité modifiée avec succès', 'Succès');

    } catch (error) {
      console.error('❌ Erreur lors de la modification:', error);
      this.notifyService.showError('Erreur lors de la modification', 'Erreur');
    }
  }

  async deleteLigne(ligne: ILigneDemande): Promise<void> {
    const ligneId = ligne.idArt || ligne.id;
    if (!ligneId) {
      this.notifyService.showError('ID de ligne manquant', 'Erreur');
      return;
    }

    try {
      await this.ligneDemandeService.deleteLigneDemande(ligneId).toPromise();
      await this.loadLignesExistantes();
      this.notifyService.showSuccess('Ligne supprimée avec succès', 'Succès');

    } catch (error) {
      console.error('❌ Erreur lors de la suppression:', error);
      this.notifyService.showError('Erreur lors de la suppression', 'Erreur');
    }
  }

  // ==================== MÉTHODES UTILITAIRES ====================

  getAllLignes(): ILigneDemande[] {
    return this.lignesExistantes;
  }

  isArticleUsed(article: Article): boolean {
    return this.lignesExistantes.some(ligne =>
      (ligne.ref === article.reference) ||
      (ligne.referenceArticle === article.reference)
    );
  }

  get canModifyDemande(): boolean {
    if (!this.selectedDemandeAchat) return false;
    const statut = this.selectedDemandeAchat.statut;
    return statut === 0 || statut === null || statut === undefined;
  }

  async updateDemandeAchatStatut(): Promise<void> {
    if (!this.selectedDemandeAchat?.id) {
      this.notifyService.showError('Demande d\'achat non trouvée', 'Erreur');
      return;
    }

    if (this.lignesExistantes.length === 0) {
      this.notifyService.showError('Impossible d\'envoyer une demande sans lignes', 'Erreur');
      return;
    }

    const confirmation = confirm(
      'Êtes-vous sûr de vouloir envoyer cette demande ?\n' +
      'Une fois envoyée, elle ne pourra plus être modifiée.'
    );

    if (!confirmation) return;

    try {
      const sub = this.demandeAchatService.updateDemandeAchatStatut(this.selectedDemandeAchat.id).subscribe({
        next: () => {
          this.notifyService.showSuccess('Demande envoyée avec succès', 'Succès');
          this.loadDemandeDetails(this.selectedDemandeAchat!.id!);
        },
        error: (error) => {
          console.error('❌ Erreur lors de l\'envoi:', error);
          this.notifyService.showError('Erreur lors de l\'envoi de la demande', 'Erreur');
        }
      });
      this.subscriptions.push(sub);

    } catch (error) {
      this.notifyService.showError('Erreur lors de l\'envoi de la demande', 'Erreur');
    }
  }

  // ==================== TRACKING POUR NGFOR ====================
  getSelectedFamilleDesignation(): string {
    if (!this.selectedFamilleCode) {
      return '';
    }

    const famille = this.familleOptions.find(f => f.code === this.selectedFamilleCode);
    return famille ? famille.designation : this.selectedFamilleCode;
  }
  trackByArticleId(index: number, article: Article): any {
    return article.id || article.reference || index;
  }

  trackByLigneId(index: number, ligne: ILigneDemande): any {
    return ligne.idArt || ligne.id || index;
  }

  // ==================== UTILITAIRES DE PAGINATION ====================

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const start = Math.max(0, this.currentPage - 2);
    const end = Math.min(this.totalPages - 1, this.currentPage + 2);

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  }

  get isFirstPage(): boolean {
    return this.currentPage === 0;
  }

  get isLastPage(): boolean {
    return this.currentPage === this.totalPages - 1;
  }

  protected readonly Math = Math;
}
