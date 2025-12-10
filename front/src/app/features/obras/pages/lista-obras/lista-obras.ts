import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { Obras } from '../../services/obras';
import { finalize } from 'rxjs/operators';
import { FormsModule } from '@angular/forms';
import { Authservices } from '../../../auth/services/authservices';

interface Obra {
  id: number;
  titulo: string;
  imagen: string;
  descripcion: string;
  precio?: number;
}

@Component({
  selector: 'app-lista-obras',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './lista-obras.html',
  styleUrl: './lista-obras.scss',
})
export class ListaObras implements OnInit {

  private obrasService = inject(Obras); // servicio de obras
  private msalService = inject(MsalService);

  listaObras: any[] = [];
  cargandoLista = true;
  errorCarga = '';

  modoEdicion = false; // Para controlar si estamos en modo edición
  obraEditando: any = {}; // ID de la obra que se está editando
  guardandoCambios = false;


  private authService = inject(Authservices); // servicio de auth login

  usuarioLogueado: any = null;

  ngOnInit() {

    this.usuarioLogueado = this.authService.getUser();
    console.log('Usuario actual:', this.usuarioLogueado);

    this.cargarObras();
  }
//FUNCIONA PERFECT
  // puedeEditar(obra: any): boolean {
  //   // REGLA 1: Debe estar logueado
  //   if (!this.usuarioLogueado) return false;

  //   // REGLA 2 (OPCIONAL): ¿Es mi foto? 
  //   // Comparamos el email de Azure con el username de la base de datos
  //   // Ojo: Asegúrate que tu DB guarde el email completo o ajusta esta lógica.
  //   // return this.usuarioLogueado.username === obra.username;
    
  //   // POR AHORA: Si está logueado, puede editar todo (Modo simple)
  //   return true; 
  // }

  // puedeEditar(obra: any): boolean {
  //   // 1. Si no hay nadie logueado, nadie edita.
  //   if (!this.usuarioLogueado || !this.usuarioLogueado.localAccountId) {
  //       return false;
  //   }
  //   // 2. ¿Soy Admin? (Opcional, pero recomendado)
  //   // Si tienes implementado el método esAdmin() en tu servicio, úsalo:
  //   // if (this.authService.esAdmin()) {
  //   //     return true; 
  //   // }

  //   // 3. COMPARACIÓN DE DUEÑO
  //   // Mi ID (del login)
  //   const miId = this.usuarioLogueado.localAccountId.toLowerCase();
    
  //   // El ID de la obra (que ahora viene del Backend gracias al paso 1)
  //   const obraId = obra.id_azure ? obra.id_azure.toLowerCase() : null;

  //   // Si la obra no tiene dueño (es vieja), nadie la edita (salvo admin)
  //   if (!obraId) return false;

  //   // ¿Son iguales?
  //   return miId === obraId; 
  // }

  puedeEditar(obra: any): boolean {
    // 1. LOG DE DEPURACIÓN (Solo para ver si está llegando el rol)
     console.log('Soy Admin?', this.authService.esAdmin());

    // 2. REGLA SUPREMA: Si es Admin, puede editar TODO.
    if (this.authService.esAdmin()) {
        return true; 
    }

    // 3. REGLA DE PROPIEDAD: Si no es Admin, revisamos si es el dueño
    if (!this.usuarioLogueado || !this.usuarioLogueado.localAccountId) {
        return false;
    }

    const miId = this.usuarioLogueado.localAccountId.toLowerCase();
    const obraId = obra.id_azure ? obra.id_azure.toLowerCase() : null;

    if (!obraId) return false;

    return miId === obraId;
  }

  cargarObras(cacheBust: boolean = false) {
    this.cargandoLista = true;
    this.obrasService.getObras(cacheBust).subscribe({
      next: (datos) => {
        this.listaObras = datos;
        this.cargandoLista = false;
        
        // TRUCO: Apenas llega la lista, salimos a buscar las fotos una por una
        this.cargarImagenesFaltantes();
      },
      error: (err) => {
        console.error(err);
        this.errorCarga = 'Error cargando la lista.';
        this.cargandoLista = false;
      }
    });
  }

  cargarImagenesFaltantes() {
    // Recorremos cada obra de la lista
    this.listaObras.forEach((obra, index) => {
      // Llamamos al servicio para pedir el detalle (que trae la foto)
      this.obrasService.getObraPorId(obra.id_obra).subscribe({
        next: (obraConFoto) => {
          // Cuando llega la foto, actualizamos esa obra específica en la lista
          if (obraConFoto.imagenBase64) {
            // Le agregamos la propiedad 'imagen' para que el HTML la detecte
            this.listaObras[index].imagenBase64 = obraConFoto.imagenBase64;
          }
        },
        error: (err) => console.error(`Error cargando foto ${obra.id_obra}`, err)
      });
    });
  }

  // --- LÓGICA DE BORRADO ---
  borrarObra(id: number, event: Event) {
    event.stopPropagation(); // Evita que se abra la foto al hacer clic en borrar

    // Obtenemos el id del usuario desde MSAL
    const account = this.msalService.instance.getActiveAccount() || this.msalService.instance.getAllAccounts()[0];
    const idAzure = account?.localAccountId || account?.homeAccountId || account?.username || '';

    if (!idAzure) {
      alert('Debes iniciar sesión para eliminar una obra.');
      return;
    }

    if (confirm('¿Estás seguro de que quieres eliminar esta obra? No se puede deshacer.')) {
      this.obrasService.eliminarObra(id, idAzure).subscribe({
        next: () => {
          // Eliminado correctamente: actualizamos la lista localmente
          // y mostramos mensaje no intrusivo
          console.log('Obra eliminada');
          this.listaObras = this.listaObras.filter(o => o.id_obra !== id);
        },
        error: (err) => {
          console.error(err);
          alert('Error al eliminar. (Nota: Tu backend dice que solo Admins pueden borrar)');
        }
      });
    }
  }

  // --- LÓGICA DE EDICIÓN ---
  abrirEditor(obra: any, event: Event) {
    event.stopPropagation();
    // Creamos una copia para no modificar la tarjeta visualmente hasta guardar
    this.obraEditando = { ...obra, imagenBase64: null }; // No cargamos la foto vieja en el editor para no hacerlo pesado
    this.modoEdicion = true;
  }

  cerrarEditor() {
    this.modoEdicion = false;
    this.obraEditando = {};
  }

  // Lógica para cambiar la foto (Idéntica a NuevaObraComponent)
  onFileChange(event: any) {
    const file = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => {
        const base64 = (reader.result as string).split(',')[1];
        this.obraEditando.imagenBase64 = base64; // Guardamos la nueva foto
      };
      reader.readAsDataURL(file);
    }
  }

  // guardarCambios() {
  //   // Enviamos los cambios al backend
  //   this.obrasService.editarObra(this.obraEditando.id_obra, this.obraEditando).subscribe({
  //     next: (res) => {
  //       alert('Cambios guardados');
  //       this.cerrarEditor();
  //       this.cargarObras(); // Recargamos la lista para ver los cambios
  //     },
  //     error: (err) => alert('Error al guardar cambios')
  //   });
  // }
  guardarCambios() {
    // Preservamos los datos que enviaremos porque vamos a cerrar el modal inmediatamente
    const payload = { ...this.obraEditando };
    const id = payload.id_obra;

    // Cerramos el modal inmediatamente para que la UI no quede bloqueada
    this.cerrarEditor();

    // Actualizamos optimísticamente la lista en memoria para mostrar cambios inmediatamente
    const idx = this.listaObras.findIndex(o => o.id_obra === id);
    if (idx > -1) {
      // Mezclamos los cambios sobre la entrada existente
      this.listaObras[idx] = { ...this.listaObras[idx], ...payload };
    }

    // Mostramos un indicador global (guardandoCambios) si quieres usarlo en otra parte
    this.guardandoCambios = true;

    this.obrasService.editarObra(id, payload)
      .pipe(
        finalize(() => {
          this.guardandoCambios = false;
        })
      )
      .subscribe({
        next: (res) => {
          // Refrescamos la lista en background (forzando cache-bust)
          this.cargarObras(true);
          // Notificación no bloqueante (reemplaza el alert para no interrumpir al usuario)
          console.log('Cambios guardados exitosamente');
        },
        error: (err) => {
          console.error(err);
          alert('Error al guardar cambios');
        }
      });
  }





}
