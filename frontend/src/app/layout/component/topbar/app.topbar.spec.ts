import { signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom, of } from 'rxjs';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';

import { AccountService } from 'app/core/auth/account.service';
import { AuthServerProvider } from 'app/core/auth/auth-jwt.service';
import { StateStorageService } from 'app/core/auth/state-storage.service';
import { LayoutService } from 'app/layout/service/layout.service';
import { AppTopbar } from './app.topbar';

describe('AppTopbar', () => {
  let translateService: TranslateService;
  let storeLocaleCalls: string[];

  beforeEach(() => {
    storeLocaleCalls = [];

    TestBed.configureTestingModule({
      imports: [AppTopbar],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideTranslateService({ lang: 'en', fallbackLang: 'en' }),
        LayoutService,
        {
          provide: AccountService,
          useValue: {
            trackCurrentAccount: () => signal(null),
            authenticate: () => undefined,
          },
        },
        {
          provide: StateStorageService,
          useValue: {
            getLocale: () => null,
            storeLocale: (language: string) => {
              storeLocaleCalls.push(language);
            },
          },
        },
        {
          provide: AuthServerProvider,
          useValue: {
            logout: () => of(null),
          },
        },
      ],
    });

    translateService = TestBed.inject(TranslateService);
    translateService.setTranslation('en', {
      layout: {
        topbar: {
          toggleMenu: 'Toggle menu',
          toggleTheme: 'Toggle theme',
          login: 'Sign in',
          logout: 'Sign out',
        },
        language: {
          en: 'English',
          vi: 'Vietnamese',
        },
      },
    });
    translateService.setTranslation('vi', {
      layout: {
        topbar: {
          toggleMenu: 'Mo menu',
          toggleTheme: 'Doi giao dien',
          login: 'Dang nhap',
          logout: 'Dang xuat',
        },
        language: {
          en: 'English',
          vi: 'Tieng Viet',
        },
      },
    });
  });

  it('stores and applies the selected language', async () => {
    const fixture = TestBed.createComponent(AppTopbar);
    const component = fixture.componentInstance;
    await firstValueFrom(translateService.use('en'));
    fixture.detectChanges();

    component.changeLanguage('vi');
    fixture.detectChanges();

    expect(storeLocaleCalls).toEqual(['vi']);
    expect(component.activeLanguage()).toBe('vi');
    expect(translateService.currentLang).toBe('vi');
  });

  it('renders translated language controls and updates pressed state', async () => {
    const fixture = TestBed.createComponent(AppTopbar);
    await firstValueFrom(translateService.use('en'));
    fixture.detectChanges();

    const nativeElement = fixture.nativeElement as HTMLElement;
    expect(nativeElement.textContent).toContain('English');
    expect(nativeElement.textContent).toContain('Vietnamese');

    const languageButtons = nativeElement.querySelectorAll('button[aria-pressed]');
    expect(languageButtons[0]?.getAttribute('aria-pressed')).toBe('false');
    expect(languageButtons[1]?.getAttribute('aria-pressed')).toBe('true');

    const component = fixture.componentInstance;
    component.changeLanguage('vi');
    fixture.detectChanges();

    expect(languageButtons[0]?.getAttribute('aria-pressed')).toBe('true');
    expect(languageButtons[1]?.getAttribute('aria-pressed')).toBe('false');
  });
});
