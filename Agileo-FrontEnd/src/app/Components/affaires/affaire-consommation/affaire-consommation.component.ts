import {Component, ElementRef, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {Affaire} from "../../../../interfaces/iaffaire";


@Component({
  selector: 'app-affaire-consommation',
  templateUrl: './affaire-consommation.component.html',
  styleUrls: ['./affaire-consommation.component.css']
})
export class AffaireConsommationComponent {
  @ViewChild('closebutton', {static: false}) closebutton!: ElementRef;
  @Output() consommationAffaireEffectue = new EventEmitter<void>();
  @Input() affaire!: Affaire;

}
