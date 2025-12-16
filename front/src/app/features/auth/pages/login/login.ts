import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { Authservices } from '../../services/authservices';

@Component({
  selector: 'app-login',
  imports: [],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {

  constructor(
    private router: Router,
    private msalService: MsalService, // 2. Inyectamos el servicio aquí
    private miAuthService: Authservices // servicio propio de auth
  ) {}

  async login() {
    try {
      await this.msalService.instance.initialize();
    } catch (error) {
      console.log('Nota: Instancia ya iniciada');
    }

    this.msalService.loginPopup({
      scopes: ["openid", "profile", "email"] 
    })
      .subscribe({
        next: (result) => {
          console.log('1️⃣ Login Azure: ÉXITO', result);
          this.msalService.instance.setActiveAccount(result.account);

          // Guardamos el token crudo para que 'eventos.ts' lo pueda leer
          const tokenParaAPI = result.accessToken || result.idToken;
          localStorage.setItem('token', tokenParaAPI);
          console.log('🔑 Token guardado correctamente en localStorage como "token"');
          // =======================================================


          // Extracción de datos (simplificada para no fallar)
          const claims = result.idTokenClaims as any;
          const email = claims?.email || claims?.emails?.[0] || result.account?.username || 'no-email';
          const nombre = claims?.name || result.account?.name || 'no-name';

          const datosUsuario = {
            id_azure: result.account.localAccountId, 
            username: email, 
            nombre_completo: nombre
          };

          console.log('2️⃣ Intentando Sincronizar con Backend:', datosUsuario);

          // PASO CRÍTICO 1: Sincronizar
          this.miAuthService.sincronizarUsuario(datosUsuario).subscribe({
            next: (res) => {
                console.log('3️⃣ Sincronización: ÉXITO', res);
                console.log('4️⃣ Intentando pedir Perfil Completo (ROL) para ID:', datosUsuario.id_azure);

                // PASO CRÍTICO 2: Obtener Perfil
                this.miAuthService.obtenerPerfilDeBaseDeDatos(datosUsuario.id_azure).subscribe({
                    next: (usuarioCompleto) => {
                        console.log('5️⃣ Perfil Recibido:', usuarioCompleto);
                        
                        // AQUÍ ES DONDE SE GUARDA EL USUARIO (PERO NO EL TOKEN)
                        this.miAuthService.currentUser = usuarioCompleto;
                        localStorage.setItem('usuario_app', JSON.stringify(usuarioCompleto));
                        console.log('6️⃣ ✅ ¡GUARDADO EN LOCALSTORAGE!');

                        this.router.navigate(['/obras']);
                    },
                    error: (err) => {
                        // SI ENTRA AQUÍ, ES PORQUE FALLÓ EL GET DEL PERFIL
                        console.error('❌ FALLÓ LA OBTENCIÓN DEL PERFIL (GET /usuarios/{id})');
                        console.error('Detalle del error:', err);
                        
                        // Plan B: Guardamos lo que tenemos aunque no tenga rol
                        const usuarioBasico = { ...datosUsuario, id_rol: 2 }; // Asumimos Artista por defecto
                        localStorage.setItem('usuario_app', JSON.stringify(usuarioBasico));
                        console.log('⚠️ Guardado perfil básico de emergencia');
                        
                        this.router.navigate(['/obras']);
                    }
                });
            },
            error: (err) => {
                console.error('❌ FALLÓ LA SINCRONIZACIÓN (POST /usuarios/sync)');
                console.error(err);
                alert('Error de conexión con el servidor.');
            }
          });
        },
        error: (error) => {
          console.error('Error Login Microsoft:', error);
        }
      });
  }

  entrarComoVisitante() {
    this.router.navigate(['/obras']);
  }


  



}
