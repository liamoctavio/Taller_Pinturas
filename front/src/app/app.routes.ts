import { Component, NgModule } from '@angular/core';
import { RouterModule, Routes, PreloadAllModules } from '@angular/router';
import { Login } from './features/auth/pages/login/login';

export const routes: Routes = [

    { path: '', redirectTo: '/login', pathMatch: 'full' },

    { path: 'login', component: Login },
  {
    path: 'obras',
    loadChildren: () => import('./features/obras/obras-module').then(m => m.ObrasModule)
  },
  {
    path: 'eventos',
    loadComponent: () => import('./features/eventos/pages/calendario/calendario').then(c => c.Calendario)
  },
  { path: 'eventos/nuevo', 
    loadComponent: () => import('./features/eventos/pages/nuevo-evento/nuevo-evento').then(c => c.NuevoEvento) 
  },
  {
    path: 'usuarios',
    loadComponent: () => import('./features/usuarios/pages/lista-usuarios/lista-usuarios').then(c => c.ListaUsuarios)
  }

];

@NgModule({
  imports: [RouterModule.forRoot(routes, { preloadingStrategy: PreloadAllModules })],
  exports: [RouterModule]
})
export class AppRoutingModule { }