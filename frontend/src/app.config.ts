import { ApplicationConfig, inject } from '@angular/core';
import {
  NavigationError,
  provideRouter,
  Router,
  RouterFeatures,
  TitleStrategy,
  withComponentInputBinding,
  withEnabledBlockingInitialNavigation,
  withInMemoryScrolling,
  withNavigationErrorHandler,
} from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideMissingTranslationHandler, provideTranslateService } from '@ngx-translate/core';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';

import { AppPageTitleStrategy } from './app/app-page-title-strategy';
import { applyDayjsLocale } from './app/config/dayjs';
import { LANGUAGE_DEFAULT, LANGUAGE_FALLBACK } from './app/config/language.constants';
import { getTranslationLoaderValueProvider, MissingTranslationHandlerImpl, provideStaticTranslateLoader } from './app/config/translation.config';
import { appRoutes } from './app.routes';
import { httpInterceptorProviders } from 'app/core/interceptor';

applyDayjsLocale(LANGUAGE_DEFAULT);

const routerFeatures: RouterFeatures[] = [
  withComponentInputBinding(),
  withInMemoryScrolling({ anchorScrolling: 'enabled', scrollPositionRestoration: 'enabled' }),
  withEnabledBlockingInitialNavigation(),
  withNavigationErrorHandler((e: NavigationError) => {
    const router = inject(Router);
    if (e.error?.status === 403) {
      router.navigate(['/accessdenied']);
    } else if (e.error?.status === 404) {
      router.navigate(['/404']);
    } else if (e.error?.status === 401) {
      router.navigate(['/login']);
    } else {
      router.navigate(['/error']);
    }
  }),
];

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(appRoutes, ...routerFeatures),
    provideHttpClient(withInterceptorsFromDi()),
    providePrimeNG({ theme: { preset: Aura, options: { darkModeSelector: '.app-dark' } } }),
    provideTranslateService({
      loader: provideStaticTranslateLoader(),
      lang: LANGUAGE_DEFAULT,
      fallbackLang: LANGUAGE_FALLBACK,
      missingTranslationHandler: provideMissingTranslationHandler(MissingTranslationHandlerImpl),
    }),
    getTranslationLoaderValueProvider(),
    AppPageTitleStrategy,
    { provide: TitleStrategy, useExisting: AppPageTitleStrategy },
    httpInterceptorProviders,
  ],
};
