import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Obras } from '../../services/obras';
import { Authservices } from '../../../auth/services/authservices';

@Component({
  selector: 'app-gestion-obras',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink],
  templateUrl: './gestion-obras.html',
  styleUrl: './gestion-obras.scss',
})
export class GestionObras {

  private obrasService = inject(Obras); // servicio de obras
  private router = inject(Router);
  private authService = inject(Authservices); // servicio de auth login
  private cd = inject(ChangeDetectorRef);

  loading = false;
  previewUrl: string | null | ArrayBuffer = null;
  mensajeError: string = '';

  isDragging = false;

  nuevaObra = {
    titulo: '',
    descripcion: '',
    id_tipo_obra: 1, // 1 = Pintura (Asegúrate que coincida con tu DB)
    imagenBase64: ''
  };

  constructor() {
    if (!this.authService.isLoggedIn()) {
      alert('Debes iniciar sesión para subir obras.');
      this.router.navigate(['/login']);
    }
  }


  // 1. Entrada por Input File (Click)
  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.procesarArchivo(file);
    }
  }

  // 2. Entrada por Drag & Drop
  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
    
    if (event.dataTransfer && event.dataTransfer.files.length > 0) {
      const file = event.dataTransfer.files[0];
      this.procesarArchivo(file);
    }
  }

  // 3. Procesamiento REAL (Valida, Lee y Actualiza Vista)
  procesarArchivo(file: File) {
    if (!file) return;

    this.mensajeError = '';

    // A. Validaciones
    if (!file.type.match(/image.*/)) {
      this.mensajeError = 'Solo se permiten archivos de imagen (JPG, PNG)';
      this.cd.detectChanges(); // Actualizar error visualmente
      return;
    }

    if (file.size > 2 * 1024 * 1024) {
      this.mensajeError = 'La imagen es muy pesada. Máximo 2MB.';
      this.cd.detectChanges();
      return;
    }

    // B. Leer Archivo
    const reader = new FileReader();
    
    reader.onload = (e) => {
      const base64String = e.target?.result as string;
      
      // Actualizamos variables
      this.previewUrl = base64String;
      // Quitamos el prefijo para enviarlo limpio a Java
      this.nuevaObra.imagenBase64 = base64String.split(',')[1];

      this.cd.detectChanges();
    };

    reader.readAsDataURL(file);
  }


  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  removerImagen() {
    this.previewUrl = null;
    this.nuevaObra.imagenBase64 = '';
    this.cd.detectChanges();
  }


  guardar() {
    if (!this.nuevaObra.titulo || !this.nuevaObra.imagenBase64) {
      this.mensajeError = 'El título y la imagen son obligatorios';
      return;
    }

    const usuario = this.authService.getUser();
    if (!usuario || !usuario.localAccountId) {
        alert('Error: No se pudo identificar al usuario.');
        return;
    }

    const obraParaEnviar = {
      ...this.nuevaObra,
      id_azure: usuario.localAccountId 
    };

    this.loading = true;
    this.mensajeError = '';

    this.obrasService.crearObra(obraParaEnviar).subscribe({
      next: (res) => {
        alert('¡Obra subida con éxito!');
        this.loading = false;
        this.router.navigate(['/obras']); 
      },
      error: (err) => {
        console.error(err);
        this.mensajeError = 'Hubo un error al subir la obra.';
        this.loading = false;
        this.cd.detectChanges(); // Actualizar error si falla
      }
    });
  }

}
