import { signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { firstValueFrom, of } from 'rxjs';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';

import { AccountService } from 'app/core/auth/account.service';
import { AuthServerProvider } from 'app/core/auth/auth-jwt.service';
import { StateStorageService } from 'app/core/auth/state-storage.service';
import { LayoutService } from 'app/layout/service/layout.service';
import { AppConfigurator } from './app.configurator';
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
          openSettings: 'Open settings',
          login: 'Sign in',
          logout: 'Sign out',
        },
        settings: {
          title: 'Settings',
          subtitle: 'Language and theme controls',
          language: 'Language',
          preset: 'Preset',
          primary: 'Primary',
          surface: 'Surface',
          appearance: {
            label: 'Appearance',
            light: 'Light',
            dark: 'Dark',
          },
          menuMode: {
            label: 'Menu mode',
            static: 'Static',
            overlay: 'Overlay',
          },
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
          openSettings: 'Mo cai dat',
          login: 'Dang nhap',
          logout: 'Dang xuat',
        },
        settings: {
          title: 'Cai dat',
          subtitle: 'Ngon ngu va giao dien',
          language: 'Ngon ngu',
          preset: 'Preset',
          primary: 'Mau chinh',
          surface: 'Nen',
          appearance: {
            label: 'Giao dien',
            light: 'Sang',
            dark: 'Toi',
          },
          menuMode: {
            label: 'Che do menu',
            static: 'Static',
            overlay: 'Overlay',
          },
        },
        language: {
          en: 'English',
          vi: 'Tieng Viet',
        },
      },
    });
  });

  it('stores and applies the selected language from the settings panel', async () => {
    const fixture = TestBed.createComponent(AppTopbar);
    await firstValueFrom(translateService.use('en'));
    fixture.detectChanges();

    fixture.componentInstance.settingsOpen.set(true);
    fixture.detectChanges();

    const configurator = fixture.debugElement.query(By.directive(AppConfigurator)).componentInstance as AppConfigurator;
    configurator.changeLanguage('vi');
    fixture.detectChanges();

    expect(storeLocaleCalls).toEqual(['vi']);
    expect(configurator.activeLanguage()).toBe('vi');
    expect(translateService.currentLang).toBe('vi');
  });

  it('renders the settings trigger and toggles the panel content', async () => {
    const fixture = TestBed.createComponent(AppTopbar);
    await firstValueFrom(translateService.use('en'));
    fixture.detectChanges();

    const nativeElement = fixture.nativeElement as HTMLElement;
    const settingsButton = nativeElement.querySelector('button[aria-label="Open settings"]') as HTMLButtonElement;

    expect(settingsButton).not.toBeNull();
    expect(nativeElement.textContent).not.toContain('Settings');

    settingsButton.click();
    fixture.detectChanges();

    expect(nativeElement.textContent).toContain('Settings');
    expect(nativeElement.textContent).toContain('English');
    expect(nativeElement.textContent).toContain('Vietnamese');
  });
});
