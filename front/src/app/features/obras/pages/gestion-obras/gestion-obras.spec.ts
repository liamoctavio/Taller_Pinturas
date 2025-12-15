import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

import { GestionObras } from './gestion-obras';
import { Obras } from '../../services/obras';
import { Authservices } from '../../../auth/services/authservices';
import { Router, ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

describe('GestionObras', () => {
  let component: GestionObras;
  let fixture: ComponentFixture<GestionObras>;
  let obrasService: Obras;

  beforeEach(async () => {
    const authServiceMock = {
      isLoggedIn: vi.fn().mockReturnValue(true),
      estaAutenticado: vi.fn().mockReturnValue(true),
      obtenerUsuario: vi.fn().mockReturnValue({ id: '1', nombre: 'Test' })
    };

    const routerMock = {
      navigate: vi.fn().mockResolvedValue(true)
    };

    const activatedRouteMock = {
      snapshot: {
        params: {},
        queryParams: {}
      },
      params: of({}),
      queryParams: of({})
    };

    await TestBed.configureTestingModule({
      imports: [GestionObras, HttpClientTestingModule, ReactiveFormsModule, FormsModule],
      providers: [
        {
          provide: Obras,
          useValue: {
            getObras: vi.fn().mockReturnValue(of([])),
            getObraPorId: vi.fn().mockReturnValue(of({})),
            crearObra: vi.fn().mockReturnValue(of({}))
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

    fixture = TestBed.createComponent(GestionObras);
    component = fixture.componentInstance;
    obrasService = TestBed.inject(Obras);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty nuevaObra', () => {
    expect(component.nuevaObra.titulo).toBe('');
    expect(component.nuevaObra.descripcion).toBe('');
    expect(component.nuevaObra.imagenBase64).toBe('');
  });

  it('should show error when file is not an image', () => {
    const file = new File(['test'], 'test.txt', { type: 'text/plain' });
    component.procesarArchivo(file);
    
    expect(component.mensajeError).toBe('Solo se permiten archivos de imagen (JPG, PNG)');
  });

  it('should show error when file size exceeds 2MB', () => {
    const largeContent = new Array(3 * 1024 * 1024).join('a');
    const file = new File([largeContent], 'large.jpg', { type: 'image/jpeg' });
    component.procesarArchivo(file);
    
    expect(component.mensajeError).toBe('La imagen es muy pesada. Máximo 2MB.');
  });

  it('should remove image preview', () => {
    component.previewUrl = 'some-url';
    component.nuevaObra.imagenBase64 = 'base64string';
    
    component.removerImagen();
    
    expect(component.previewUrl).toBeNull();
    expect(component.nuevaObra.imagenBase64).toBe('');
  });

  it('should show error when saving without title', () => {
    component.nuevaObra.titulo = '';
    component.nuevaObra.imagenBase64 = 'base64';
    
    component.guardar();
    
    expect(component.mensajeError).toBe('El título y la imagen son obligatorios');
  });

  it('should show error when saving without image', () => {
    component.nuevaObra.titulo = 'Test Obra';
    component.nuevaObra.imagenBase64 = '';
    
    component.guardar();
    
    expect(component.mensajeError).toBe('El título y la imagen son obligatorios');
  });
});
