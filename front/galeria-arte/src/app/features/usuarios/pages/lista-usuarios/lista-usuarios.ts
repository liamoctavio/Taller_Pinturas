import { Component, inject, OnInit } from '@angular/core';
import { Authservices } from '../../../auth/services/authservices';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-lista-usuarios',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './lista-usuarios.html',
  styleUrl: './lista-usuarios.scss',
})
export class ListaUsuarios implements OnInit{

  private authService = inject(Authservices);

  usuarios: any[] = [];
  cargando = true;

  ngOnInit() {
    this.cargarUsuarios();
  }

  cargarUsuarios() {
    this.cargando = true;
    this.authService.obtenerTodosLosUsuarios().subscribe({
      next: (data) => {
        this.usuarios = data;
        this.cargando = false;
        console.log('Usuarios cargados:', data);
      },
      error: (err) => {
        console.error(err);
        this.cargando = false;
      }
    });
  }

  // Helper para pintar las etiquetas de rol bonito
  // getBadgeClass(rol: string): string {
  //   // 1. Si es nulo o indefinido, devolvemos gris
  //   if (!rol) return 'bg-secondary';
    
  //   const nombreRol = rol?.nombre_rol ? rol.nombre_rol : String(rol);
  //   // 2. TRUCO DE BLINDAJE: Convertimos a String obligatoriamente
  //   // Esto evita el error "is not a function" si llega un número
  //   const r = String(nombreRol).toLowerCase(); 

  //   // 3. Verificamos texto o números (por si acaso llega el ID)
  //   if (r.includes('admin') || r === '1') return 'bg-danger';      // Rojo
  //   if (r.includes('artista') || r === '2') return 'bg-primary';   // Azul
  //   if (r.includes('visitante') || r === '3') return 'bg-info text-dark'; // Celeste
    
  //   return 'bg-secondary'; // Gris por defecto
  // }
  // ...existing code...
  // Helper para pintar las etiquetas de rol bonito
  getBadgeClass(rol: string | number | { nombre_rol?: string } | null | undefined): string {
    // 1. Si es nulo o indefinido, devolvemos gris
    if (rol == null) return 'bg-secondary';

    // 2. Resolver nombreRol de forma segura según el tipo recibido
    let nombreRol: string;
    if (typeof rol === 'object') {
      // rol puede ser un objeto con nombre_rol
      nombreRol = String((rol as any).nombre_rol ?? rol);
    } else {
      // string o number
      nombreRol = String(rol);
    }

    // 3. TRUCO DE BLINDAJE: Convertimos a String obligatoriamente
    const r = nombreRol.toLowerCase();

    // 4. Verificamos texto o números (por si acaso llega el ID)
    if (r.includes('admin') || r === '1') return 'bg-danger';      // Rojo
    if (r.includes('artista') || r === '2') return 'bg-primary';   // Azul
    if (r.includes('visitante') || r === '3') return 'bg-info text-dark'; // Celeste

    return 'bg-secondary'; // Gris por defecto
  }
// ...existing code...

}
