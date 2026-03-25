import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom, of } from 'rxjs';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { vi } from 'vitest';

import { NavigationService } from 'app/layout/navigation/navigation.service';
import AccessDeniedComponent from './access-denied.component';

describe('AccessDeniedComponent', () => {
  let router: Router;
  let translateService: TranslateService;
  let navigationService: {
    resolveFallbackRoute: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    navigationService = {
      resolveFallbackRoute: vi.fn(() => of('/admin/security/row-policies')),
    };

    await TestBed.configureTestingModule({
      imports: [AccessDeniedComponent],
      providers: [
        provideRouter([]),
        provideTranslateService({ lang: 'en', fallbackLang: 'en' }),
        { provide: NavigationService, useValue: navigationService },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    translateService = TestBed.inject(TranslateService);
    translateService.setTranslation('en', {
      error: {
        accessDenied: {
          blockedMessage: 'You do not have access to {{ destination }}.',
          recoveryMessage:
            'Go to {{ fallback }} or contact an administrator if you think this is a mistake.',
          goTo: 'Go to {{ destination }}',
        },
      },
      pageTitle: {
        accessDenied: 'Access denied',
      },
      global: {
        menu: {
          admin: {
            userManagement: 'User management',
          },
          entities: {
            organization: 'Organizations',
          },
        },
      },
      layout: {
        menu: {
          security: {
            rowPolicies: 'Row policies',
          },
        },
      },
    });
    await firstValueFrom(translateService.use('en'));
  });

  afterEach(() => {
    history.replaceState(null, '', '/');
  });

  it('renders blocked destination copy and a fallback CTA from current navigation state', () => {
    vi.spyOn(router, 'getCurrentNavigation').mockReturnValue({
      extras: {
        state: {
          blockedLabelKey: 'global.menu.admin.userManagement',
          sectionId: 'security',
        },
      },
    } as unknown as ReturnType<Router['getCurrentNavigation']>);

    const fixture = TestBed.createComponent(AccessDeniedComponent);

    fixture.detectChanges();

    expect(navigationService.resolveFallbackRoute).toHaveBeenCalledWith('security');
    expect(fixture.componentInstance.fallbackRoute()).toBe('/admin/security/row-policies');

    const nativeElement = fixture.nativeElement as HTMLElement;
    expect(nativeElement.textContent).toContain('User management');
    expect(nativeElement.textContent).toContain('Go to Row policies');
  });

  it('falls back to history state when no current navigation is available', () => {
    navigationService.resolveFallbackRoute.mockReturnValue(of('/entities/organization'));
    vi.spyOn(router, 'getCurrentNavigation').mockReturnValue(null);
    history.replaceState(
      {
        blockedLabelKey: 'global.menu.entities.organization',
        sectionId: 'entities',
      },
      '',
      '/accessdenied',
    );

    const fixture = TestBed.createComponent(AccessDeniedComponent);

    fixture.detectChanges();

    expect(navigationService.resolveFallbackRoute).toHaveBeenCalledWith('entities');
    expect(fixture.componentInstance.fallbackRoute()).toBe('/entities/organization');

    const nativeElement = fixture.nativeElement as HTMLElement;
    expect(nativeElement.textContent).toContain('Organizations');
    expect(nativeElement.textContent).toContain('Go to Organizations');
  });
});
