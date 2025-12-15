// import { HttpClient } from '@angular/common/http';
// import { inject, Injectable } from '@angular/core';
// import { MsalService } from '@azure/msal-angular';
// import { environment } from '../../../../environments/environment';

// @Injectable({
//   providedIn: 'root',
// })
// export class Authservices {
//   private msalService = inject(MsalService);
//   private apiUrl: string = environment.apiUrl;
  

//   constructor(private http: HttpClient) {
//     const guardado = localStorage.getItem('usuario_app');
//     if (guardado) {
//       try {
//         this.currentUser = JSON.parse(guardado);
//         console.log('🔄 Sesión restaurada desde LocalStorage:', this.currentUser);
//       } catch (e) {
//         console.error('Error restaurando sesión', e);
//       }
//     }
//   }

//   public currentUser: any = null;

//   // 1. Obtener el usuario completo (si existe)
//   getUser() {
//     return this.msalService.instance.getActiveAccount();
//   }

//   // 2. Saber si estoy logueado (Devuelve true/false)
//   isLoggedIn(): boolean {
//     return this.msalService.instance.getActiveAccount() != null;
//   }

//   // 3. Obtener solo el nombre/email para mostrar
//   getUserName(): string {
//     const account = this.getUser();
//     return account ? (account.username || account.name || 'Usuario') : '';
//   }

//   // 4. Cerrar Sesión
//   // logout() {
//   //   this.msalService.logoutPopup({
//   //     mainWindowRedirectUri: '/'
//   //   });
//   // }

//   // En tu componente del Navbar

//   // logout() {
//   //   // 1. PRIMERO: Limpiamos nuestra casa (Variables y LocalStorage)
//   //   this.authService.logoutLocal(); 

//   //   // 2. SEGUNDO: Le decimos a Microsoft que cierre sesión
//   //   this.msalService.logoutPopup({
//   //       mainWindowRedirectUri: "/" // Redirigir al inicio al terminar
//   //   });
    
//   //   // Si usas logoutRedirect en vez de Popup, es lo mismo:
//   //   // this.msalService.logoutRedirect();
//   // }


//   //par borrar cache
//   logoutLocal() {
//     // 1. Borramos la variable en memoria
//     this.currentUser = null;
    
//     // 2. Borramos el "Disco Duro"
//     localStorage.removeItem('usuario_app');
    
//     // Opcional: Borrar todo por si acaso (limpieza nuclear)
//     // localStorage.clear(); 
    
//     console.log('🧹 Sesión local eliminada correctamente.');
//   }

//   sincronizarUsuario(datos: any) {
//     // Apunta al BFF
//     //return this.http.post(`${this.apiUrl}/usuarios/sync`, datos); 
//     // Ojo: revisa que this.apiUrl apunte a la base correcta o escríbelo directo:
//     return this.http.post('http://44.197.230.98:8080/bff/usuarios/sync', datos);
//     // return this.http.post('http://localhost:8080/bff/usuarios/sync', datos);
//   }

//   obtenerPerfilDeBaseDeDatos(idAzure: string) {
//     // Llama a tu endpoint GET /api/usuarios/{id}
//     // Asegúrate de que la URL apunte a tu BFF
//     return this.http.get(`http://44.197.230.98:8080/bff/usuarios/${idAzure}`);
//     // return this.http.get(`http://localhost:8080/bff/usuarios/${idAzure}`);
//   }

//   // 3. ACTUALIZAMOS esAdmin PARA USAR LA VARIABLE
//   esAdmin(): boolean {
//     // Si ya cargamos los datos y el id_rol es 1...
//     if (this.currentUser && this.currentUser.id_rol === 1) {
//         return true;
//     }

//     // 2. NUEVO: Si la variable está vacía (por F5), intento leer de LocalStorage
//     const guardado = localStorage.getItem('usuario_app');
//     if (guardado) {
//         try {
//             const usuario = JSON.parse(guardado);
//             // Restauramos la variable para que la próxima vez sea rápido
//             this.currentUser = usuario; 
            
//             if (usuario.id_rol === 1) {
//                 return true;
//             }
//         } catch (e) {
//             console.error('Error leyendo localStorage', e);
//         }
//     }
//     return false;
//   } 

//   // OPCIONAL: También un método para recuperar el usuario completo al recargar
//   getUserFromStorage() {
//       if (this.currentUser) return this.currentUser;
//       const guardado = localStorage.getItem('usuario_app');
//       return guardado ? JSON.parse(guardado) : null;
//   }

//   // NUEVO: Obtener la lista completa
//   obtenerTodosLosUsuarios() {
//     return this.http.get<any[]>(`${this.apiUrl}/usuarios`); 
//     // Asegúrate que apiUrl apunte a http://localhost:8080/bff
//   }
  
// }

// aaaaaaaaaaaaa
// //aaaaaa

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