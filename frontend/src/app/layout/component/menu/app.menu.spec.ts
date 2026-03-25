import { TestBed } from '@angular/core/testing';
import { ReplaySubject, firstValueFrom } from 'rxjs';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';

import { AppMenu } from './app.menu';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';

const mockAdminAccount: Account = new Account(
  true,
  ['ROLE_ADMIN', 'ROLE_USER'],
  'admin@test.com',
  'Admin',
  'en',
  'User',
  'admin',
  null,
);
const mockUserAccount: Account = new Account(
  true,
  ['ROLE_USER'],
  'user@test.com',
  'Regular',
  'en',
  'User',
  'user',
  null,
);

describe('AppMenu', () => {
  let mockAccountService: Partial<AccountService>;
  let authenticationState$: ReplaySubject<Account | null>;
  let translateService: TranslateService;

  function setupWithAccount(account: Account | null, isAdmin: boolean) {
    authenticationState$ = new ReplaySubject<Account | null>(1);
    authenticationState$.next(account);
    mockAccountService = {
      getAuthenticationState: () => authenticationState$.asObservable(),
      hasAnyAuthority: (_authorities: string[] | string) => isAdmin,
    };

    TestBed.configureTestingModule({
      imports: [AppMenu],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideTranslateService({ lang: 'en', fallbackLang: 'en' }),
        { provide: AccountService, useValue: mockAccountService },
      ],
    });

    translateService = TestBed.inject(TranslateService);
    translateService.setTranslation('en', {
      global: {
        menu: {
          home: 'Home',
          admin: {
            userManagement: 'User management',
          },
          entities: {
            main: 'Entities',
            organization: 'Organizations',
            department: 'Departments',
          },
        },
      },
      layout: {
        menu: {
          entities: {
            employee: 'Employees',
          },
          security: {
            main: 'Security Admin',
            roles: 'Security roles',
            rowPolicies: 'Row policies',
          },
        },
      },
    });
    translateService.setTranslation('vi', {
      global: {
        menu: {
          home: 'Trang chu',
          admin: {
            userManagement: 'Quan ly tai khoan',
          },
          entities: {
            main: 'Thuc the',
            organization: 'To chuc',
            department: 'Phong ban',
          },
        },
      },
      layout: {
        menu: {
          entities: {
            employee: 'Nhan vien',
          },
          security: {
            main: 'Quan tri bao mat',
            roles: 'Vai tro bao mat',
            rowPolicies: 'Chinh sach hang',
          },
        },
      },
    });
  }

  it('should show Security Admin section when user has ROLE_ADMIN', () => {
    setupWithAccount(mockAdminAccount, true);
    const fixture = TestBed.createComponent(AppMenu);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    const securityAdminSection = component.model.find((item) => item.id === 'security');
    expect(securityAdminSection).toBeTruthy();
    expect(securityAdminSection?.items?.some((item) => item.label === 'User management')).toBe(
      true,
    );
    expect(securityAdminSection?.items?.some((item) => item.label === 'Security roles')).toBe(true);
    expect(securityAdminSection?.items?.some((item) => item.label === 'Row policies')).toBe(true);
  });

  it('should hide Security Admin section when user is not admin', () => {
    setupWithAccount(mockUserAccount, false);
    const fixture = TestBed.createComponent(AppMenu);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    const securityAdminSection = component.model.find((item) => item.id === 'security');
    expect(securityAdminSection).toBeUndefined();
  });

  it('should always show Entities section with Organizations, Departments, Employees', () => {
    setupWithAccount(mockUserAccount, false);
    const fixture = TestBed.createComponent(AppMenu);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    const entitiesSection = component.model.find((item) => item.label === 'Entities');
    expect(entitiesSection).toBeTruthy();
    expect(entitiesSection?.items?.some((item) => item.label === 'Organizations')).toBe(true);
    expect(entitiesSection?.items?.some((item) => item.label === 'Departments')).toBe(true);
    expect(entitiesSection?.items?.some((item) => item.label === 'Employees')).toBe(true);
  });

  it('refreshes menu labels after a language change', async () => {
    setupWithAccount(mockAdminAccount, true);
    const fixture = TestBed.createComponent(AppMenu);
    const component = fixture.componentInstance;
    await firstValueFrom(translateService.use('en'));
    fixture.detectChanges();

    expect(component.model[0]?.label).toBe('Home');
    const englishUserManagementItem = component.model
      .find((item) => item.id === 'security')
      ?.items?.find(
        (item) => Array.isArray(item.routerLink) && item.routerLink.join('/') === '/admin/users',
      );

    expect(englishUserManagementItem?.label).toBe('User management');

    await firstValueFrom(translateService.use('vi'));
    fixture.detectChanges();

    expect(component.model[0]?.label).toBe('Trang chu');
    expect(component.model[1]?.label).toBe('Thuc the');
    const vietnameseUserManagementItem = component.model
      .find((item) => item.id === 'security')
      ?.items?.find(
        (item) => Array.isArray(item.routerLink) && item.routerLink.join('/') === '/admin/users',
      );

    expect(vietnameseUserManagementItem?.label).toBe('Quan ly tai khoan');
  });
});
