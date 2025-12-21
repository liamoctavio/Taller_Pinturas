import { HttpClient, HttpHeaders } from '@angular/common/http'; // IMPORTANTE: HttpHeaders
import { inject, Injectable } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class Authservices {
  private msalService = inject(MsalService);
  private http = inject(HttpClient); 
  private apiUrl: string = environment.apiUrl; 

  public currentUser: any = null;

  constructor() {
    const guardado = localStorage.getItem('usuario_app');
    if (guardado) {
      try {
        this.currentUser = JSON.parse(guardado);
        console.log('🔄 Sesión restaurada:', this.currentUser);
      } catch (e) {
        console.error('Error restaurando sesión', e);
      }
    }
  }

  // ==============================================================
  // 1. MÉTODO AUXILIAR PARA OBTENER EL TOKEN
  // ==============================================================
  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token'); 
    if (!token) {
        console.warn('⚠️ No hay token en localStorage, la petición Auth fallará');
        return new HttpHeaders();
    }
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }

  // ==============================================================
  // 2. MÉTODOS PÚBLICOS
  // ==============================================================

  getUser() {
    return this.msalService.instance.getActiveAccount();
  }

  isLoggedIn(): boolean {
    return this.msalService.instance.getActiveAccount() != null;
  }

  getUserName(): string {
    const account = this.getUser();
    return account ? (account.username || account.name || 'Usuario') : '';
  }

  logoutLocal() {
    this.currentUser = null;
    localStorage.removeItem('usuario_app');
    localStorage.removeItem('token'); // TAMBIÉN BORRAMOS EL TOKEN
    console.log('🧹 Sesión local eliminada correctamente.');
  }

  // 3. SINCRONIZAR (POST)
  sincronizarUsuario(datos: any) {
    // IMPORTANTE: Agregamos headers también aquí por si acaso el backend lo pide
    return this.http.post(`${this.apiUrl}/usuarios/sync`, datos, { headers: this.getHeaders() }); 
  }

  // 4. OBTENER PERFIL (GET) - AQUÍ FALLABA
  obtenerPerfilDeBaseDeDatos(idAzure: string) {
    // Agregamos headers manualmente
    return this.http.get(`${this.apiUrl}/usuarios/${idAzure}`, { headers: this.getHeaders() });
  }

  // 5. LISTAR TODOS (GET)
  obtenerTodosLosUsuarios() {
    // Agregamos headers manualmente
    return this.http.get<any[]>(`${this.apiUrl}/usuarios`, { headers: this.getHeaders() }); 
  }

  // 6. VERIFICAR ADMIN
  esAdmin(): boolean {
    if (this.currentUser && this.currentUser.id_rol === 1) {
        return true;
    }
    const guardado = localStorage.getItem('usuario_app');
    if (guardado) {
        try {
            const usuario = JSON.parse(guardado);
            this.currentUser = usuario; 
            if (usuario.id_rol === 1) return true;
        } catch (e) { }
    }
    return false;
  } 

  getUserFromStorage() {
      if (this.currentUser) return this.currentUser;
      const guardado = localStorage.getItem('usuario_app');
      return guardado ? JSON.parse(guardado) : null;
  }
}