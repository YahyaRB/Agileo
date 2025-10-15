import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import {SidebarState} from "../Menu/sidebar/sidebar.component";

@Injectable({
  providedIn: 'root'
})
export class SidebarService {
  private sidebarStateSubject = new BehaviorSubject<SidebarState>(SidebarState.EXPANDED);

  sidebarState$: Observable<SidebarState> = this.sidebarStateSubject.asObservable();

  constructor() {
    // Charger l'état sauvegardé
    const savedState = localStorage.getItem('sidebarState');
    if (savedState && Object.values(SidebarState).includes(savedState as SidebarState)) {
      this.sidebarStateSubject.next(savedState as SidebarState);
    }
  }

  setSidebarState(state: SidebarState) {
    this.sidebarStateSubject.next(state);
    localStorage.setItem('sidebarState', state);
  }

  toggleSidebar() {
    const currentState = this.sidebarStateSubject.value;
    let newState: SidebarState;

    switch (currentState) {
      case SidebarState.EXPANDED:
        newState = SidebarState.COLLAPSED;
        break;
      case SidebarState.COLLAPSED:
        newState = SidebarState.HIDDEN;
        break;
      case SidebarState.HIDDEN:
        newState = SidebarState.EXPANDED;
        break;
    }

    this.setSidebarState(newState);
  }

  get currentState(): SidebarState {
    return this.sidebarStateSubject.value;
  }

  expandSidebar() {
    this.setSidebarState(SidebarState.EXPANDED);
  }

  collapseSidebar() {
    this.setSidebarState(SidebarState.COLLAPSED);
  }

  hideSidebar() {
    this.setSidebarState(SidebarState.HIDDEN);
  }
}
