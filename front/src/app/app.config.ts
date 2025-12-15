// import { ApplicationConfig, importProvidersFrom, provideBrowserGlobalErrorListeners } from '@angular/core';
// import { provideRouter } from '@angular/router';

// import { routes } from './app.routes';
// import { provideHttpClient, withInterceptorsFromDi, HTTP_INTERCEPTORS, withFetch } from '@angular/common/http';
// import { BrowserModule } from '@angular/platform-browser';

// import { PublicClientApplication, InteractionType, BrowserCacheLocation } from '@azure/msal-browser';
// import { MsalModule, MsalService, MsalGuard, MsalInterceptor, MsalBroadcastService } from '@azure/msal-angular';

// export const appConfig: ApplicationConfig = {
//   providers: [
//     provideBrowserGlobalErrorListeners(),
//     provideRouter(routes),
//     provideHttpClient(withFetch()),

//     //La nueva forma de configurar MSAL
//     provideHttpClient(withInterceptorsFromDi()),

//     // 3. Configuración de MSAL (La "traducción" de lo que hacía tu profesor)
//     importProvidersFrom(
//       BrowserModule,
//       MsalModule.forRoot(new PublicClientApplication({
//         auth: {
//           // El ID del ejemplo de tu profesor
//           clientId: '2b2c5b78-e1fc-41e6-88e1-20d591febee0', 
//           // TU AUTHORITY (Construido con tu ID DE INQUILINO)
//           // Esto asegura que la sesión se valide contra tu directorio de Duoc/Azure
//           // authority: 'https://login.microsoftonline.com/8efa7e1c-3a42-41a2-8459-f5f7c237d8de', 
//           authority: 'https://duocpruebaazure3.b2clogin.com/duocpruebaazure3.onmicrosoft.com/B2C_1_DuocDemoAzure_Login',
//           redirectUri: 'http://localhost:4200',
//           knownAuthorities: ['duocpruebaazure3.b2clogin.com']
//         },
//         cache: {
//           cacheLocation: BrowserCacheLocation.LocalStorage,
//           storeAuthStateInCookie: true, 
//         }
//       }),
//       {
//         interactionType: InteractionType.Popup, // Usar ventanas emergentes
//         authRequest: {
//           scopes: ['user.read']
//         }
//       },
//       {
//         interactionType: InteractionType.Popup,
//         protectedResourceMap: new Map([
//           ['https://graph.microsoft.com/v1.0/me', ['user.read']]
//         ])
//       })
//     ),

//     // 4. Proveedores de Servicios (Lo que estaba en 'providers' del profesor)
//     MsalService,
//     MsalGuard,
//     MsalBroadcastService,
//     {
//       provide: HTTP_INTERCEPTORS,
//       useClass: MsalInterceptor, // Usamos el interceptor oficial de Microsoft
//       multi: true
//     }
    
//   ]
// };


import { ApplicationConfig, importProvidersFrom, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptorsFromDi, HTTP_INTERCEPTORS, withFetch } from '@angular/common/http';
import { BrowserModule } from '@angular/platform-browser';

import { PublicClientApplication, InteractionType, BrowserCacheLocation } from '@azure/msal-browser';
import { MsalModule, MsalService, MsalGuard, MsalInterceptor, MsalBroadcastService } from '@azure/msal-angular';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withFetch()),
    provideHttpClient(withInterceptorsFromDi()),

    importProvidersFrom(
      BrowserModule,
      MsalModule.forRoot(new PublicClientApplication({
        auth: {
          clientId: '2b2c5b78-e1fc-41e6-88e1-20d591febee0', 
          authority: 'https://duocpruebaazure3.b2clogin.com/duocpruebaazure3.onmicrosoft.com/B2C_1_DuocDemoAzure_Login',
          knownAuthorities: ['duocpruebaazure3.b2clogin.com'],
          
          // CAMBIO 1: DEJA DE APUNTAR A LOCALHOST
          // Usamos window.location.origin para que funcione tanto en local como en la nube automáticamente
          redirectUri: 'http://localhost:4200'
          // O si prefieres ponerlo fijo: 'http://100.48.62.106'
        },
        cache: {
          cacheLocation: BrowserCacheLocation.LocalStorage,
          storeAuthStateInCookie: true, 
        }
      }),
      {
        interactionType: InteractionType.Popup,
        authRequest: {
          scopes: ['openid', 'profile', 'email'] // Aseguramos scopes básicos
        }
      },
      {
        interactionType: InteractionType.Popup,
        
        // CAMBIO 2: AQUÍ ESTÁ LA SOLUCIÓN DEL 401
        protectedResourceMap: new Map([
          // Mantenemos Graph si lo usas
          ['https://graph.microsoft.com/v1.0/me', ['user.read']],
          
          // AGREGAMOS TU BFF EN LA NUBE
          // Angular detectará cualquier petición que empiece con esto y le pegará el Token
          // ['http://44.197.230.98:8080/bff', ['openid', 'profile', 'email']]
        ])
      })
    ),

    MsalService,
    MsalGuard,
    MsalBroadcastService,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: MsalInterceptor,
      multi: true
    }
  ]
};