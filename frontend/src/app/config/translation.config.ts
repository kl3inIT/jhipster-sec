import type { Provider, ValueProvider } from '@angular/core';
import { MissingTranslationHandler, provideTranslateLoader, type MissingTranslationHandlerParams } from '@ngx-translate/core';
import { TRANSLATE_HTTP_LOADER_CONFIG, TranslateHttpLoader } from '@ngx-translate/http-loader';
import { I18N_HASH } from './i18n-hash.generated';

export const translationNotFoundMessage = 'translation-not-found';
export const translationLoaderConfig = {
  prefix: './i18n/',
  suffix: `.json?_=${I18N_HASH}`,
  useHttpBackend: true,
} as const satisfies { prefix: string; suffix: string; useHttpBackend: boolean };

export class MissingTranslationHandlerImpl implements MissingTranslationHandler {
  handle(params: MissingTranslationHandlerParams): string {
    return `${translationNotFoundMessage}[${params.key}]`;
  }
}

export const provideStaticTranslateLoader = (): Provider => provideTranslateLoader(TranslateHttpLoader);

export const getTranslationLoaderValueProvider = (): ValueProvider => ({
  provide: TRANSLATE_HTTP_LOADER_CONFIG,
  useValue: translationLoaderConfig,
});
