import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { MsalService } from '@azure/msal-angular';

import { Login } from './login';
import { Authservices } from '../../services/authservices';
import { Router } from '@angular/router';
import { of } from 'rxjs';

describe('Login', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;
  let msalService: MsalService;
  let authService: Authservices;

  beforeEach(async () => {
    const msalServiceMock = {
      instance: {
        initialize: vi.fn().mockResolvedValue(undefined),
        setActiveAccount: vi.fn(),
        getActiveAccount: vi.fn().mockReturnValue(null),
        getAllAccounts: vi.fn().mockReturnValue([])
      },
      loginPopup: vi.fn().mockReturnValue(of({
        account: {
          localAccountId: 'user-123',
          username: 'test@example.com',
          name: 'Test User'
        },
        idTokenClaims: {
          email: 'test@example.com',
          name: 'Test User'
        }
      }))
    };

    const authServiceMock = {
      sincronizarUsuario: vi.fn().mockReturnValue(of({}))
    };

    const routerMock = {
      navigate: vi.fn().mockResolvedValue(true)
    };

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        {
          provide: MsalService,
          useValue: msalServiceMock
        },
        {
          provide: Authservices,
          useValue: authServiceMock
        },
        {
          provide: Router,
          useValue: routerMock
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    msalService = TestBed.inject(MsalService);
    authService = TestBed.inject(Authservices);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have MsalService injected', () => {
    expect(msalService).toBeDefined();
    expect(msalService.instance).toBeDefined();
  });

  it('should have Authservices injected', () => {
    expect(authService).toBeDefined();
    expect(authService.sincronizarUsuario).toBeDefined();
  });
});
