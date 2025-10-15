import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class SidebarFixServiceService {
  constructor() {
    this.fixSidebar();
  }

  private fixSidebar(): void {
    // S'exécute après le chargement de core.js
    setTimeout(() => {
      // Retirer la classe d'ouverture
      document.body.classList.remove('right_tb_toggle');

      // Remplacer l'event handler
      const rightTabs = document.querySelectorAll('.right_tab');
      rightTabs.forEach(tab => {
        tab.addEventListener('click', this.preventSidebarToggle, true);
      });
    }, 500);
  }

  private preventSidebarToggle(e: Event): void {
    e.preventDefault();
    e.stopPropagation();
    e.stopImmediatePropagation();
  }
}
