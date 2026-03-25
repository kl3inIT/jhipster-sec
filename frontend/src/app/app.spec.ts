import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { provideRouter, TitleStrategy } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import dayjs from 'dayjs';
import { provideTranslateService } from '@ngx-translate/core';

import { AccountService } from './core/auth/account.service';
import { StateStorageService } from './core/auth/state-storage.service';
import { ApplicationConfigService } from './core/config/application-config.service';
import { AppPageTitleStrategy } from './app-page-title-strategy';

class MockStateStorageService {
  storedLocale: string | null = null;

  getLocale(): string | null {
    return this.storedLocale;
  }
}

class MockAccountService {
  account = { langKey: 'en' };

  identity() {
    return of(this.account);
  }
}

class MockPageTitleStrategy {
  calls = 0;

  updateTitle(): void {
    this.calls += 1;
  }
}

describe('App', () => {
  let stateStorageService: MockStateStorageService;
  let accountService: MockAccountService;
  let appPageTitleStrategy: MockPageTitleStrategy;

  beforeEach(async () => {
    stateStorageService = new MockStateStorageService();
    accountService = new MockAccountService();
    appPageTitleStrategy = new MockPageTitleStrategy();

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideTranslateService({ lang: 'vi', fallbackLang: 'en' }),
        ApplicationConfigService,
        { provide: StateStorageService, useValue: stateStorageService },
        { provide: AccountService, useValue: accountService },
        { provide: TitleStrategy, useValue: appPageTitleStrategy },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('prefers the stored locale over the default and account language', () => {
    stateStorageService.storedLocale = 'en';
    accountService.account = { langKey: 'vi' };

    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    expect(document.documentElement.lang).toBe('en');
    expect(dayjs.locale()).toBe('en');
    expect(appPageTitleStrategy.calls).toBeGreaterThan(0);
  });

  it('falls back to the account language only when no stored locale exists', () => {
    stateStorageService.storedLocale = null;
    accountService.account = { langKey: 'en' };

    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    expect(document.documentElement.lang).toBe('en');
    expect(dayjs.locale()).toBe('en');
  });
});

describe('App with title strategy token wiring', () => {
  it('creates when the title strategy is only provided through the TitleStrategy token', async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideTranslateService({ lang: 'en', fallbackLang: 'en' }),
        ApplicationConfigService,
        { provide: TitleStrategy, useClass: AppPageTitleStrategy },
        { provide: StateStorageService, useValue: { getLocale: () => null } },
        { provide: AccountService, useValue: { identity: () => of({ langKey: 'en' }) } },
      ],
    }).compileComponents();

    expect(() => TestBed.createComponent(App)).not.toThrow();
  });
});
