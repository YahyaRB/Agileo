import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Observable} from "rxjs";
import {Access} from "../../interfaces/iaccess";
import {environment} from "../../environments/environment";
const AUTH_API = 'access';
const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};
@Injectable({
  providedIn: 'root'
})
export class AccessService {

  constructor(private http: HttpClient) { }
  getAllAccess(): Observable<Access[]>{
    return this.http.get<Access[]>(environment.apiUrl+AUTH_API);
  }

  createAccess(access: any): Observable<any> {
    return this.http.post<any>(environment.apiUrl + AUTH_API, access, httpOptions);
  }

  deleteAccess(accessId: number | undefined): Observable<any> {
    return this.http.delete<any>(`${environment.apiUrl}${AUTH_API}/${accessId}`, httpOptions);
  }

  updateAccess(accessId: number, access: any): Observable<any> {
    return this.http.put<any>(`${environment.apiUrl}${AUTH_API}/${accessId}`, access, httpOptions);
  }
}
