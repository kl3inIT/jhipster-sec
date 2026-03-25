import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';

import { AlertComponent } from 'app/shared/alert/alert.component';
import { BreadcrumbItem, BreadcrumbService } from 'app/layout/navigation/breadcrumb.service';
import { LayoutService } from 'app/layout/service/layout.service';
import { AppFooter } from '../footer/app.footer';
import { AppSidebar } from '../sidebar/app.sidebar';
import { AppTopbar } from '../topbar/app.topbar';
import { AppLayout } from './app.layout';

@Component({
  selector: 'app-topbar',
  standalone: true,
  template: '',
})
class StubTopbar {}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  template: '',
})
class StubSidebar {}

@Component({
  selector: 'app-footer',
  standalone: true,
  template: '',
})
class StubFooter {}

@Component({
  selector: 'jhi-alert',
  standalone: true,
  template: '',
})
class StubAlert {}

describe('AppLayout', () => {
  let breadcrumbItems: ReturnType<typeof signal<BreadcrumbItem[]>>;

  beforeEach(async () => {
    breadcrumbItems = signal<BreadcrumbItem[]>([]);

    await TestBed.configureTestingModule({
      imports: [AppLayout],
      providers: [
        provideRouter([]),
        provideTranslateService({ lang: 'en', fallbackLang: 'en' }),
        LayoutService,
        {
          provide: BreadcrumbService,
          useValue: {
            items: breadcrumbItems,
          },
        },
      ],
    })
      .overrideComponent(AppLayout, {
        remove: {
          imports: [AppTopbar, AppSidebar, AppFooter, AlertComponent],
        },
        add: {
          imports: [StubTopbar, StubSidebar, StubFooter, StubAlert],
        },
      })
      .compileComponents();
  });

  it('does not render the breadcrumb strip when there are no breadcrumb items', () => {
    const fixture = TestBed.createComponent(AppLayout);

    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('nav[aria-label="Breadcrumb"]')).toBeNull();
  });

  it('renders the breadcrumb strip when breadcrumb items exist', () => {
    breadcrumbItems.set([
      {
        labelKey: 'global.menu.entities.main',
        routerLink: ['/entities/organization'],
        current: false,
      },
      {
        labelKey: 'angappApp.organization.detail.title',
        current: true,
      },
    ]);
    const fixture = TestBed.createComponent(AppLayout);

    fixture.detectChanges();

    const breadcrumbNav = fixture.nativeElement.querySelector(
      'nav[aria-label="Breadcrumb"]',
    ) as HTMLElement | null;
    expect(breadcrumbNav).not.toBeNull();
    expect(breadcrumbNav?.textContent).toContain('global.menu.entities.main');
    expect(breadcrumbNav?.textContent).toContain('angappApp.organization.detail.title');
    expect(breadcrumbNav?.querySelector('[aria-current="page"]')?.textContent).toContain(
      'angappApp.organization.detail.title',
    );
  });
});
