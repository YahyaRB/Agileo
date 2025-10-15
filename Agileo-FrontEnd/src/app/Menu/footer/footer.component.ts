import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.css']
})
export class FooterComponent {

  currentYear: number = new Date().getFullYear();
  version: string = '1.0.0';

  constructor() { }

  openSupport(event: Event): void {
    event.preventDefault();
    // Ouvrir support - remplacer par votre logique
    window.open('mailto:support@agileo.ma', '_blank');
  }

  openDocumentation(event: Event): void {
    event.preventDefault();
    // Ouvrir documentation - remplacer par votre logique
    window.open('/assets/docs/documentation.pdf', '_blank');
  }

}
