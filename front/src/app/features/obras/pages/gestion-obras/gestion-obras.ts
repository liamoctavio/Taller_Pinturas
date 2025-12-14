import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Obras } from '../../services/obras';
import { Authservices } from '../../../auth/services/authservices';

@Component({
  selector: 'app-gestion-obras',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './gestion-obras.html',
  styleUrl: './gestion-obras.scss',
})
export class GestionObras {

  private obrasService = inject(Obras); // servicio de obras
  private router = inject(Router);
  private authService = inject(Authservices); // servicio de auth login

  loading = false;
  previewUrl: string | null = null;
  mensajeError: string = '';

  nuevaObra = {
    titulo: '',
    descripcion: '',
    id_tipo_obra: 1, // 1 = Pintura (Asegúrate que coincida con tu DB)
    imagenBase64: ''
  };

  constructor() {
    // Si NO está logueado, lo mandamos al login (o inicio)
    if (!this.authService.isLoggedIn()) {
      alert('Debes iniciar sesión para subir obras.');
      this.router.navigate(['/login']); // O la ruta de tu login
    }
  }

  // Lógica para seleccionar archivo y convertir a Base64
  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      // Validación de tamaño (opcional): máx 2MB para no saturar la DB
      if (file.size > 2 * 1024 * 1024) {
        alert('La imagen es muy pesada. Máximo 2MB.');
        return;
      }

      const reader = new FileReader();
      reader.onload = () => {
        const base64String = reader.result as string;
        this.previewUrl = base64String;
        // Quitamos el prefijo "data:image/jpeg;base64," para enviarlo limpio a Java
        this.nuevaObra.imagenBase64 = base64String.split(',')[1];
      };
      reader.readAsDataURL(file);
    }
  }

  // guardar() {
  //   if (!this.nuevaObra.titulo || !this.nuevaObra.imagenBase64) {
  //     this.mensajeError = 'El título y la imagen son obligatorios';
  //     return;
  //   }

  //   // 2. OBTENEMOS EL USUARIO ACTUAL
  //   const usuario = this.authService.getUser();
    
  //   // 3. AGREGAMOS LA FIRMA (El username o email)
  //   // Nota: Depende de cómo se llame la columna en tu base de datos (username, id_usuario, email?)
  //   // Asumiremos que tu backend espera un campo "username"
  //   const obraParaEnviar = {
  //     ...this.nuevaObra,
  //     username: usuario?.username || 'Anónimo' 
  //   };

  //   this.loading = true;
  //   this.mensajeError = '';

  //   this.obrasService.crearObra(obraParaEnviar).subscribe({
  //     next: (res) => {
  //       alert('¡Obra subida con éxito!');
  //       this.loading = false;
  //       // Redirigir a la galería cuando la tengas lista
  //       // this.router.navigate(['/obras/galeria']); 
  //     },
  //     error: (err) => {
  //       console.error(err);
  //       this.mensajeError = 'Hubo un error al subir la obra.';
  //       this.loading = false;
  //     }
  //   });
  // }

  guardar() {
    if (!this.nuevaObra.titulo || !this.nuevaObra.imagenBase64) {
      this.mensajeError = 'El título y la imagen son obligatorios';
      return;
    }

    // 1. Obtenemos el usuario logueado
    const usuario = this.authService.getUser();
    
    // Si no hay usuario (caso raro si hay auth guard), detenemos
    if (!usuario || !usuario.localAccountId) {
        alert('Error: No se pudo identificar al usuario.');
        return;
    }

    // 2. Preparamos el objeto.
    // IMPORTANTE: Enviamos 'id_azure' (el UUID), NO el username.
    const obraParaEnviar = {
      ...this.nuevaObra,
      id_azure: usuario.localAccountId // <--- AQUÍ ESTÁ EL CAMBIO CLAVE
    };

    this.loading = true;
    this.mensajeError = '';

    console.log('Enviando obra:', obraParaEnviar);

    this.obrasService.crearObra(obraParaEnviar).subscribe({
      next: (res) => {
        alert('¡Obra subida con éxito y vinculada a tu usuario!');
        this.loading = false;
        // Navegar a lista de obras
        this.router.navigate(['/lista-obras']); 
      },
      error: (err) => {
        console.error(err);
        this.mensajeError = 'Hubo un error al subir la obra.';
        this.loading = false;
      }
    });
  }


}
