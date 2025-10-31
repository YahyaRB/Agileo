import {Component, OnDestroy, OnInit} from '@angular/core';
import {KeycloakTimeService} from "../../services/keycloak-time.service";

@Component({
  selector: 'app-server-time-checker',
  templateUrl: './server-time-checker.component.html',
  styleUrls: ['./server-time-checker.component.css']
})
export class ServerTimeCheckerComponent implements OnInit, OnDestroy {
  serverTime: Date | null = null;
  localTime: Date = new Date();
  timeDifference: string = '';
  tokenDetails: any = null;
  isLoading: boolean = true;

  private intervalId: any;

  constructor(private keycloakTimeService: KeycloakTimeService) {}

  async ngOnInit() {
    await this.loadTimeInfo();

    // Mettre à jour la date locale chaque seconde
    this.intervalId = setInterval(() => {
      this.localTime = new Date();
    }, 1000);
  }

  ngOnDestroy() {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  async loadTimeInfo() {
    this.isLoading = true;
    try {
      this.serverTime = await this.keycloakTimeService.getServerTime();
      this.tokenDetails = await this.keycloakTimeService.getTokenDetails();

      if (this.serverTime) {
        this.timeDifference = this.keycloakTimeService.getTimeDifference(this.serverTime);
      }
    } catch (error) {
      console.error('Erreur lors du chargement:', error);
    } finally {
      this.isLoading = false;
    }
  }

  getTokenLifetime(): string {
    if (!this.tokenDetails?.issuedAt || !this.tokenDetails?.expiresAt) {
      return 'N/A';
    }

    const lifetime = this.tokenDetails.expiresAt.getTime() -
      this.tokenDetails.issuedAt.getTime();
    const minutes = Math.floor(lifetime / 60000);
    const seconds = Math.floor((lifetime % 60000) / 1000);

    return `${minutes}min ${seconds}s`;
  }

  getRemainingTime(): string {
    if (!this.tokenDetails?.expiresAt) {
      return 'N/A';
    }

    const now = new Date();
    const remaining = this.tokenDetails.expiresAt.getTime() - now.getTime();

    if (remaining < 0) {
      return 'Expiré';
    }

    const minutes = Math.floor(remaining / 60000);
    const seconds = Math.floor((remaining % 60000) / 1000);

    return `${minutes}min ${seconds}s`;
  }

  async refresh() {
    await this.loadTimeInfo();
  }

  isTokenExpiringSoon(): boolean {
    if (!this.tokenDetails?.expiresAt) return false;

    const now = new Date();
    const remaining = this.tokenDetails.expiresAt.getTime() - now.getTime();

    // Moins de 5 minutes restantes
    return remaining < 300000 && remaining > 0;
  }

  isTokenExpired(): boolean {
    if (!this.tokenDetails?.expiresAt) return false;

    const now = new Date();
    return now.getTime() > this.tokenDetails.expiresAt.getTime();
  }
}
