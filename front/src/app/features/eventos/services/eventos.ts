import { inject, Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment.development';
import { HttpClient } from '@angular/common/http';


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

  obtenerEventos() {
    return this.http.get<any[]>(this.apiUrl);
  }


  crearEvento(evento: any) {
    return this.http.post(this.apiUrl, evento);
  }

  // Editar
  editarEvento(id: number, evento: any) {
    return this.http.put(`${this.apiUrl}/${id}`, evento);
  }

  // Eliminar
  // eliminarEvento(id: number) {
  //   return this.http.delete(`${this.apiUrl}/${id}`);
  // }
  eliminarEvento(id: number, idAzure: string) { // <--- Agregamos idAzure
    return this.http.delete(`${this.apiUrl}/eventos/${id}?id_azure=${idAzure}`);
  }

  
}
