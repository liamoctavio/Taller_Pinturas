import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { ListaUsuarios } from './lista-usuarios';
import { Authservices } from '../../../auth/services/authservices';
import { of } from 'rxjs';

describe('ListaUsuarios', () => {
  let component: ListaUsuarios;
  let fixture: ComponentFixture<ListaUsuarios>;
  let authService: Authservices;

  beforeEach(async () => {
    const authServiceMock = {
      obtenerTodosLosUsuarios: vi.fn().mockReturnValue(of([
        { id_usuario: 1, nombre: 'Admin User', id_rol: 1 },
        { id_usuario: 2, nombre: 'Artista User', id_rol: 2 }
      ]))
    };

    await TestBed.configureTestingModule({
      imports: [ListaUsuarios, HttpClientTestingModule],
      providers: [
        {
          provide: Authservices,
          useValue: authServiceMock
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ListaUsuarios);
    component = fixture.componentInstance;
    authService = TestBed.inject(Authservices);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load usuarios on init and set correct badge classes', () => {
    component.ngOnInit();
    
    expect(component.usuarios.length).toBe(2);
    expect(component.cargando).toBe(false);
    
    // Verificar que getBadgeClass devuelve los colores correctos
    expect(component.getBadgeClass(1)).toBe('bg-danger');      // Admin
    expect(component.getBadgeClass(2)).toBe('bg-primary');     // Artista
    expect(component.getBadgeClass(3)).toBe('bg-info text-dark'); // Visitante
    expect(component.getBadgeClass('admin')).toBe('bg-danger');
  });
});
