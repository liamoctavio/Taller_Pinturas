import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment';


@Injectable({
  providedIn: 'root',
})
export class Obras {
  private http = inject(HttpClient);
  // URL final: http://localhost:8080/bff/obras
  private apiUrl = `${environment.apiUrl}/obras`; 

  // 1. Obtener lista (Solo títulos y descripciones, carga rápido)
  getObras(cacheBust: boolean = false) {
    if (!cacheBust) {
      return this.http.get<any[]>(this.apiUrl);
    }
    // Añadimos un query param con timestamp para evitar caches intermedios
    const params = { t: Date.now().toString() };
    return this.http.get<any[]>(this.apiUrl, { params });
  }

  // 2. Obtener detalle (Con la foto en Base64, carga más lento)
  getObraPorId(id: number) {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  // 3. Subir nueva obra
  crearObra(obra: any) {
    return this.http.post(this.apiUrl, obra);
  }

  // Editar obra
  editarObra(id: number, datos: any) {
    return this.http.put(`${this.apiUrl}/${id}`, datos);
  }

  // Eliminar obra
  // eliminarObra(id: number) {
  //   return this.http.delete(`${this.apiUrl}/${id}`);
  // }
  eliminarObra(idObra: number, idAzure: string) { // <--- Agregamos idAzure
    // Enviamos el ID del usuario en la URL: DELETE /api/obras/123?id_azure=5f78...
    return this.http.delete(`${this.apiUrl}/${idObra}?id_azure=${idAzure}`);
  }
}
