import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment';


@Injectable({
  providedIn: 'root',
})
export class Obras {
  private http = inject(HttpClient);
  // URL final: http://localhost:8080/bff/obras
  private apiUrl = `${environment.apiUrl}/obras`; 

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token'); // Recuperamos el token que guardaste en el Login
    if (!token) {
        console.warn('⚠️ No hay token en localStorage, la petición a Obras fallará');
        return new HttpHeaders();
    }
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }

  // 1. Obtener lista
  getObras(cacheBust: boolean = false) {
    if (!cacheBust) {
      // Agregamos headers aquí
      return this.http.get<any[]>(this.apiUrl, { headers: this.getHeaders() });
    }
    
    // Si hay params, los combinamos con los headers
    const params = { t: Date.now().toString() };
    return this.http.get<any[]>(this.apiUrl, { 
        params: params,
        headers: this.getHeaders() // <--- Agregamos headers también aquí
    });
  }


  getObraPorId(id: number) {

    return this.http.get<any>(`${this.apiUrl}/${id}?includeImage=true`, { 
        headers: this.getHeaders() 
    });
  }

  // 3. Subir nueva obra
  crearObra(obra: any) {
    return this.http.post(this.apiUrl, obra, { headers: this.getHeaders() });
  }

  // Editar obra
  editarObra(id: number, datos: any) {
    return this.http.put(`${this.apiUrl}/${id}`, datos, { headers: this.getHeaders() });
  }

  // Eliminar obra
  eliminarObra(idObra: number, idAzure: string) { 
    // Enviamos el ID del usuario en la URL: DELETE /api/obras/123?id_azure=...
    // Y el token en los headers
    return this.http.delete(`${this.apiUrl}/${idObra}?id_azure=${idAzure}`, { headers: this.getHeaders() });
  }
}
