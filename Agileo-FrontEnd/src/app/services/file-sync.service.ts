import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface FileSyncEvent {
  receptionId: number;
  action: 'upload' | 'delete' | 'refresh';
  timestamp: number;
  files?: any[];
}

@Injectable({
  providedIn: 'root'
})
export class FileSyncService {
  private fileSyncSubject = new BehaviorSubject<FileSyncEvent | null>(null);
  public fileSync$ = this.fileSyncSubject.asObservable();

  emitFileSyncEvent(event: FileSyncEvent): void {
    console.log('🔄 FileSyncService - Émission événement:', event);
    this.fileSyncSubject.next(event);
  }

  notifyFileUpload(receptionId: number, files?: any[]): void {
    this.emitFileSyncEvent({
      receptionId,
      action: 'upload',
      timestamp: Date.now(),
      files
    });
  }

  notifyFileDelete(receptionId: number): void {
    this.emitFileSyncEvent({
      receptionId,
      action: 'delete',
      timestamp: Date.now()
    });
  }

  notifyFileRefresh(receptionId: number): void {
    this.emitFileSyncEvent({
      receptionId,
      action: 'refresh',
      timestamp: Date.now()
    });
  }
}
