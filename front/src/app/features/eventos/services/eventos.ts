import { inject, Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment.development';
import { HttpClient, HttpHeaders } from '@angular/common/http';


export interface Evento {
  id: number;
  titulo: string;
  tipo: string; // 'Taller' | 'Conferencia' | 'Exposición' | etc.
  fecha: Date;
  hora: string;
  precio: number;
  ubicacion: string;
  lat: number; 
  lng: number; 
  imagen: string;
}

@Injectable({
  providedIn: 'root',
})


export class Eventos {

  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/eventos`; // http://localhost:8080/bff/eventos

  private getHeaders(): HttpHeaders {
  const token = localStorage.getItem('token'); // O la clave que uses
  if (!token) {
      console.warn('⚠️ No hay token en localStorage, la petición fallará');
      return new HttpHeaders();
  }
  return new HttpHeaders({
    'Authorization': `Bearer ${token}`
  });
}

  obtenerEventos() {
    // return this.http.get<any[]>(this.apiUrl);
    return this.http.get<any[]>(this.apiUrl, { headers: this.getHeaders() });
  }


  crearEvento(evento: any) {
    return this.http.post(this.apiUrl, evento, { headers: this.getHeaders() });
  }

  // Editar
  // editarEvento(id: number, evento: any) {
  //   return this.http.put(`${this.apiUrl}/${id}`, evento);
  // }
  editarEvento(id: number, evento: any) {
      return this.http.put(`${this.apiUrl}/${id}`, evento, { headers: this.getHeaders() });
  }

  // Eliminar

  // eliminarEvento(id: number, idAzure: string) { // <--- Agregamos idAzure
  //   return this.http.delete(`${this.apiUrl}/eventos/${id}?id_azure=${idAzure}`);
  // }
  // eliminarEvento(id: number, idAzure: string) {
  //   // 1. Agregamos el query param ?id_azure=...
  //   // 2. Agregamos los headers con el Token
  //   return this.http.delete(`${this.apiUrl}/${id}?id_azure=${idAzure}`, { 
  //       headers: this.getHeaders() 
  //   });
  // }
  eliminarEvento(id: number, idAzure: string) {
    // ⚠️ ERROR COMÚN: No uses comillas simples ' '
    // ✅ CORRECTO: Usa comillas invertidas ` ` para que funcionen las variables ${...}
    console.log('>>> Servicio borrando ID:', id, ' usuario:', idAzure);
    // La URL debe quedar así: http://.../bff/eventos/15?id_azure=e5e7...
    const url = `${this.apiUrl}/${id}?id_azure=${idAzure}`;
    console.log('>>> URL Generada:', url);
    return this.http.delete(url, { 
        headers: this.getHeaders() // No olvides los headers del token
    });
  }

  
}
