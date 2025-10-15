export interface IPieces {
  id?: number;
  nom: string; // Correspond probablement à 'name' dans le template HTML
  url: string;
  taille?: number;
  type: string;
  dateUpload?: string; // Correspond probablement à 'uploadDate' dans le template HTML

  // Ajoutez les propriétés manquantes que votre template utilise :
  fileId?: number; // Pour les appels de fonction (viewFile, downloadFile, deleteFile)
  fullFileName?: string; // Utilisé pour le titre et getFileIcon
  sizeFormatted?: string; // Utilisé pour l'affichage de la taille
  uploadDate?: string; // Déjà présent (à vérifier si le type doit être string ou Date)
  uploadedByNom?: string; // Utilisé pour afficher l'utilisateur
}

// NOTE: Si 'nom' correspond à 'name' et 'dateUpload' correspond à 'uploadDate'
// vous devriez harmoniser, soit en utilisant les noms de l'interface,
// soit en adaptant l'interface.
