import { TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MsalService } from '@azure/msal-angular';

import { Authservices } from './authservices';

describe('Authservices', () => {
  let service: Authservices;

  beforeEach(() => {
    const msalServiceMock = {
      instance: {
        getActiveAccount: vi.fn().mockReturnValue(null)
      }
    };

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        Authservices,
        {
          provide: MsalService,
          useValue: msalServiceMock
        }
      ]
    });
    service = TestBed.inject(Authservices);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should return false when user is not logged in', () => {
    const result = service.isLoggedIn();
    expect(result).toBe(false);
  });

  it('should clear localStorage on logout', () => {
    localStorage.setItem('usuario_app', JSON.stringify({ id_rol: 1 }));
    localStorage.setItem('token', 'test-token');
    
    service.logoutLocal();
    
    expect(localStorage.getItem('usuario_app')).toBeNull();
    expect(localStorage.getItem('token')).toBeNull();
    expect(service.currentUser).toBeNull();
  });
});
