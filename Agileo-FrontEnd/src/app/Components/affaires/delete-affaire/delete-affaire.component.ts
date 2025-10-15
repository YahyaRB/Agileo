import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import {Affaire} from "../../../../interfaces/iaffaire";
import {AffaireServiceService} from "../../../services/affaire-service.service";

@Component({
  selector: 'app-delete-affaire',
  templateUrl: './delete-affaire.component.html',
  styleUrls: ['./delete-affaire.component.css']
})
export class DeleteAffaireComponent implements OnInit, OnChanges {
  @ViewChild('closebutton', {static: false}) closebutton!: ElementRef;
  @Output() deleteEffectue = new EventEmitter<void>();
  @Input()
  public affaireToDelete?: Affaire;
  constructor(private  affaireService: AffaireServiceService) {
  }

  ngOnInit(): void {
        console.log("Affaire to delete =>", this.affaireToDelete);
    }
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['affaireToDelete']) {
      console.log("Affaire reçue :", this.affaireToDelete);
    }
  }

  deleteAffaire() {
      console.log("Affaire to delete =>", this.affaireToDelete);
    // @ts-ignore
    this.affaireService.deleteAffaire(this.affaireToDelete.id).subscribe({
        next: ()=> {
          console.log("Affaire is deleted successfully.");
          this.deleteEffectue.emit();
          this.closebutton.nativeElement.click();
        },
        error: ()=> {
          console.log("Affaire is deleted errorfully.");
        }
      });
  }
}
