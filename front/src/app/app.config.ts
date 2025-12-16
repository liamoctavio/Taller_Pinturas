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
          
          clientId: '2b2c5b78-e1fc-41e6-88e1-20d591febee0', // Client ID de la aplicación registrada en Azure AD B2C
          authority: 'https://duocpruebaazure3.b2clogin.com/duocpruebaazure3.onmicrosoft.com/B2C_1_DuocDemoAzure_Login', // Autoridad con el User Flow de B2C
          knownAuthorities: ['duocpruebaazure3.b2clogin.com'],
          
          // Usamos window.location.origin para que funcione tanto en local como en la nube automáticamente
          redirectUri: 'http://localhost:4200'

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
        

        protectedResourceMap: new Map([

          ['https://graph.microsoft.com/v1.0/me', ['user.read']],
          
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