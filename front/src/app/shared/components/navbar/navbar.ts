import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { routes } from '../../../app.routes';
import { MsalService, MsalBroadcastService } from '@azure/msal-angular';
import { EventMessage, EventType } from '@azure/msal-browser';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { Router, RouterModule, NavigationStart, NavigationEnd, NavigationCancel, NavigationError, RouteConfigLoadStart, RouteConfigLoadEnd } from '@angular/router';
import { Authservices } from '../../../features/auth/services/authservices';


@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar implements OnInit, OnDestroy {

  usuarioLogueado: boolean = false;
  nombreUsuario: string = '';
  private _obrasPrefetched = false;
  isLoading = false;

  public authService = inject(Authservices);

  constructor(
    private msalService: MsalService,
    private router: Router,
    private msalBroadcast: MsalBroadcastService
  ) {}

  goToObras(event: Event) {
    event.preventDefault();
    console.log('Navbar: navegando a /obras');
    this.router.navigate(['/obras']);
  }

  prefetchObras() {
    if (this._obrasPrefetched) return;
    this._obrasPrefetched = true;
    import('../../../features/obras/obras-module')
      .then(() => console.log('Obras module prefetched'))
      .catch(err => {
        console.warn('Prefetch obras failed', err);
        this._obrasPrefetched = false; // allow retry
      });
  }

  private _destroying$ = new Subject<void>();

  async ngOnInit() {
    try {
      await this.msalService.instance.initialize();
    } catch (error) {
      console.log('MSAL ya estaba inicializado.');
    }

    this.msalService.instance.handleRedirectPromise().then((result) => {
      this.revisarCuenta();
    }).catch(error => {
      console.error('Error en redirect:', error);
    });

    this.msalBroadcast.msalSubject$
      .pipe(
        filter((msg: EventMessage) =>
          msg.eventType === EventType.LOGIN_SUCCESS || msg.eventType === EventType.ACQUIRE_TOKEN_SUCCESS
        ),
        takeUntil(this._destroying$)
      )
      .subscribe(() => {
        this.revisarCuenta();
      });

    this.revisarCuenta();

    this.router.events
      .pipe(takeUntil(this._destroying$))
      .subscribe(event => {
        if (event instanceof RouteConfigLoadStart || event instanceof NavigationStart) {
          this.isLoading = true;
        } else if (
          event instanceof RouteConfigLoadEnd ||
          event instanceof NavigationEnd ||
          event instanceof NavigationCancel ||
          event instanceof NavigationError
        ) {
          this.isLoading = false;
        }
      });
  }

  ngOnDestroy(): void {
    this._destroying$.next();
    this._destroying$.complete();
  }

  // Función auxiliar para revisar si hay usuario
  revisarCuenta() {
    const accounts = this.msalService.instance.getAllAccounts();
    
    if (accounts.length > 0) {
      this.msalService.instance.setActiveAccount(accounts[0]);
      this.usuarioLogueado = true;
      this.nombreUsuario = accounts[0].name || 'Usuario';
    } else {
      this.usuarioLogueado = false;
      this.nombreUsuario = '';
    }
  }

  login() {
    this.msalService.loginRedirect();
  }

  logout() {
    this.authService.logoutLocal(); 

    this.msalService.logoutPopup({
        mainWindowRedirectUri: "/"
    });
  }

}
