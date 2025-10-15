import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import { Observable } from 'rxjs';
import { Role } from '../../interfaces/irole';
import { environment } from '../../environments/environment';
const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};
@Injectable({
  providedIn: 'root'
})
export class RoleService {

  constructor(private http: HttpClient) { }

  getAllRoles(): Observable<Role[]> {
    return this.http.get<Role[]>(`${environment.apiUrl}roles`);
  }
  createRole(role: any): Observable<any> {
    return this.http.post<any>(`${environment.apiUrl}roles`, role, httpOptions);
  }

  deleteRole(roleId: number): Observable<any> {
    return this.http.delete<any>(`${environment.apiUrl}roles/${roleId}`, httpOptions);
  }

  updateRole(roleId: number, role: any): Observable<any> {
    return this.http.put<any>(`${environment.apiUrl}roles/${roleId}`, role, httpOptions);
  }
}
