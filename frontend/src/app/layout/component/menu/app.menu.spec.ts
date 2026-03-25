import { TestBed } from '@angular/core/testing';
import { ReplaySubject, firstValueFrom } from 'rxjs';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';

import { AppMenu } from './app.menu';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { AppNavigationSection } from 'app/layout/navigation/navigation.model';
import { APP_NAVIGATION_TREE } from 'app/layout/navigation/navigation-registry';
import { NavigationService } from 'app/layout/navigation/navigation.service';

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
  let mockNavigationService: Partial<NavigationService>;
  let visibleTree$: ReplaySubject<AppNavigationSection[]>;
  let translateService: TranslateService;

  function cloneTree(tree: readonly AppNavigationSection[]): AppNavigationSection[] {
    return tree.map(section => ({
      ...section,
      routerLink: [...section.routerLink],
      children: section.children.map(child => ({
        ...child,
        routerLink: [...child.routerLink],
      })),
    }));
  }

  function setupWithAccount(account: Account | null, visibleTree: AppNavigationSection[]) {
    authenticationState$ = new ReplaySubject<Account | null>(1);
    authenticationState$.next(account);
    visibleTree$ = new ReplaySubject<AppNavigationSection[]>(1);
    visibleTree$.next(visibleTree);
    mockAccountService = {
      getAuthenticationState: () => authenticationState$.asObservable(),
    };
    mockNavigationService = {
      visibleTree: () => visibleTree$.asObservable(),
    };

    TestBed.configureTestingModule({
      imports: [AppMenu],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideTranslateService({ lang: 'en', fallbackLang: 'en' }),
        { provide: AccountService, useValue: mockAccountService },
        { provide: NavigationService, useValue: mockNavigationService },
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

  it('renders only the visible navigation leaves from the shared navigation service', () => {
    const filteredTree = cloneTree(APP_NAVIGATION_TREE).filter(section => section.id !== 'security');
    setupWithAccount(mockAdminAccount, filteredTree);
    const fixture = TestBed.createComponent(AppMenu);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.model.map(item => item.id)).toEqual(['home', 'entities']);
    expect(component.model.find(item => item.id === 'security')).toBeUndefined();
    expect(component.model.find(item => item.id === 'entities')?.items?.map(item => item.id)).toEqual([
      'entities.organization',
      'entities.department',
      'entities.employee',
    ]);
  });

  it('keeps a section visible while filtering out an unauthorized leaf', () => {
    const filteredTree = cloneTree(APP_NAVIGATION_TREE).map(section =>
      section.id === 'entities'
        ? {
            ...section,
            children: section.children.filter(child => child.id !== 'entities.organization'),
          }
        : section,
    );
    setupWithAccount(mockAdminAccount, filteredTree);
    const fixture = TestBed.createComponent(AppMenu);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.model.find(item => item.id === 'entities')?.items?.map(item => item.id)).toEqual([
      'entities.department',
      'entities.employee',
    ]);
    expect(component.model.find(item => item.id === 'entities')?.items?.some(item => item.id === 'entities.organization')).toBe(false);
  });

  it('tracks stable path ids from navigation metadata', () => {
    setupWithAccount(mockUserAccount, cloneTree(APP_NAVIGATION_TREE));
    const fixture = TestBed.createComponent(AppMenu);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.model.find(item => item.id === 'home')?.['path']).toBe('home');
    expect(component.model.find(item => item.id === 'entities')?.['path']).toBe('entities');
    expect(component.model.find(item => item.id === 'security')?.items?.[0]?.['path']).toBe('security.users');
  });

  it('refreshes menu labels after a language change', async () => {
    setupWithAccount(mockAdminAccount, cloneTree(APP_NAVIGATION_TREE));
    const fixture = TestBed.createComponent(AppMenu);
    const component = fixture.componentInstance;
    await firstValueFrom(translateService.use('en'));
    fixture.detectChanges();

    expect(component.model[0]?.label).toBe('Home');
    const englishUserManagementItem = component.model.find(item => item.id === 'security')?.items?.find(item => item.id === 'security.users');

    expect(englishUserManagementItem?.label).toBe('User management');
    expect(englishUserManagementItem?.id).toBe('security.users');

    await firstValueFrom(translateService.use('vi'));
    fixture.detectChanges();

    expect(component.model[0]?.label).toBe('Trang chu');
    expect(component.model[1]?.label).toBe('Thuc the');
    const vietnameseUserManagementItem = component.model.find(item => item.id === 'security')?.items?.find(item => item.id === 'security.users');

    expect(vietnameseUserManagementItem?.label).toBe('Quan ly tai khoan');
    expect(vietnameseUserManagementItem?.id).toBe('security.users');
  });
});
