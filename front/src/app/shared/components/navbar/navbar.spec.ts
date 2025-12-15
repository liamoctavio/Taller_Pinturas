import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { MsalService, MsalBroadcastService } from '@azure/msal-angular';

import { Navbar } from './navbar';
import { Authservices } from '../../../features/auth/services/authservices';
import { Router, ActivatedRoute } from '@angular/router';
import { of, Subject } from 'rxjs';

describe('Navbar', () => {
  let component: Navbar;
  let fixture: ComponentFixture<Navbar>;
  let router: Router;

  beforeEach(async () => {
    const msalServiceMock = {
      instance: {
        initialize: vi.fn().mockResolvedValue(undefined),
        handleRedirectPromise: vi.fn().mockResolvedValue(null),
        getActiveAccount: vi.fn().mockReturnValue(null),
        getAllAccounts: vi.fn().mockReturnValue([])
      }
    };

    const msalBroadcastMock = {
      msalSubject$: new Subject()
    };

    const authServiceMock = {
      isLoggedIn: vi.fn().mockReturnValue(false),
      getUserName: vi.fn().mockReturnValue('')
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
      imports: [Navbar],
      providers: [
        {
          provide: MsalService,
          useValue: msalServiceMock
        },
        {
          provide: MsalBroadcastService,
          useValue: msalBroadcastMock
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

    fixture = TestBed.createComponent(Navbar);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
  });

  // it('should create', () => {
  //   expect(component).toBeTruthy();
  // });

  // it('should navigate to /obras when goToObras is called', () => {
  //   const mockEvent = new Event('click');
  //   mockEvent.preventDefault = vi.fn();
    
  //   component.goToObras(mockEvent);
    
  //   expect(mockEvent.preventDefault).toHaveBeenCalled();
  //   expect(router.navigate).toHaveBeenCalledWith(['/obras']);
  // });
});
