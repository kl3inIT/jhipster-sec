import { Component, computed, effect, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AppTopbar } from '../topbar/app.topbar';
import { AppSidebar } from '../sidebar/app.sidebar';
import { AppFooter } from '../footer/app.footer';
import { BreadcrumbService } from 'app/layout/navigation/breadcrumb.service';
import { LayoutService } from 'app/layout/service/layout.service';
import { AlertComponent } from 'app/shared/alert/alert.component';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    CommonModule,
    AppTopbar,
    AppSidebar,
    RouterModule,
    TranslatePipe,
    AppFooter,
    AlertComponent,
  ],
  template: `<div class="layout-wrapper" [ngClass]="containerClass()">
    <div class="layout-sidebar">
      <div class="sidebar-header">
        <a class="sidebar-logo" routerLink="/">
          <span class="sidebar-app-name">JHipster Security</span>
        </a>
      </div>
      <div class="layout-menu-container">
        <app-sidebar></app-sidebar>
      </div>
    </div>
    <div class="layout-main-container">
      <app-topbar></app-topbar>
      <div class="layout-main">
        @if (breadcrumbs().length > 0) {
          <nav class="mb-4" aria-label="Breadcrumb">
            <ol
              class="m-0 flex list-none flex-wrap items-center gap-2 p-0 text-sm text-color-secondary"
            >
              @for (breadcrumb of breadcrumbs(); track breadcrumb.labelKey) {
                <li class="flex items-center gap-2">
                  @if (breadcrumb.current || !breadcrumb.routerLink) {
                    <span class="font-semibold text-primary" aria-current="page">
                      {{ breadcrumb.labelKey | translate }}
                    </span>
                  } @else {
                    <a
                      class="text-color-secondary no-underline hover:text-primary"
                      [routerLink]="breadcrumb.routerLink"
                    >
                      {{ breadcrumb.labelKey | translate }}
                    </a>
                  }
                  @if (!$last) {
                    <span aria-hidden="true">/</span>
                  }
                </li>
              }
            </ol>
          </nav>
        }
        <jhi-alert></jhi-alert>
        <router-outlet></router-outlet>
      </div>
      <app-footer></app-footer>
    </div>
    <div class="layout-mask"></div>
  </div>`,
})
export class AppLayout {
  layoutService = inject(LayoutService);
  readonly breadcrumbs = inject(BreadcrumbService).items;

  constructor() {
    effect(() => {
      const state = this.layoutService.layoutState();
      if (state.mobileMenuActive) {
        document.body.classList.add('blocked-scroll');
      } else {
        document.body.classList.remove('blocked-scroll');
      }
    });
  }

  containerClass = computed(() => {
    const config = this.layoutService.layoutConfig();
    const state = this.layoutService.layoutState();
    return {
      'layout-overlay': config.menuMode === 'overlay',
      'layout-static': config.menuMode === 'static',
      'layout-static-inactive': state.staticMenuDesktopInactive && config.menuMode === 'static',
      'layout-overlay-active': state.overlayMenuActive,
      'layout-mobile-active': state.mobileMenuActive,
    };
  });
}
