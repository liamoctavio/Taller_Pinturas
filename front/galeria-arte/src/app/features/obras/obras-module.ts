import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { ListaObras } from './pages/lista-obras/lista-obras';
import { GestionObras } from './pages/gestion-obras/gestion-obras';
import { ReactiveFormsModule } from '@angular/forms';


const obrasRoutes: Routes = [
  { path: '', component: ListaObras },
  //paso pa agregar mas rutas
  { path: 'crear', component: GestionObras }
];

@NgModule({

  imports: [
    CommonModule,
    ListaObras,
    GestionObras,
    ReactiveFormsModule,
    RouterModule.forChild(obrasRoutes)
  ]
})
export class ObrasModule { }
