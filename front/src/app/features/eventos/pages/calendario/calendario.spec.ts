import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { Calendario } from './calendario';
import { Eventos } from '../../services/eventos';
import { Authservices } from '../../../auth/services/authservices';
import { of } from 'rxjs';

describe('Calendario', () => {
  let component: Calendario;
  let fixture: ComponentFixture<Calendario>;
  let eventosService: Eventos;

  beforeEach(async () => {
    const authServiceMock = {
      isLoggedIn: vi.fn().mockReturnValue(true),
      getUser: vi.fn().mockReturnValue({ 
        localAccountId: 'user-123',
        nombre: 'Test User' 
      })
    };

    await TestBed.configureTestingModule({
      imports: [Calendario, HttpClientTestingModule],
      providers: [
        {
          provide: Eventos,
          useValue: {
            obtenerEventos: vi.fn().mockReturnValue(of([])),
            eliminarEvento: vi.fn().mockReturnValue(of({})),
            actualizarEvento: vi.fn().mockReturnValue(of({}))
          }
        },
        {
          provide: Authservices,
          useValue: authServiceMock
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Calendario);
    component = fixture.componentInstance;
    eventosService = TestBed.inject(Eventos);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load eventos on init', () => {
    const mockEventos = [
      { 
        id_evento: 1, 
        titulo: 'Evento 1', 
        fechaInicio: '2025-12-15T10:00:00Z',
        fechaTermino: '2025-12-15T12:00:00Z',
        direccion: 'Test Address',
        tipo: { nombre: 'Taller' }
      }
    ];
    
    eventosService.obtenerEventos = vi.fn().mockReturnValue(of(mockEventos));
    
    component.ngOnInit();
    
    expect(component.eventos.length).toBeGreaterThan(0);
    expect(component.cargando).toBe(false);
  });

  it('should open editor with evento data', () => {
    const mockEvento = {
      id_evento: 1,
      titulo: 'Test Evento',
      fechaInicioRaw: '2025-12-15T10:00:00Z',
      fechaTerminoRaw: '2025-12-15T12:00:00Z',
      direccion: 'Test Address',
      ubicacion: 'Test Location',
      usuario: { id_azure: 'user-123' }
    };
    
    component.abrirEditor(mockEvento);
    
    expect(component.modoEdicion).toBe(true);
    expect(component.eventoEditando.titulo).toBe('Test Evento');
    expect(component.eventoEditando.fechaInicioInput).toBe('2025-12-15T10:00');
    expect(component.eventoEditando.id_azure).toBe('user-123');
  });

  it('should generate correct Google Maps URL', () => {
    const direccion = 'Calle Falsa 123, Santiago';
    const lat = -33.4489;
    const lng = -70.6693;
    
    window.open = vi.fn();
    
    component.verEnMapa(lat, lng, direccion);
    
    expect(window.open).toHaveBeenCalledWith(
      `https://www.google.com/maps/search/?api=1&query=${lat},${lng}`,
      '_blank'
    );
  });
});
