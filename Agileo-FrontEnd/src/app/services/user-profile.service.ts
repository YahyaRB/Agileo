import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { environment } from '../../environments/environment';

export interface UserProfileData {
  firstName: string;
  lastName: string;
  email: string;
  username: string;
  phone?: string;
  position?: string;
  department?: string;
}

export interface UserSettings {
  language: string;
  theme: string;
  notifications: {
    email: boolean;
    browser: boolean;
    sound: boolean;
  };
  privacy: {
    profileVisible: boolean;
    emailVisible: boolean;
  };
}

export interface PasswordChangeData {
  currentPassword: string;
  newPassword: string;
}

export interface LoginHistory {
  id: number;
  date: string;
  device: string;
  ipAddress: string;
  status: string;
  userAgent: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserProfileService {
  private apiUrl = environment.apiUrl;
  private currentUserSubject = new BehaviorSubject<any>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadCurrentUser();
  }

  private getHttpOptions() {
    return {
      headers: new HttpHeaders({
        'Content-Type': 'application/json'
      })
    };
  }
  // =============== GESTION DU PROFIL ===============

  /**
   * Récupère l'utilisateur actuel
   */
  getCurrentUser(): Observable<any> {
    return this.http.get(`${this.apiUrl}users/current`, this.getHttpOptions());
  }

  /**
   * Met à jour le profil utilisateur
   */
  updateProfile(profileData: UserProfileData): Observable<any> {
    return this.http.put(`${this.apiUrl}/user/profile`, profileData, this.getHttpOptions());
  }

  /**
   * Met à jour l'avatar utilisateur
   */
  updateAvatar(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('avatar', file);

    return this.http.post(`${this.apiUrl}/user/avatar`, formData);
  }

  // =============== GESTION DES PARAMÈTRES ===============

  /**
   * Récupère les paramètres utilisateur
   */
  getUserSettings(): Observable<UserSettings> {
    return this.http.get<UserSettings>(`${this.apiUrl}/user/settings`, this.getHttpOptions());
  }

  /**
   * Sauvegarde les paramètres utilisateur
   */
  saveUserSettings(settings: UserSettings): Observable<any> {
    // Sauvegarder en local storage pour la démo
    localStorage.setItem('userSettings', JSON.stringify(settings));

    // Ici vous pourriez aussi sauvegarder sur le serveur
    return this.http.put(`${this.apiUrl}/user/settings`, settings, this.getHttpOptions());
  }

  /**
   * Applique le thème sélectionné
   */
  applyTheme(theme: string): void {
    document.body.className = document.body.className.replace(/theme-\w+/g, '');
    document.body.classList.add(`theme-${theme}`);

    // Sauvegarder le thème dans le localStorage
    localStorage.setItem('selectedTheme', theme);
  }

  /**
   * Charge le thème sauvegardé
   */
  loadSavedTheme(): void {
    const savedTheme = localStorage.getItem('selectedTheme');
    if (savedTheme) {
      this.applyTheme(savedTheme);
    }
  }

  // =============== GESTION DE LA SÉCURITÉ ===============


  exportUserData(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/user/export`, {
      responseType: 'blob'
    });
  }

  /**
   * Récupère l'historique des connexions
   */
  getLoginHistory(): Observable<LoginHistory[]> {
    // Données de démonstration
    const mockHistory: LoginHistory[] = [
      {
        id: 1,
        date: new Date().toISOString(),
        device: 'Chrome/Windows',
        ipAddress: '192.168.1.100',
        status: 'active',
        userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
      },
      {
        id: 2,
        date: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
        device: 'Safari/iOS',
        ipAddress: '192.168.1.105',
        status: 'closed',
        userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_7_1 like Mac OS X)'
      }
    ];

    return new Observable(observer => {
      observer.next(mockHistory);
      observer.complete();
    });
  }

  /**
   * Récupère les statistiques d'utilisation
   */
  getUsageStatistics(): Observable<any> {
    const mockStats = {
      storageUsed: 157, // MB
      storageTotal: 200, // MB
      connectionsThisMonth: 28,
      documentsShared: 12,
      lastBackup: new Date().toISOString()
    };

    return new Observable(observer => {
      observer.next(mockStats);
      observer.complete();
    });
  }


  /**
   * Envoie une demande de support
   */
  submitSupportRequest(data: { subject: string; message: string; category: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/support/request`, data, this.getHttpOptions());
  }

  // =============== NOTIFICATIONS ===============

  /**
   * Demande la permission pour les notifications du navigateur
   */
  requestNotificationPermission(): Promise<NotificationPermission> {
    if ('Notification' in window) {
      return Notification.requestPermission();
    }
    return Promise.resolve('denied');
  }

  /**
   * Envoie une notification de test
   */
  sendTestNotification(): void {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification('Test de notification', {
        body: 'Vos notifications fonctionnent correctement !',
        icon: '/assets/images/notification-icon.png'
      });
    }
  }

  // =============== MÉTHODES UTILITAIRES ===============

  /**
   * Charge l'utilisateur actuel et met à jour le subject
   */
  private loadCurrentUser(): void {
    this.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUserSubject.next(user);
      },
      error: (error) => {
        console.error('Erreur lors du chargement de l\'utilisateur:', error);
      }
    });
  }

  /**
   * Met à jour l'utilisateur actuel dans le subject
   */
  updateCurrentUser(user: any): void {
    this.currentUserSubject.next(user);
  }

  /**
   * Génère un nom de fichier pour l'export des données
   */
  generateExportFilename(): string {
    const now = new Date();
    const dateStr = now.toISOString().split('T')[0];
    return `mes-donnees-${dateStr}.json`;
  }

  /**
   * Valide un mot de passe selon les critères de sécurité
   */
  validatePassword(password: string): { valid: boolean; errors: string[] } {
    const errors: string[] = [];

    if (password.length < 8) {
      errors.push('Le mot de passe doit contenir au moins 8 caractères');
    }

    if (!/[A-Z]/.test(password)) {
      errors.push('Le mot de passe doit contenir au moins une majuscule');
    }

    if (!/[a-z]/.test(password)) {
      errors.push('Le mot de passe doit contenir au moins une minuscule');
    }

    if (!/\d/.test(password)) {
      errors.push('Le mot de passe doit contenir au moins un chiffre');
    }

    if (!/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
      errors.push('Le mot de passe doit contenir au moins un caractère spécial');
    }

    return {
      valid: errors.length === 0,
      errors
    };
  }

  /**
   * Formate la taille des fichiers
   */
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }
}
