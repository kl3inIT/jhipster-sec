import type { ClassProvider, ValueProvider } from '@angular/core';
import { TranslateLoader } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { I18N_HASH } from './i18n-hash.generated';

import {
  getTranslationLoaderValueProvider,
  MissingTranslationHandlerImpl,
  provideStaticTranslateLoader,
  translationLoaderConfig,
  translationNotFoundMessage,
} from './translation.config';

describe('translation config', () => {
  it('formats missing translations with the required prefix', () => {
    const handler = new MissingTranslationHandlerImpl();

    expect(handler.handle({ key: 'pageTitle.home' } as never)).toBe(`${translationNotFoundMessage}[pageTitle.home]`);
  });

  it('uses the static public i18n bundles', () => {
    expect(translationLoaderConfig).toEqual({
      prefix: './i18n/',
      suffix: `.json?_=${I18N_HASH}`,
      useHttpBackend: true,
    });

    const translateLoaderProvider = provideStaticTranslateLoader() as ClassProvider;
    expect(translateLoaderProvider.provide).toBe(TranslateLoader);
    expect(translateLoaderProvider.useClass).toBe(TranslateHttpLoader);

    const loaderConfigProvider = getTranslationLoaderValueProvider() as ValueProvider;
    expect(loaderConfigProvider.useValue).toEqual(translationLoaderConfig);
  });
});
