import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MsalModule } from '@azure/msal-angular';
import { Navbar } from './shared/components/navbar/navbar';
import { Usuarios } from './features/usuarios/services/usuarios';
import { CommonModule } from '@angular/common';
import { Footer } from './shared/components/footer/footer';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, MsalModule,Navbar, CommonModule, Footer],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit{
  protected readonly title = signal('galeria-arte');

  // Inyectamos el servicio
  private usuariosService = inject(Usuarios); //viene de usuario services
  
  // Variable para guardar los datos
  listaUsuarios: any[] = [];

  ngOnInit() {
    // Al iniciar el componente, pedimos los datos
    this.usuariosService.getUsuarios().subscribe({
      next: (datos) => {
        console.log('Datos recibidos del BFF:', datos);
        this.listaUsuarios = datos;
      },
      error: (error) => {
        console.error('Error conectando al BFF:', error);
      }
    });
  }
}







