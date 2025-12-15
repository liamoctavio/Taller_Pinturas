import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { NuevoEvento } from './nuevo-evento';
import { Eventos } from '../../services/eventos';
import { Authservices } from '../../../auth/services/authservices';
import { Router, ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

describe('NuevoEvento', () => {
  let component: NuevoEvento;
  let fixture: ComponentFixture<NuevoEvento>;
  let eventosService: Eventos;
  let router: Router;

  beforeEach(async () => {
    const authServiceMock = {
      getUser: vi.fn().mockReturnValue({ 
        localAccountId: 'user-123',
        username: 'test@example.com' 
      })
    };

    const routerMock = {
      navigate: vi.fn().mockResolvedValue(true)
    };

    const activatedRouteMock = {
      snapshot: { params: {}, queryParams: {} },
      params: of({}),
      queryParams: of({})
    };

    await TestBed.configureTestingModule({
      imports: [NuevoEvento, HttpClientTestingModule],
      providers: [
        {
          provide: Eventos,
          useValue: {
            crearEvento: vi.fn().mockReturnValue(of({}))
          }
        },
        {
          provide: Authservices,
          useValue: authServiceMock
        },
        {
          provide: Router,
          useValue: routerMock
        },
        {
          provide: ActivatedRoute,
          useValue: activatedRouteMock
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NuevoEvento);
    component = fixture.componentInstance;
    eventosService = TestBed.inject(Eventos);
    router = TestBed.inject(Router);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty nuevoEvento', () => {
    expect(component.nuevoEvento.titulo).toBe('');
    expect(component.nuevoEvento.descripcion).toBe('');
    expect(component.nuevoEvento.id_azure).toBeNull();
  });

  it('should show alert when required fields are missing', () => {
    window.alert = vi.fn();
    
    component.nuevoEvento.titulo = '';
    component.nuevoEvento.fechaInicio = '';
    component.nuevoEvento.direccion = '';
    
    component.guardar();
    
    expect(window.alert).toHaveBeenCalledWith('Por favor completa Título, Fecha Inicio y Dirección');
  });

  it('should format date correctly', () => {
    const fechaHtml = '2025-12-15T10:00';
    const fechaFormateada = component['formatearFecha'](fechaHtml);
    
    expect(fechaFormateada).toBe('2025-12-15T10:00:00Z');
  });

  it('should create event with user id_azure', () => {
    component.nuevoEvento.titulo = 'Test Evento';
    component.nuevoEvento.fechaInicio = '2025-12-15T10:00';
    component.nuevoEvento.direccion = 'Test Address';
    
    component.guardar();
    
    expect(component.nuevoEvento.id_azure).toBe('user-123');
    expect(eventosService.crearEvento).toHaveBeenCalled();
  });
});
