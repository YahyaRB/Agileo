import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Observable} from "rxjs";
import {ILigneReception} from "../../interfaces/ilignereception";
import {environment} from "../../environments/environment";

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable({
  providedIn: 'root'
})
export class LigneReceptionService {

  constructor(private http: HttpClient) { }

  addLigneReception(lignes: ILigneReception[], receptionId: number): Observable<any> {
    return this.http.post<any>(`${environment.apiUrl}receptions/${receptionId}/lignes`, lignes, httpOptions);
  }

  getLignesReceptionByReceptionId(receptionId: number): Observable<ILigneReception[]> {
    return this.http.get<ILigneReception[]>(`${environment.apiUrl}receptions/${receptionId}/lignes`);
  }

  updateLigneReception(ligneId: number, ligne: ILigneReception): Observable<any> {
    return this.http.put<any>(`${environment.apiUrl}receptions/lignes/${ligneId}`, ligne, httpOptions);
  }

  deleteLigneReception(ligneId: number): Observable<any> {
    return this.http.delete<any>(`${environment.apiUrl}receptions/lignes/${ligneId}`, httpOptions);
  }
}
