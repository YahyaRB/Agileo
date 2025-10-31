import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

@Injectable({
  providedIn: 'root'
})
export class KeycloakTimeService {

  constructor(private keycloak: KeycloakService) {}

  async getServerTime(): Promise<Date | null> {
    try {
      const token = await this.keycloak.getToken();
      if (!token) return null;

      const payload = this.decodeToken(token);

      if (payload.iat) {
        const serverTimestamp = payload.iat * 1000;
        const serverDate = new Date(serverTimestamp);

        console.log('🕐 Date serveur Keycloak:', serverDate.toISOString());
        console.log('🕐 Date locale:', new Date().toISOString());
        console.log('⏱️ Décalage:', this.getTimeDifference(serverDate));

        return serverDate;
      }

      return null;
    } catch (error) {
      console.error('❌ Erreur récupération date serveur:', error);
      return null;
    }
  }

  private decodeToken(token: string): any {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) {
        throw new Error('Token JWT invalide');
      }

      const payload = parts[1];
      const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(decoded);
    } catch (error) {
      console.error('❌ Erreur décodage token:', error);
      return {};
    }
  }

  getTimeDifference(serverDate: Date): string {
    const now = new Date();
    const diff = now.getTime() - serverDate.getTime();
    const diffSeconds = Math.abs(Math.floor(diff / 1000));

    if (diffSeconds < 60) {
      return `${diffSeconds} secondes`;
    } else if (diffSeconds < 3600) {
      return `${Math.floor(diffSeconds / 60)} minutes`;
    } else {
      return `${Math.floor(diffSeconds / 3600)} heures`;
    }
  }

  async getTokenDetails(): Promise<any> {
    try {
      const token = await this.keycloak.getToken();
      if (!token) return null;

      const payload = this.decodeToken(token);

      return {
        issuedAt: payload.iat ? new Date(payload.iat * 1000) : null,
        expiresAt: payload.exp ? new Date(payload.exp * 1000) : null,
        notBefore: payload.nbf ? new Date(payload.nbf * 1000) : null,
        authTime: payload.auth_time ? new Date(payload.auth_time * 1000) : null,
        issuer: payload.iss,
        subject: payload.sub,
        audience: payload.aud,
        rawPayload: payload
      };
    } catch (error) {
      console.error('❌ Erreur récupération détails token:', error);
      return null;
    }
  }
}
