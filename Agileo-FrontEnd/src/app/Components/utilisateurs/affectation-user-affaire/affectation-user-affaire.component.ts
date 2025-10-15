import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { Affaire } from "../../../../interfaces/iaffaire";
import { AffaireServiceService } from "../../../services/affaire-service.service";
import { Iuser } from "../../../../interfaces/iuser";
import { UtilisateurServiceService } from "../../../services/utilisateur-service.service";
import {NotificationService} from "../../../services/notification.service";

// Type pour les affaires sélectionnables avec états
type AffaireSelectable = Affaire & {
  selected?: boolean;
  fromDatabase?: boolean;
  toBeDeleted?: boolean;
};

@Component({
  selector: 'app-affectation-user-affaire',
  templateUrl: './affectation-user-affaire.component.html',
  styleUrls: ['./affectation-user-affaire.component.css']
})
export class AffectationUserAffaireComponent implements OnInit, OnChanges {
  @Input() userSelected!: Iuser;
  @Input() accessorSelected: any;

  // Nouvelles listes pour le système gauche/droite
  affairesDisponibles: AffaireSelectable[] = [];
  affairesAttribuees: AffaireSelectable[] = [];

  isLoading: boolean = false;
  pfiltre: any;
  page: number = 1;
  count: number = 0;
  tableSize: number = 10;
  tableSizes: any = [5, 10, 15, 20];
  //Pour affaires attribuer
  pfiltreAttr: any;
  pageAttr: number = 1;
  countAttr: number = 0;
  tableSizeAttr: number = 10;
  tableSizesAttr: any = [5, 10, 15, 20];
  constructor(
    private affaireService: AffaireServiceService,
    private notifyService: NotificationService,
    private utilisateurService: UtilisateurServiceService
  ) {}

  ngOnInit(): void {
    console.log('ngOnInit - accessorSelected:', this.accessorSelected);
    console.log('ngOnInit - userSelected:', this.userSelected);

    if (this.accessorSelected?.accessorId) {
      this.loadData();
    } else {
      this.loadAllAffaires(); // Charger toutes les affaires disponibles
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    console.log('ngOnChanges called with:', changes);

    if (changes['accessorSelected']) {
      console.log('accessorSelected changed:', changes['accessorSelected'].currentValue);
      if (this.accessorSelected?.accessorId) {
        this.loadData();
      }
    }

    if (changes['userSelected']) {
      console.log('userSelected changed:', changes['userSelected'].currentValue);
    }
  }

  private loadData(): void {
    console.log('loadData called with accessor:', this.accessorSelected);
    // Charger d'abord les affaires de l'utilisateur, puis toutes les affaires
    this.loadUserAffaires(() => this.loadAllAffaires());
  }

  // Charger toutes les affaires disponibles
  loadAllAffaires(): void {
    this.isLoading = true;
    this.affaireService.getAffaires().subscribe({
      next: (data: Affaire[]) => {
        // Filtrer les affaires déjà attribuées
        const affairesAttribueesIds = new Set(this.affairesAttribuees.map(a => this.getAffaireCode(a)));

        this.affairesDisponibles = (data || [])
          .filter(affaire => !affairesAttribueesIds.has(this.getAffaireCode(affaire)))
          .map(affaire => ({
            ...affaire,
            selected: false
          }));

        this.isLoading = false;
        console.log('Affaires disponibles chargées:', this.affairesDisponibles);
      },
      error: (err) => {
        console.error('Erreur chargement affaires:', err);
        this.affairesDisponibles = [];
        this.isLoading = false;
      }
    });
  }

  // Charger les affaires déjà attribuées à l'utilisateur
  loadUserAffaires(callback?: () => void): void {
    if (!this.accessorSelected?.accessorId) {
      console.warn('Aucun accessor sélectionné');
      this.affairesAttribuees = [];
      if (callback) callback();
      return;
    }

    this.utilisateurService.getAccessorAffaires(this.accessorSelected.accessorId).subscribe({
      next: (data: Affaire[]) => {
        // Marquer ces affaires comme venant de la base de données
        this.affairesAttribuees = (data || []).map(affaire => ({
          ...affaire,
          selected: false,
          fromDatabase: true
        }));

        console.log('Affaires attribuées chargées:', this.affairesAttribuees);
        if (callback) callback();
      },
      error: (err) => {
        console.error('Erreur chargement affaires utilisateur:', err);
        this.affairesAttribuees = [];
        if (callback) callback();
      }
    });
  }
  onTableDataChange(event: any) {
    this.page = event;
    // this.loadAllAffaires();
  }
  onTableDataChangeAttr(event: any) {
    this.pageAttr = event;
  }
  // Déplacer les affaires sélectionnées de gauche vers droite (attribuer)
  attribuerAffaires(): void {
    const selection = this.affairesDisponibles.filter(a => a.selected);

    if (selection.length === 0) {
      alert('Veuillez sélectionner des affaires à attribuer');
      return;
    }

    // Marquer comme nouveau (pas encore en base de données)
    const nouvellesAffaires = selection.map(affaire => ({
      ...affaire,
      selected: false,
      fromDatabase: false
    }));

    this.affairesAttribuees.push(...nouvellesAffaires);

    // Retirer de la liste des disponibles
    this.affairesDisponibles = this.affairesDisponibles.filter(a => !a.selected);

    console.log('Affaires attribuées:', nouvellesAffaires.map(a => this.getAffaireCode(a)));
  }

  // Déplacer les affaires sélectionnées de droite vers gauche (retirer)
  retirerAffaires(): void {
    const selection = this.affairesAttribuees.filter(a => a.selected);

    if (selection.length === 0) {
      alert('Veuillez sélectionner des affaires à retirer');
      return;
    }

    selection.forEach(affaire => {
      if (affaire.fromDatabase) {
        // Si c'est une affaire existante en DB, la marquer pour suppression
        affaire.toBeDeleted = true;
      }

      // Remettre dans la liste des disponibles si pas déjà présent
      if (!this.affairesDisponibles.find(x => this.getAffaireCode(x) === this.getAffaireCode(affaire))) {
        this.affairesDisponibles.push({
          ...affaire,
          selected: false
        });
      }
    });

    // Retirer de la liste des attribuées
    this.affairesAttribuees = this.affairesAttribuees.filter(a => !a.selected);

    console.log('Affaires retirées:', selection.map(a => this.getAffaireCode(a)));
  }

  // Validation finale
  validerAffectation(): void {
    if (!this.accessorSelected?.accessorId) {
      alert('Aucun accessor sélectionné');
      return;
    }

    this.isLoading = true;

    // Nouvelles affaires à ajouter
    const nouvellesAffaires = this.affairesAttribuees.filter(a => !a.fromDatabase);

    // Affaires à supprimer (marquées toBeDeleted dans les disponibles)
    const affairesASupprimer = this.affairesDisponibles.filter(a => a.toBeDeleted);

    console.log('Nouvelles affaires à ajouter:', nouvellesAffaires.map(a => this.getAffaireCode(a)));
    console.log('Affaires à supprimer:', affairesASupprimer.map(a => this.getAffaireCode(a)));

    let operations = 0;
    let operationsComplete = 0;
    let errors: string[] = [];

    const checkComplete = () => {
      if (operations === operationsComplete) {
        this.isLoading = false;
        if (errors.length > 0) {
          alert(`Affectation terminée avec erreurs:\n${errors.join('\n')}`);
        }
        // Recharger les données
        this.loadData();
      }
    };

    // Ajouter les nouvelles affaires
    nouvellesAffaires.forEach(affaire => {
      operations++;
      const code = this.getAffaireCode(affaire);

      this.utilisateurService.addAffaireToAccessor(this.accessorSelected.accessorId, code).subscribe({
        next: () => {
          operationsComplete++;
          console.log(`Affaire ${code} ajoutée avec succès`);
          // Marquer comme existante en DB
          affaire.fromDatabase = true;
          checkComplete();
        },
        error: (err) => {
          console.error(`Erreur ajout affaire ${code}:`, err);
          errors.push(`Erreur ajout ${code}: ${err.error?.message || err.message}`);
          operationsComplete++;
          checkComplete();
        }
      });
    });

    // Supprimer les affaires retirées
    affairesASupprimer.forEach(affaire => {
      operations++;
      const code = this.getAffaireCode(affaire);

      this.utilisateurService.removeAffaireFromAccessor(this.accessorSelected.accessorId, code).subscribe({
        next: () => {
          operationsComplete++;
          console.log(`Affaire ${code} supprimée avec succès`);
          // Enlever le marqueur de suppression
          affaire.toBeDeleted = false;
          checkComplete();
        },
        error: (err) => {
          console.error(`Erreur suppression affaire ${code}:`, err);
          errors.push(`Erreur suppression ${code}: ${err.error?.message || err.message}`);
          operationsComplete++;
          checkComplete();
        }
      });
    });

    // Si aucune opération nécessaire
    if (operations === 0) {
      this.isLoading = false;
      alert('Aucun changement détecté');
    }
  }

  // Méthodes utilitaires pour gérer les différentes structures d'affaires
  getAffaireCode(affaire: Affaire): string {
    return affaire.code || affaire.affaire || affaire.id?.toString() || '';
  }

  getAffaireName(affaire: Affaire): string {
    return affaire.nom || affaire.libelle || 'Sans nom';
  }

  getAffaireStatus(affaire: Affaire): number {
    return affaire.statut !== undefined ? affaire.statut : (affaire.sysState || 0);
  }

  // TrackBy function pour optimiser les performances de ngFor
  trackByAffaire(index: number, affaire: Affaire): any {
    return affaire.id || affaire.code || affaire.affaire || index;
  }

  // Réinitialiser le composant
  resetModal(): void {
    this.affairesDisponibles = [];
    this.affairesAttribuees = [];
    this.isLoading = false;
  }

  // Méthode publique pour rafraîchir les listes
  refreshLists(): void {
    if (this.accessorSelected?.accessorId) {
      this.loadData();
    }
  }

}
