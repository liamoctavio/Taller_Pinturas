import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class Usuarios {
  private http = inject(HttpClient);
  
  // Usamos la URL que definiste en el paso anterior
  private apiUrl = `${environment.apiUrl}/usuarios`; 

  getUsuarios() {
    // Esto devuelve un Observable (un flujo de datos)
    return this.http.get<any[]>(this.apiUrl);
  }
}
