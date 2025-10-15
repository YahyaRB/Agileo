import {Component, ElementRef, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges,
  ViewChild
} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {Affaire} from "../../../../interfaces/iaffaire";
import {AffaireServiceService} from "../../../services/affaire-service.service";


@Component({
  selector: 'app-update-affaire',
  templateUrl: './update-affaire.component.html',
  styleUrls: ['./update-affaire.component.css']
})
export class UpdateAffaireComponent implements OnInit, OnChanges {
  @ViewChild('closebutton', {static: false}) closebutton!: ElementRef;
  @Output() updateEffectue = new EventEmitter<void>();
  @Input()
  public affaireToUpdate!: Affaire;
  myFormUpdate!: FormGroup;

  constructor(private fb: FormBuilder,
              private affaireService: AffaireServiceService
              ) {
  }

  ngOnChanges(changes: SimpleChanges): void {
        if(this.affaireToUpdate){
          this.affectAffaireToForm(this.affaireToUpdate.id);
        }
    }

  ngOnInit(): void {
    this.initFormUpdate();
    }
  private initFormUpdate(){
    this.myFormUpdate = this.fb.group({
      id: ['', Validators.required],
      code: ['', Validators.required],
      designation: ['', Validators.required],
      statut: ['', Validators.required],
    });
  }
  private affectAffaireToForm(id: number|undefined){
    this.myFormUpdate.setValue({
      id: this.affaireToUpdate.id,
      code: this.affaireToUpdate.code,
      designation: this.affaireToUpdate.nom,
      statut: this.affaireToUpdate.statut === 1 ? 'Inactif' : 'Actif'
    })
  }

  onUpdateAffaire() {
    if (this.myFormUpdate.valid) {
      const {id,code, designation,statut} = this.myFormUpdate.value;
      const statutNum = statut === 'Actif' ? 2 : 1;
      const payload = {
        id: id,
        code: code,
        nom: designation,
        statut: statutNum
      };
      this.affaireService.updateAffaire(this.affaireToUpdate.id,payload).subscribe({
        next: (data) => {
          console.log("Affaire is updated successfully");
          this.updateEffectue.emit();
          this.closebutton.nativeElement.click();
        },
        error: (data) => {
          console.log("Affaire is not savec");
        }
      })
      console.log("Incomming Affaire from the list => ", this.affaireToUpdate);
      console.log("Affaire update to be send => ",this.myFormUpdate.value);
    }else {
      console.log("Affaire update to be send provide an error => ",this.myFormUpdate.errors);
    }
  }
}
