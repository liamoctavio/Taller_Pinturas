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

 
//esto funciona perfecto
  // async login() {
  //   try {
  //     await this.msalService.instance.initialize();
  //   } catch (error) {
  //     console.log('Nota: La instancia ya estaba inicializada o en proceso.');
  //   }

  //   // CORRECCIÓN: Quitamos "User.Read" y dejamos los estándar
  //   this.msalService.loginPopup({
  //     scopes: ["openid", "profile", "email"] 
  //   })
  //     .subscribe({
  //       next: (result) => {
  //         console.log('Login Exitoso. Resultado:', result);
  //         this.msalService.instance.setActiveAccount(result.account);

  //         const claims = result.idTokenClaims as any;
  //         console.log('CLAIMS RECIBIDOS:', claims); // <--- Mira esto en consola

  //         // =========================================================
  //         // LÓGICA ROBUSTA PARA AZURE B2C / EXTERNAL
  //         // =========================================================
          
  //         // 1. Email: Azure B2C a veces lo manda como 'emails' (array)
  //         let emailFinal = '';
  //         if (claims?.email) {
  //            emailFinal = claims.email;
  //         } else if (claims?.emails && claims.emails.length > 0) {
  //            emailFinal = claims.emails[0]; // Toma el primero de la lista
  //         } else if (claims?.preferred_username) {
  //            emailFinal = claims.preferred_username;
  //         } else {
  //            emailFinal = result.account?.username || '';
  //         }

  //         // 2. Nombre:
  //         let nombreFinal = '';
  //         if (claims?.name) {
  //            nombreFinal = claims.name;
  //         } else if (result.account?.name) {
  //            nombreFinal = result.account.name;
  //         } else {
  //            nombreFinal = emailFinal; // Peor caso: usa el email como nombre
  //         }

  //         const datosUsuario = {
  //           id_azure: result.account.localAccountId, 
  //           username: emailFinal, 
  //           nombre_completo: nombreFinal
  //         };

  //         console.log('Enviando al Backend:', datosUsuario);

  //         // Si el email sigue vacío, no tiene sentido enviar
  //         if (!datosUsuario.username) {
  //            alert('Advertencia: No se pudo recuperar el email del usuario.');
  //         }

  //         this.miAuthService.sincronizarUsuario(datosUsuario).subscribe({
  //           next: (res) => console.log('✅ Sincronizado:', res),
  //           error: (err) => console.error('❌ Error sync:', err)
  //         });
          
  //         this.router.navigate(['/obras']);
  //       },
  //       error: (error) => {
  //         console.error('Error Login:', error);
  //         alert('Error al iniciar sesión.');
  //       }
  //     });
  // }
  // async login() {
  //   try {
  //     await this.msalService.instance.initialize();
  //   } catch (error) {
  //     console.log('Nota: La instancia ya estaba inicializada o en proceso.');
  //   }

  //   // CORRECCIÓN: Quitamos "User.Read" y dejamos los estándar
  //   this.msalService.loginPopup({
  //     scopes: ["openid", "profile", "email"] 
  //   })
  //     .subscribe({
  //       next: (result) => {
  //         console.log('Login Exitoso. Resultado:', result);
  //         this.msalService.instance.setActiveAccount(result.account);

  //         const claims = result.idTokenClaims as any;
  //         console.log('CLAIMS RECIBIDOS:', claims);

  //         // =========================================================
  //         // LÓGICA ROBUSTA PARA AZURE B2C / EXTERNAL
  //         // =========================================================
          
  //         let emailFinal = '';
  //         if (claims?.email) {
  //            emailFinal = claims.email;
  //         } else if (claims?.emails && claims.emails.length > 0) {
  //            emailFinal = claims.emails[0];
  //         } else if (claims?.preferred_username) {
  //            emailFinal = claims.preferred_username;
  //         } else {
  //            emailFinal = result.account?.username || '';
  //         }

  //         let nombreFinal = '';
  //         if (claims?.name) {
  //            nombreFinal = claims.name;
  //         } else if (result.account?.name) {
  //            nombreFinal = result.account.name;
  //         } else {
  //            nombreFinal = emailFinal;
  //         }

  //         const datosUsuario = {
  //           id_azure: result.account.localAccountId, 
  //           username: emailFinal, 
  //           nombre_completo: nombreFinal
  //         };

  //         console.log('Enviando al Backend:', datosUsuario);

  //         if (!datosUsuario.username) {
  //            alert('Advertencia: No se pudo recuperar el email del usuario.');
  //         }

  //         // =========================================================
  //         // AQUÍ CAMBIA LA LÓGICA: ENCADENAMOS LAS LLAMADAS
  //         // =========================================================

  //         // 1. SINCRONIZAMOS (Aseguramos que exista en BD)
  //         this.miAuthService.sincronizarUsuario(datosUsuario).subscribe({
  //           next: (res) => {
  //               console.log('✅ Usuario sincronizado en BD:', res);

  //               // 2. AHORA PEDIMOS SU ROL (GET /usuarios/{id})
  //               // Esto es vital para saber si es ADMIN o ARTISTA
  //               this.miAuthService.obtenerPerfilDeBaseDeDatos(datosUsuario.id_azure).subscribe({
  //                   next: (usuarioCompleto) => {
  //                       console.log('👤 Perfil cargado con ROL:', usuarioCompleto);
                        
  //                       // 3. GUARDAMOS EL DATO EN EL SERVICIO (currentUser)
  //                       this.miAuthService.currentUser = usuarioCompleto;

  //                       localStorage.setItem('usuario_app', JSON.stringify(usuarioCompleto));

  //                       // 4. FINALMENTE REDIRIGIMOS
  //                       // Lo hacemos aquí adentro para asegurar que ya tenemos los permisos cargados
  //                       this.router.navigate(['/obras']);
  //                   },
  //                   error: (err) => {
  //                       console.error('⚠️ No se pudo cargar el perfil completo', err);
  //                       // Si falla la carga del perfil, dejamos pasar igual (será visitante/artista por defecto)
  //                       this.router.navigate(['/obras']);
  //                   }
  //               });
  //           },
  //           error: (err) => {
  //               console.error('❌ Error sincronizando usuario:', err);
  //               // Si falla la sincronización crítica, quizás no deberíamos dejarlo entrar, 
  //               // pero por ahora lo dejamos pasar a obras
  //               this.router.navigate(['/obras']);
  //           }
  //         });
          
  //         // NOTA: Quité el this.router.navigate de aquí abajo, 
  //         // porque ahora está adentro de los subscribes.
  //       },
  //       error: (error) => {
  //         console.error('Error Login:', error);
  //         alert('Error al iniciar sesión.');
  //       }
  //     });
  // }

  // async login() {
  //   try {
  //     await this.msalService.instance.initialize();
  //   } catch (error) {
  //     console.log('Nota: Instancia ya iniciada');
  //   }

  //   this.msalService.loginPopup({
  //     scopes: ["openid", "profile", "email"] 
  //   })
  //     .subscribe({
  //       next: (result) => {
  //         console.log('1️⃣ Login Azure: ÉXITO', result);
  //         this.msalService.instance.setActiveAccount(result.account);

  //         // Extracción de datos (simplificada para no fallar)
  //         const claims = result.idTokenClaims as any;
  //         const email = claims?.email || claims?.emails?.[0] || result.account?.username || 'no-email';
  //         const nombre = claims?.name || result.account?.name || 'no-name';

  //         const datosUsuario = {
  //           id_azure: result.account.localAccountId, 
  //           username: email, 
  //           nombre_completo: nombre
  //         };

  //         console.log('2️⃣ Intentando Sincronizar con Backend:', datosUsuario);

  //         // PASO CRÍTICO 1: Sincronizar
  //         this.miAuthService.sincronizarUsuario(datosUsuario).subscribe({
  //           next: (res) => {
  //               console.log('3️⃣ Sincronización: ÉXITO', res);

  //               console.log('4️⃣ Intentando pedir Perfil Completo (ROL) para ID:', datosUsuario.id_azure);

  //               // PASO CRÍTICO 2: Obtener Perfil
  //               this.miAuthService.obtenerPerfilDeBaseDeDatos(datosUsuario.id_azure).subscribe({
  //                   next: (usuarioCompleto) => {
  //                       console.log('5️⃣ Perfil Recibido:', usuarioCompleto);
                        
  //                       // AQUÍ ES DONDE SE GUARDA
  //                       this.miAuthService.currentUser = usuarioCompleto;
  //                       localStorage.setItem('usuario_app', JSON.stringify(usuarioCompleto));
  //                       console.log('6️⃣ ✅ ¡GUARDADO EN LOCALSTORAGE!');

  //                       this.router.navigate(['/obras']);
  //                   },
  //                   error: (err) => {
  //                       // SI ENTRA AQUÍ, ES PORQUE FALLÓ EL GET DEL PERFIL
  //                       console.error('❌ FALLÓ LA OBTENCIÓN DEL PERFIL (GET /usuarios/{id})');
  //                       console.error('Detalle del error:', err);
                        
  //                       // Plan B: Guardamos lo que tenemos aunque no tenga rol
  //                       const usuarioBasico = { ...datosUsuario, id_rol: 2 }; // Asumimos Artista por defecto
  //                       localStorage.setItem('usuario_app', JSON.stringify(usuarioBasico));
  //                       console.log('⚠️ Guardado perfil básico de emergencia');
                        
  //                       this.router.navigate(['/obras']);
  //                   }
  //               });
  //           },
  //           error: (err) => {
  //               console.error('❌ FALLÓ LA SINCRONIZACIÓN (POST /usuarios/sync)');
  //               console.error(err);
  //               alert('Error de conexión con el servidor.');
  //           }
  //         });
  //       },
  //       error: (error) => {
  //         console.error('Error Login Microsoft:', error);
  //       }
  //     });
  // }
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
