import {Irole} from "./irole";

export interface Iuser {
  id : number;
  email : string;
  matricule : string;
  nom : string;
  dernierConnexion:string;
  prenom: string;
  statut: string;
  idAgelio?: string;
  login: number;
  roles: Irole[];
}


export interface UserData {
  id?: number;
  email?: string;
  matricule?: string;
  nom?: string;
  prenom?: string;
  statut?: string;
  login?: string;
  roles?: string[];
  affaires?:string[];
  acces?: string[];
  idAgelio?: string;
  keycloakUser?: {
    id: string;
    username: string;
    firstName: string;
    lastName: string;
    email: string;
    emailVerified: boolean;
  }
}
