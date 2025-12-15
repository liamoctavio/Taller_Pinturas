import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MsalService } from '@azure/msal-angular';

import { ListaObras } from './lista-obras';
import { Obras } from '../../services/obras';
import { Authservices } from '../../../auth/services/authservices';
import { of } from 'rxjs';

describe('ListaObras', () => {
  let component: ListaObras;
  let fixture: ComponentFixture<ListaObras>;
  let obrasService: Obras;

  beforeEach(async () => {
    const msalServiceMock = {
      instance: {},
      loginPopup: vi.fn().mockResolvedValue(null),
      logout: vi.fn().mockResolvedValue(null),
      getActiveAccount: vi.fn().mockReturnValue(null)
    };

    const authServiceMock = {
      estaAutenticado: vi.fn().mockReturnValue(true),
      obtenerUsuario: vi.fn().mockReturnValue({ id: '1', nombre: 'Test' })
    };

    await TestBed.configureTestingModule({
      imports: [ListaObras, HttpClientTestingModule],
      providers: [
        {
          provide: Obras,
          useValue: {
            getObras: vi.fn().mockReturnValue(of([])),
            getObraPorId: vi.fn().mockReturnValue(of({}))
          }
        },
        {
          provide: MsalService,
          useValue: msalServiceMock
        },
        {
          provide: Authservices,
          useValue: authServiceMock
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ListaObras);
    component = fixture.componentInstance;
    obrasService = TestBed.inject(Obras);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });


  it('should allow admin to edit any obra', () => {
    const authServiceMock = TestBed.inject(Authservices);
    authServiceMock.esAdmin = vi.fn().mockReturnValue(true);
    
    const obra = { id_azure: 'different-user-id', titulo: 'Test' };
    
    expect(component.puedeEditar(obra)).toBe(true);
  });

  it('should allow user to edit their own obra', () => {
    const authServiceMock = TestBed.inject(Authservices);
    authServiceMock.esAdmin = vi.fn().mockReturnValue(false);
    authServiceMock.getUser = vi.fn().mockReturnValue({ 
      localAccountId: 'user-123' 
    });
    
    component.usuarioLogueado = { localAccountId: 'user-123' };
    const obra = { id_azure: 'user-123', titulo: 'Test' };
    
    expect(component.puedeEditar(obra)).toBe(true);
  });

  it('should not allow user to edit obra from different user', () => {
    const authServiceMock = TestBed.inject(Authservices);
    authServiceMock.esAdmin = vi.fn().mockReturnValue(false);
    
    component.usuarioLogueado = { localAccountId: 'user-123' };
    const obra = { id_azure: 'user-456', titulo: 'Test' };
    
    expect(component.puedeEditar(obra)).toBe(false);
  });

  it('should open editor with obra copy', () => {
    const mockObra = { 
      id_obra: 1, 
      titulo: 'Test Obra',
      imagenBase64: 'base64string' 
    };
    const mockEvent = new Event('click');
    mockEvent.stopPropagation = vi.fn();
    
    component.abrirEditor(mockObra, mockEvent);
    
    expect(component.modoEdicion).toBe(true);
    expect(component.obraEditando.titulo).toBe('Test Obra');
    expect(component.obraEditando.imagenBase64).toBeNull();
  });

  it('should close editor and clear obraEditando', () => {
    component.modoEdicion = true;
    component.obraEditando = { id_obra: 1, titulo: 'Test' };
    
    component.cerrarEditor();
    
    expect(component.modoEdicion).toBe(false);
    expect(component.obraEditando).toEqual({});
  });
});
