import {Component, ElementRef, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {Affaire} from "../../../../interfaces/iaffaire";

@Component({
  selector: 'app-affaire-demande-achat',
  templateUrl: './affaire-demande-achat.component.html',
  styleUrls: ['./affaire-demande-achat.component.css']
})
export class AffaireDemandeAchatComponent {
  @ViewChild('closebutton', {static: false}) closebutton!: ElementRef;
  @Output() demandeEffectue = new EventEmitter<void>();
  @Input() affaire!: Affaire;

}
