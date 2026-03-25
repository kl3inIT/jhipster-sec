import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';

import { AppPageTitleStrategy } from './app-page-title-strategy';

@Component({
  template: '',
})
class DummyComponent {}

describe('AppPageTitleStrategy', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [DummyComponent],
      providers: [
        provideRouter([{ path: '', component: DummyComponent, title: 'pageTitle.home' }]),
        provideTranslateService({ lang: 'en', fallbackLang: 'en' }),
        AppPageTitleStrategy,
      ],
    });
  });

  it('translates the current route title and refreshes it after language changes', async () => {
    const router = TestBed.inject(Router);
    const translateService = TestBed.inject(TranslateService);
    const strategy = TestBed.inject(AppPageTitleStrategy);

    translateService.setTranslation('en', { pageTitle: { home: 'Home' } });
    translateService.setTranslation('vi', { pageTitle: { home: 'Trang chu' } });

    await firstValueFrom(translateService.use('en'));
    await router.navigateByUrl('/');
    strategy.updateTitle(router.routerState.snapshot);

    expect(document.title).toBe('Home');

    const titleRefresh = firstValueFrom(translateService.onLangChange);
    const useVietnamese = firstValueFrom(translateService.use('vi'));
    await useVietnamese;
    await titleRefresh;
    strategy.updateTitle(router.routerState.snapshot);

    expect(document.title).toBe('Trang chu');
  });
});
