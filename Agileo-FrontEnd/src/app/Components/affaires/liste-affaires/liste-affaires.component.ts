import { Component, OnChanges, OnInit, SimpleChanges, ViewChild, Output, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { EventEmitter } from '@angular/core';
import { FormGroup, FormBuilder } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ElementRef } from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { AffaireServiceService } from 'src/app/services/affaire-service.service';
import { HostListener } from '@angular/core';
import { MatDialog } from "@angular/material/dialog";
import { AffaireDemandeAchatComponent } from "../affaire-demande-achat/affaire-demande-achat.component";
import { Affaire } from "../../../../interfaces/iaffaire";
import { SortService } from "../../../services/sort.service";

@Component({
  selector: 'app-liste-affaires',
  templateUrl: './liste-affaires.component.html',
  styleUrls: ['./liste-affaires.component.css']
})
export class ListeAffairesComponent implements OnInit, OnChanges, OnDestroy {
  @Output() ajoutEffectue = new EventEmitter<void>();

  // Propriétés de base
  showModal = false;
  affaires: Affaire[] = [];
  pfiltre: any;

  // Pagination
  page: number = 1;
  count: number = 0;
  tableSize: number = 10;
  sort = { field: '', direction: 'asc' as 'asc' | 'desc' };
  tableSizes: any = [5, 10, 15, 20];

  // Propriétés pour la gestion du dropdown
  openDropdownIndex: number | null = null;
  selectedRowIndex: number | null = null;
  affaireSelected!: Affaire;

  // État de chargement
  isCollapsed = false;
  isFullscreen = false;
  isLoading: boolean = false;

  // Subscriptions pour éviter les fuites mémoire
  private subscriptions: Subscription = new Subscription();

  // Liste des statuts (corrigée pour correspondre au backend)
  listeStatus: {id: string, name: string}[] = [
    { id: '1', name: 'Actif' },
    { id: '0', name: 'Inactif' }
  ];
  selectedStatus!: string;

  constructor(
    private affaireService: AffaireServiceService,
    private sortService: SortService,
    private elementRef: ElementRef,
    private cdr: ChangeDetectorRef,
    private route: ActivatedRoute,
    private dialog: MatDialog,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.affaireList();
    this.loadData();

    // Gestion des routes enfants
    this.route.children.forEach(child => {
      child.url.subscribe(urlSegment => {
        const routePath = urlSegment.map(segment => segment.path).join('/');
        const id = this.route.snapshot.firstChild?.params['id'];

        if (routePath.includes('demandes-achat')) {
          this.dialog.open(AffaireDemandeAchatComponent, {
            data: { affaireId: id },
            width: '800px'
          });
        }
      });
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.affaireList();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  /**
   * Récupérer la liste des affaires
   */
  affaireList(): void {
    this.isLoading = true;

    const subscription = this.affaireService.getAffaires().subscribe({
      next: (data: Affaire[]) => {
        this.affaires = this.mapAffairesForCompatibility(data);
        this.count = this.affaires.length;
        this.isLoading = false;
        console.log("Data (Affaires) récupérées depuis la base de données => ", this.affaires);
      },
      error: (error) => {
        console.error('Erreur lors du chargement des affaires:', error);
        this.isLoading = false;
        this.affaires = [];
      }
    });

    this.subscriptions.add(subscription);
  }

  /**
   * Mapper les données du backend pour assurer la compatibilité avec le frontend existant
   */
  private mapAffairesForCompatibility(affaires: Affaire[]): Affaire[] {
    return affaires.map(affaire => ({
      ...affaire,
      // Mapping pour rétrocompatibilité
      id: affaire.numero || affaire.id,
      code: affaire.affaire || affaire.code,
      nom: affaire.libelle || affaire.nom,
      statut: affaire.sysState !== undefined ? affaire.sysState : affaire.statut
    }));
  }

  /**
   * Filtrer les affaires selon le statut sélectionné
   */
  getFilteredAffaires(): Affaire[] {
    if (!this.affaires) return [];
    if (!this.selectedStatus) return this.affaires;

    const wantActive = this.normalizeBoolean(this.selectedStatus);
    return this.affaires.filter(affaire => this.isAffaireActive(affaire) === wantActive);
  }

  private normalizeBoolean(value: any): boolean {
    if (typeof value === 'boolean') return value;
    if (typeof value === 'number') return value !== 0;
    if (typeof value === 'string') {
      const v = value.toLowerCase().trim();
      if (v === 'true' || v === '1' || v === 'oui' || v === 'actif') return true;
      if (v === 'false' || v === '0' || v === 'non' || v === 'inactif') return false;
      return v.length > 0; // fallback: non-empty string considered true
    }
    return !!value;
  }

  private isAffaireActive(affaire: Affaire): boolean {
    // Dans le nouveau système : sysState = 1 (actif), 0 ou null (inactif)
    // Pour la compatibilité, on vérifie les deux champs
    const status = affaire.sysState !== undefined ? affaire.sysState : affaire.statut;
    return this.normalizeBoolean(status);
  }

  /**
   * Gestion de la pagination
   */
  onTableDataChange(event: any): void {
    this.page = event;
    // Note: Pas besoin de recharger les données pour la pagination côté client
  }

  // ================ MÉTHODES DE GESTION DES MODALS ================

  ouvrirModalAvecDelay(): void {
    this.showModal = true;
    this.cdr.detectChanges();
    setTimeout(() => {
      const modalBtn = document.querySelector('[data-target="#addModal"]') as HTMLElement;
      if (modalBtn) {
        modalBtn.click();
      }
    }, 100);
  }

  ouvrirModalUpdateAvecDelay(affaire: Affaire): void {
    this.affaireSelected = affaire;
    this.showModal = true;
    this.cdr.detectChanges();
    setTimeout(() => {
      const modalBtn = document.querySelector('[data-target="#updateModal"]') as HTMLElement;
      if (modalBtn) {
        modalBtn.click();
      }
    }, 100);
  }

  ouvrirModalDeleteAvecDelay(affaire: Affaire): void {
    this.affaireSelected = affaire;
    this.showModal = true;
    this.cdr.detectChanges();
    setTimeout(() => {
      const modalBtn = document.querySelector('[data-target="#deleteModal"]') as HTMLElement;
      if (modalBtn) {
        modalBtn.click();
      }
    }, 100);
  }

  // ================ MÉTHODES D'INTERFACE ================

  private loadData(): void {
    this.isLoading = true;
    setTimeout(() => {
      this.isLoading = false;
    }, 1000);
  }

  toggleCollapse(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  toggleFullscreen(): void {
    this.isFullscreen = !this.isFullscreen;

    if (this.isFullscreen) {
      let backdrop = document.querySelector('.fullscreen-backdrop') as HTMLElement;
      if (!backdrop) {
        backdrop = document.createElement('div');
        backdrop.className = 'fullscreen-backdrop';
        backdrop.addEventListener('click', () => this.toggleFullscreen());
        document.body.appendChild(backdrop);
      }

      backdrop.classList.add('show');
      document.body.classList.add('fullscreen-active');
      const modals = document.querySelectorAll('.modal');
      modals.forEach(modal => {
        (modal as HTMLElement).style.zIndex = '10050';
      });
    } else {
      const backdrop = document.querySelector('.fullscreen-backdrop') as HTMLElement;
      if (backdrop) {
        backdrop.classList.remove('show');
      }
      document.body.classList.remove('fullscreen-active');

      const modals = document.querySelectorAll('.modal');
      modals.forEach(modal => {
        (modal as HTMLElement).style.zIndex = '';
      });
    }

    this.cdr.detectChanges();
  }

  // ================ GESTION DU DROPDOWN ================

  toggleDropdown(event: Event, affaire: Affaire): void {
    event.stopPropagation();

    // Utiliser un identifiant unique pour l'affaire
    const affaireId = affaire.numero || affaire.id;

    this.selectedRowIndex = affaireId;

    if (this.openDropdownIndex === affaireId) {
      this.openDropdownIndex = null;
      this.selectedRowIndex = null;
    } else {
      this.openDropdownIndex = affaireId;
    }
  }

  isDropdownOpen(affaire: Affaire): boolean {
    const affaireId = affaire.numero || affaire.id;
    return this.openDropdownIndex === affaireId;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event): void {
    const target = event.target as HTMLElement;
    const dropdown = target.closest('.dropdown');

    if (!dropdown) {
      this.openDropdownIndex = null;
      this.selectedRowIndex = null;
    }
  }

  @HostListener('document:keydown.escape', ['$event'])
  onEscapeKey(event: KeyboardEvent): void {
    this.openDropdownIndex = null;
    this.selectedRowIndex = null;
  }

  private closeDropdown(): void {
    this.openDropdownIndex = null;
    this.selectedRowIndex = null;
  }

  // ================ ACTIONS SUR LES AFFAIRES ================

  viewPurchaseRequests(affaire: Affaire): void {
    this.affaireSelected = affaire;
    this.showModal = true;
    this.cdr.detectChanges();
    setTimeout(() => {
      const modalBtn = document.querySelector('[data-target="#demandeAchatModal"]') as HTMLElement;
      if (modalBtn) {
        modalBtn.click();
      }
    }, 100);
    this.closeDropdown();
  }

  viewReceptions(affaire: Affaire): void {
    this.affaireSelected = affaire;
    this.showModal = true;
    this.cdr.detectChanges();
    setTimeout(() => {
      const modalBtn = document.querySelector('[data-target="#receptionModal"]') as HTMLElement;
      if (modalBtn) {
        modalBtn.click();
      }
    }, 100);
    this.closeDropdown();
  }

  viewConsumptions(affaire: Affaire): void {
    this.affaireSelected = affaire;
    this.showModal = true;
    this.cdr.detectChanges();
    setTimeout(() => {
      const modalBtn = document.querySelector('[data-target="#consommationModal"]') as HTMLElement;
      if (modalBtn) {
        modalBtn.click();
      }
    }, 100);
    this.closeDropdown();
  }

  // ================ MÉTHODES UTILITAIRES ================

  getStatusClass(status: string): string {
    const statusClass = status.toLowerCase()
      .replace(/\s+/g, '-')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');

    return `status-${statusClass}`;
  }

  sortColumn(column: string): void {
    if (this.sort.field === column) {
      this.sort.direction = this.sort.direction === 'asc' ? 'desc' : 'asc';
    } else {
      this.sort.field = column;
      this.sort.direction = 'asc';
    }
    this.sortService.sortColumn(this.affaires, column);
  }

  /**
   * Méthode pour obtenir l'ID d'une affaire de manière cohérente
   */
  getAffaireId(affaire: Affaire): number | undefined {
    return affaire.numero || affaire.id;
  }

  /**
   * TrackBy function pour optimiser les performances de ngFor
   */
  trackByAffaireId(index: number, affaire: Affaire): any {
    return affaire.numero || affaire.id || index;
  }

  /**
   * Méthode pour obtenir le code d'une affaire de manière cohérente
   */
  getAffaireCode(affaire: Affaire): string | undefined {
    return affaire.affaire || affaire.code;
  }

  /**
   * Méthode pour obtenir le nom d'une affaire de manière cohérente
   */
  getAffaireName(affaire: Affaire): string | undefined {
    return affaire.libelle || affaire.nom;
  }

  /**
   * Méthode pour obtenir le statut d'une affaire formaté pour l'affichage
   */
  getAffaireStatusDisplay(affaire: Affaire): string {
    const status = affaire.sysState !== undefined ? affaire.sysState : affaire.statut;
    // Note: Logique inversée corrigée - 1 = Actif, 0 = Inactif
    return status === 1 ? 'Actif' : 'Inactif';
  }
}
