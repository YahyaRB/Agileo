import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class SortService {
  private sortDirection: { [key: string]: boolean } = {};

  constructor() {}

  sortColumn(posts: any[], column: string) {
    // Initialiser la direction de tri si elle n'est pas définie
    if (!(column in this.sortDirection)) {
      this.sortDirection[column] = true;  // Tri croissant par défaut
    }

    const isAscending = this.sortDirection[column];

    // Tri des données en fonction de la direction et de la colonne choisie
    posts.sort((a, b) => {
      const aValue = this.resolvePath(a, column);
      const bValue = this.resolvePath(b, column);

      if (aValue < bValue) return isAscending ? -1 : 1;
      if (aValue > bValue) return isAscending ? 1 : -1;
      return 0;
    });

    // Alterner le sens du tri pour la prochaine fois
    this.sortDirection[column] = !isAscending;
  }

  // Fonction utilitaire pour accéder aux propriétés imbriquées des objets (par exemple : 'user.name')
  private resolvePath(obj: any, path: string) {
    return path.split('.').reduce((acc, key) => acc && acc[key], obj);
  }
}
