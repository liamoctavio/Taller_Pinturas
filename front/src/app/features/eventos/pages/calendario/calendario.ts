import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { Evento, Eventos } from '../../services/eventos';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Authservices } from '../../../auth/services/authservices';

@Component({
  selector: 'app-calendario',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './calendario.html',
  styleUrl: './calendario.scss',
})
export class Calendario implements OnInit {

  private eventosService = inject(Eventos); // servicio de eventos
  private authService = inject(Authservices); // servicio de auth login
  usuarioLogueado: boolean = false;

  eventos: any[] = [];
  cargando = true;

  modoEdicion = false;
  eventoEditando: any = {};
  guardando = false;

  private cd = inject(ChangeDetectorRef);

  ngOnInit() {
    this.usuarioLogueado = this.authService.isLoggedIn();
    this.cargarEventos();
  }

  cargarEventos() {
    this.cargando = true;
    this.eventosService.obtenerEventos().subscribe({
      next: (data) => {
        this.eventos = data.map(evento => {
          // Lógica de imagen (igual que antes)
          const nombreTipo = evento.tipo?.nombre || 'Evento General';
          let imagenUrl = 'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?w=500&q=80';
          if (nombreTipo.toLowerCase().includes('taller')) imagenUrl = 'https://images.unsplash.com/photo-1513364776144-60967b0f800f?w=500&q=80';
          else if (nombreTipo.toLowerCase().includes('exposicion')) imagenUrl = 'https://images.unsplash.com/photo-1536924940846-227afb31e2a5?w=500&q=80';

          return {
            ...evento, // Guardamos todas las propiedades originales (incluyendo 'direccion')
            
            // Propiedades para la VISTA (Tarjeta)
            hora: new Date(evento.fechaInicio).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            tipoNombre: nombreTipo,
            imagen: imagenUrl,
            ubicacion: evento.direccion, // Para que funcione el botón del mapa

            // Propiedades para el EDITOR (Raw = Crudo)
            // Guardamos la fecha original exacta que vino del backend
            fechaInicioRaw: evento.fechaInicio,   
            fechaTerminoRaw: evento.fechaTermino
          };
        });
        this.cargando = false;
        this.cd.detectChanges();
      },
      error: (err) => { console.error(err); this.cargando = false; }
    });
  }

  // Función para abrir Google Maps con la dirección
  verEnMapa(lat: any, lng: any, direccion: string) {
    let url = '';
    if (lat && lng) {
      url = `https://www.google.com/maps/search/?api=1&query=${lat},${lng}`;
    } else {
      // Si no hay coordenadas, busca por el texto de la dirección
      url = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(direccion)}`;
    }
    window.open(url, '_blank');
  }

  // --- BORRAR ---
  borrarEvento(id: number) {
    if(!confirm('¿Seguro que deseas cancelar este evento?')) return;

    // Obtenemos mi ID
    const usuario = this.authService.getUser();
    if (!usuario || !usuario.localAccountId) {
        alert('Error de sesión'); 
        return;
    }

    // Enviamos ID del evento + MI ID
    this.eventosService.eliminarEvento(id, usuario.localAccountId).subscribe({
      next: () => {
        alert('Evento eliminado');
        this.cargarEventos();
      },
      error: (err) => alert('Error al eliminar (Revisa permisos)')
    });
  }

  abrirEditor(evento: any) {
    // 1. Clonar el objeto
    this.eventoEditando = { ...evento };

    // 2. ARREGLAR LA DIRECCIÓN
    if (!this.eventoEditando.direccion && this.eventoEditando.ubicacion) {
        this.eventoEditando.direccion = this.eventoEditando.ubicacion;
    }

    // 3. ARREGLAR LAS FECHAS (Corte de segundos)
    if (this.eventoEditando.fechaInicioRaw) {
      this.eventoEditando.fechaInicioInput = this.eventoEditando.fechaInicioRaw.substring(0, 16);
    }
    if (this.eventoEditando.fechaTerminoRaw) {
      this.eventoEditando.fechaTerminoInput = this.eventoEditando.fechaTerminoRaw.substring(0, 16);
    }


    // Si el evento ya tiene un usuario asignado (objeto anidado), rescatamos su ID
    if (evento.usuario && evento.usuario.id_azure) {
        this.eventoEditando.id_azure = evento.usuario.id_azure;
    } 

    // El rol lo mantenemos en 1 o lo que traiga
    if (!this.eventoEditando.id_rol) {
        this.eventoEditando.id_rol = 1; 
    }
    

    // Aplanar el tipo de evento
    if (this.eventoEditando.tipo && this.eventoEditando.tipo.id_tipo_evento) {
        this.eventoEditando.id_tipo_evento = this.eventoEditando.tipo.id_tipo_evento;
    }

    this.modoEdicion = true;
  }

  cerrarEditor() {
    this.modoEdicion = false;
    this.eventoEditando = {};
  }

  // --- GUARDAR CAMBIOS ---
  guardarCambios() {
    this.guardando = true;

    const idReal = this.eventoEditando.id_eventos || this.eventoEditando.id;

    if (!idReal) {
      console.error('ERROR CRÍTICO: No se encuentra el ID del evento:', this.eventoEditando);
      alert('Error interno: No se pudo identificar el evento para editar.');
      this.guardando = false;
      return;
    }

    // 2. PREPARAR FECHAS
    // Convertir fecha del Input de vuelta a formato Java (Agregar :00Z)
    const eventoParaEnviar = {
      ...this.eventoEditando,
      // Aseguramos que enviamos el ID con el nombre que espera el backend
      id_eventos: idReal, 
      fechaInicio: this.eventoEditando.fechaInicioInput ? this.eventoEditando.fechaInicioInput + ':00Z' : null,
      fechaTermino: this.eventoEditando.fechaTerminoInput ? this.eventoEditando.fechaTerminoInput + ':00Z' : null
    };

    console.log('Enviando edición al ID:', idReal, eventoParaEnviar);

    // 3. ENVIAR AL SERVICIO
    this.eventosService.editarEvento(idReal, eventoParaEnviar).subscribe({
      next: () => {
        alert('Evento actualizado exitosamente');
        this.guardando = false;
        this.cerrarEditor();
        this.cargarEventos();
      },
      error: (err) => {
        console.error('Error al editar:', err);
        this.guardando = false;
        alert('Error al guardar cambios. Revisa la consola.');
      }
    });
  }

  puedeEditar(evento: any): boolean {
    // 1. Si es Admin, pase libre
    if (this.authService.esAdmin()) return true;

    if (!this.authService.isLoggedIn()) return false;

    const usuario = this.authService.getUser();
    
    // Validamos que existan los datos
    const miId = usuario?.localAccountId?.toLowerCase();
    const eventoId = evento.id_azure ? evento.id_azure.toLowerCase() : null; // <--- AQUÍ LLEGABA NULL ANTES



    if (!miId || !eventoId) return false;

    return miId === eventoId;
  }

}
