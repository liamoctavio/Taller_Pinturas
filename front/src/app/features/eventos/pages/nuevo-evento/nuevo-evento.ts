import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Eventos } from '../../services/eventos';
import { Authservices } from '../../../auth/services/authservices';
import { Router } from '@angular/router';

@Component({
  selector: 'app-nuevo-evento',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './nuevo-evento.html',
  styleUrl: './nuevo-evento.scss',
})
export class NuevoEvento {

  private eventosService = inject(Eventos);
  private authService = inject(Authservices);
  private router = inject(Router);

  loading = false;

  // // Modelo del formulario asie staba ante
  // nuevoEvento = {
  //   titulo: '',
  //   descripcion: '',
  //   id_tipo_evento: 1, // 1=Taller, 2=Exposición, 3=Charla (Según tu SQL)
  //   fechaInicio: '',   // Lo llenaremos desde el input
  //   fechaTermino: '',
  //   precio: 0,
  //   direccion: '',
  //   id_azure: null || null;,      // Se llena automático
  //   id_rol: 1          // Asumimos rol 1 (Admin/Usuario) por defecto
  // };

  nuevoEvento: {
    titulo: string;
    descripcion: string;
    id_tipo_evento: number;
    fechaInicio: string;
    fechaTermino: string | null;
    precio: number;
    direccion: string;
    id_azure: string | null; // <-- allow string or null
    id_rol: number;
  } = {
    titulo: '',
    descripcion: '',
    id_tipo_evento: 1,
    fechaInicio: '',
    fechaTermino: null,
    precio: 0,
    direccion: '',
    id_azure: null,
    id_rol: 1
  };

  guardar() {
    // 1. Validaciones básicas
    if (!this.nuevoEvento.titulo || !this.nuevoEvento.fechaInicio || !this.nuevoEvento.direccion) {
      alert('Por favor completa Título, Fecha Inicio y Dirección');
      return;
    }

    this.loading = true;

    // 2. Obtener usuario actual (para el id_azure)
    const cuenta = this.authService.getUser();
    // Nota: Dependiendo de tu objeto usuario, saca el ID correcto (localAccountId, homeAccountId, etc)
    // Si no tienes el ID a mano, puedes enviar un string genérico o el username si tu backend lo aguanta.
    // this.nuevoEvento.id_azure = usuario?.localAccountId || 'usuario-anonimo';
    if (cuenta) {
        // Si hay usuario logueado, sacamos el UUID (localAccountId)
        console.log('Usuario detectado:', cuenta.username);
        this.nuevoEvento.id_azure = cuenta.localAccountId; 
    } else {
        // Si no (caso raro si estás logueado), mandamos null
        this.nuevoEvento.id_azure = null; 
    }


    // 3. TRUCO DE FECHAS: Convertir formato HTML a formato Java Instant
    // HTML da: "2023-10-10T10:00" -> Java quiere: "2023-10-10T10:00:00Z"
    const eventoParaEnviar = {
      ...this.nuevoEvento,
      fechaInicio: this.formatearFecha(this.nuevoEvento.fechaInicio),
      fechaTermino: this.nuevoEvento.fechaTermino ? this.formatearFecha(this.nuevoEvento.fechaTermino) : null
    };

    // 4. Enviar al servicio
    this.eventosService.crearEvento(eventoParaEnviar).subscribe({
      next: () => {
        alert('Evento creado exitosamente');
        this.router.navigate(['/eventos']); // Volver al calendario
      },
      error: (err) => {
        console.error(err);
        alert('Error al crear evento. Revisa la consola.');
        this.loading = false;
      }
    });
  }

  // Función auxiliar para agregar segundos y Z
  private formatearFecha(fechaHtml: string): string {
    if (!fechaHtml) return '';
    return fechaHtml + ':00Z'; 
  }

}
