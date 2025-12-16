import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
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

  // NUEVO: Variable para el Lightbox
  obraSeleccionada: any = null;
  private authService = inject(Authservices); // servicio de auth login

  usuarioLogueado: any = null;

  private cd = inject(ChangeDetectorRef)

  ngOnInit() {

    this.usuarioLogueado = this.authService.getUser();
    console.log('Usuario actual:', this.usuarioLogueado);

    this.cargarObras();
  }

  puedeEditar(obra: any): boolean {
    // 1. Si es Admin, tiene superpoderes
    if (this.authService.esAdmin()) return true;

    // 2. Si no hay usuario logueado, nadie edita
    if (!this.usuarioLogueado || !this.usuarioLogueado.localAccountId) return false;

    const miId = this.usuarioLogueado.localAccountId.toLowerCase();

    // 3. BUSQUEDA ROBUSTA DEL ID DEL DUEÑO
    // Intentamos encontrar el ID en todas las variantes posibles que mandan los backends Java
    const rawId = obra.id_azure || obra.idAzure || obra.usuario?.id_azure || obra.usuario?.idAzure;
    
    const obraId = rawId ? rawId.toLowerCase() : null;

    if (!obraId) return false;

    return miId === obraId;
  }

  cargarObras(cacheBust: boolean = false) {
    this.cargandoLista = true;
    this.obrasService.getObras(cacheBust).subscribe({
      next: (datos) => {
        this.listaObras = datos;
        this.cargandoLista = false;
        this.cd.detectChanges();
        // TRUCO: Apenas llega la lista, salimos a buscar las fotos una por una
        this.cargarImagenesFaltantes();
      },
      error: (err) => {
        console.error(err);
        this.errorCarga = 'Error cargando la lista.';
        this.cargandoLista = false;
        this.cd.detectChanges();
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
            this.cd.detectChanges();
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

  // --- LÓGICA DEL LIGHTBOX (VISUALIZADOR) ---
  abrirVisualizador(obra: any) {
    this.obraSeleccionada = obra;
  }

  cerrarVisualizador() {
    this.obraSeleccionada = null;
  }





}
