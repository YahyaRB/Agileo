import {Component, ElementRef, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {Affaire} from "../../../../interfaces/iaffaire";


@Component({
  selector: 'app-affaire-reception',
  templateUrl: './affaire-reception.component.html',
  styleUrls: ['./affaire-reception.component.css']
})
export class AffaireReceptionComponent {
  @ViewChild('closebutton', {static: false}) closebutton!: ElementRef;
  @Output() receptionAffaireEffectue = new EventEmitter<void>();
  @Input() affaire!: Affaire;
}
