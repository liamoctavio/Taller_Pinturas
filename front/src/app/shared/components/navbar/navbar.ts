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

  // Navegación programática para evitar problemas de doble click
  goToObras(event: Event) {
    event.preventDefault();
    console.log('Navbar: navegando a /obras');
    this.router.navigate(['/obras']);
  }

  // Prefetch the lazy module for obras to reduce navigation delay
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

  // 1. Convertimos ngOnInit en ASYNC para poder esperar
  private _destroying$ = new Subject<void>();

  async ngOnInit() {
    // 2. Intentamos INICIALIZAR Azure si no lo está
    try {
      await this.msalService.instance.initialize();
    } catch (error) {
      // Si ya estaba inicializado, ignoramos el error y seguimos
      console.log('MSAL ya estaba inicializado.');
    }

    // 3. Ahora que estamos seguros, manejamos el resultado del Login
    this.msalService.instance.handleRedirectPromise().then((result) => {
      // Verificamos si hay cuentas después de volver de Microsoft
      this.revisarCuenta();
    }).catch(error => {
      console.error('Error en redirect:', error);
    });

    // 4. Nos suscribimos a los eventos de MSAL para actualizar el navbar sin recargar
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

    // 5. Revisamos la cuenta inicialmente para mostrar el estado correcto
    this.revisarCuenta();

    // 6. Escuchamos eventos del router para mostrar un spinner durante la navegación y la carga de módulos
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

  // logout() {
  //   this.msalService.logoutRedirect({
  //     postLogoutRedirectUri: 'http://localhost:4200'
  //   });
  //   this.usuarioLogueado = false;
  // }
  logout() {
    // 1. Llamamos al método del servicio que creamos en el Paso 1
    this.authService.logoutLocal(); 

    // 2. Cerramos sesión en Azure
    this.msalService.logoutPopup({
        mainWindowRedirectUri: "/"
    });
  }

}
