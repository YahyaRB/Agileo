import {Component, ElementRef, Input, OnInit, Output, ViewChild} from '@angular/core';
import {Affaire} from "../../../../interfaces/iaffaire";
import {Access} from "../../../../interfaces/iaccess";
import {AccessService} from "../../../services/access.service";
import {UtilisateurServiceService} from "../../../services/utilisateur-service.service";
import {Iuser} from "../../../../interfaces/iuser";
import {NotificationService} from "../../../services/notification.service";
type AccessSelectable = Access & { selected?: boolean;
  fromDatabase?: boolean;
  toBeDeleted?: boolean;   // true si on a déplacé vers la gauche mais pas encore validé
};
@Component({
  selector: 'app-affectation-user-access',
  templateUrl: './affectation-user-access.component.html',
  styleUrls: ['./affectation-user-access.component.css']
})
export class AffectationUserAccessComponent implements OnInit{
  @Input() userSelected!: Iuser;
  listAccess: AccessSelectable[] = [];
  listUserAccess: AccessSelectable[] = [];
  accessAttribuees: AccessSelectable[] = [];
  @ViewChild('accessModal', { static: true }) modalElement!: ElementRef;
  @ViewChild('closebutton') closeButton!: ElementRef;
  constructor(private accessService: AccessService,
              private notifyService: NotificationService,
              private utilisateurService: UtilisateurServiceService
              ) {
  }

  ngOnInit(): void {
    this.modalElement.nativeElement.addEventListener('hidden.bs.modal', () => {
      this.refreshLists();
    });

    // Charger d'abord les accès de l'utilisateur, puis tous les accès
    this.loadUserAccess(() => this.loadAccess());
    }

  // Charger tous les accès disponibles
  loadAccess() {
    this.accessService.getAllAccess().subscribe({
      next: data => {
        // ✅ Ne garder que ceux qui ne sont pas déjà attribués
        const userAccessIds = new Set(this.listUserAccess.map(a => a.id));
        this.listAccess = data
          .filter(a => !userAccessIds.has(a.id))
          .map(a => ({ ...a, selected: false }));
        console.log("List of Access", this.listAccess);
      },
      error: err => console.log(err)
    });
  }

  // Charger les accès déjà attribués à l'utilisateur
  loadUserAccess(callback?: () => void) {
    this.utilisateurService.getUserAccess(this.userSelected.id).subscribe({
      next: data => {
        // Marquer ces accès comme venant de la base
        this.listUserAccess = data.map(a => ({ ...a, selected: false, fromDatabase: true }));
        console.log(`Access list of user ${this.userSelected.id}`, data);
        if (callback) callback();
      },
      error: err => console.log(err)
    });
  }

  // Déplacer les accès sélectionnés de gauche vers droite
  attribuerAccess() {
    const selection = this.listAccess.filter(a => a.selected);
    // ✅ Marquer comme nouveau (pas en base)
    this.listUserAccess.push(...selection.map(a => ({ ...a, selected: false, fromDatabase: false })));
    this.listAccess = this.listAccess.filter(a => !a.selected);
  }


  // Déplacer de droite vers gauche
  retirerAccess() {

    const selection = this.listUserAccess.filter(a => a.selected);

    selection.forEach(a => {
      if (a.fromDatabase) {
        // Marquer pour suppression
        a.toBeDeleted = true;
      }

      // Le remettre dans la liste de gauche si pas déjà présent
      if (!this.listAccess.find(x => x.id === a.id)) {
        this.listAccess.push({ ...a, selected: false });
      }

      // Retirer de la liste de droite
      this.listUserAccess = this.listUserAccess.filter(x => x !== a);
    });
  }

  onTableDataChange($event: number) {
    this.loadAccess();
  }


  refreshLists() {
    this.loadUserAccess(() => this.loadAccess());
  }
  public resetModal() {
    this.refreshLists();
  }
  // Validation
  async OnValideAffectation(): Promise<void> {
    const nouveauxAccess = this.listUserAccess.filter(access => !access.fromDatabase);
    const accessASupprimer = this.listAccess.filter(access => access.toBeDeleted);

    const totalOperations = nouveauxAccess.length + accessASupprimer.length;

    if (totalOperations === 0) {
      this.notifyService.showInfo('Aucune modification à appliquer', "Info");
      this.closeModal();
      return;
    }
    try {
      if (nouveauxAccess.length > 0) {
        console.log('Début des ajouts d\'accès...');
        for (let i = 0; i < nouveauxAccess.length; i++) {
          const access = nouveauxAccess[i];
          console.log(`[${i + 1}/${nouveauxAccess.length}] Ajout de l'accès ${access.code}`);

          await this.addAccessSequentially(access);
          console.log(`✓ Accès ${access.code} ajouté avec succès`);
        }
        console.log('Tous les ajouts terminés');
      }
      if (accessASupprimer.length > 0) {
        console.log('Début des suppressions d\'accès...');
        for (let i = 0; i < accessASupprimer.length; i++) {
          const access = accessASupprimer[i];
          console.log(`[${i + 1}/${accessASupprimer.length}] Suppression de l'accès ${access.code}`);

          await this.removeAccessSequentially(access);
          console.log(`✓ Accès ${access.code} supprimé avec succès`);
        }
        console.log('Toutes les suppressions terminées');
      }

      this.notifyService.showSuccess('Affectation des accès mise à jour avec succès', "Success");
      this.closeModal();
      this.refreshLists();

    } catch (error) {
      console.error('Erreur lors de l\'affectation:', error);
      this.notifyService.showError('Une erreur est survenue lors de l\'affectation des accès', "Erreur");
    }
  }
  private addAccessSequentially(access: AccessSelectable): Promise<void> {
    return new Promise((resolve, reject) => {
      this.utilisateurService.addAccessToUser(this.userSelected.id, access.id).subscribe({
        next: () => {
          access.fromDatabase = true;
          resolve();
        },
        error: (err) => {
          console.error(`Erreur lors de l'ajout de l'accès ${access.code}:`, err);
          reject(err);
        }
      });
    });
  }
  private removeAccessSequentially(access: AccessSelectable): Promise<void> {
    return new Promise((resolve, reject) => {
      this.utilisateurService.removeAccessFromUser(this.userSelected.id, access.id).subscribe({
        next: () => {
          access.toBeDeleted = false;
          resolve();
        },
        error: (err) => {
          console.error(`Erreur lors de la suppression de l'accès ${access.code}:`, err);
          reject(err);
        }
      });
    });
  }
  private closeModal(): void {
    this.closeButton.nativeElement.click();
  }
}
