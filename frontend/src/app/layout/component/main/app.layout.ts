import { Component, computed, effect, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AppTopbar } from '../topbar/app.topbar';
import { AppMenu } from '../menu/app.menu';
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
    AppMenu,
    AppSidebar,
    RouterModule,
    TranslatePipe,
    AppFooter,
    AlertComponent,
  ],
  template: `<div class="layout-wrapper" [ngClass]="containerClass()">
    @if (showCompactSidebarRail()) {
      <aside
        class="layout-sidebar-mini"
        [attr.aria-label]="'layout.brand.name' | translate"
        (mouseenter)="onDrawerRailEnter()"
        (mouseleave)="onDrawerRailLeave()"
      >
        <div class="layout-sidebar-mini-header">
          <a class="layout-sidebar-mini-logo" routerLink="/" [attr.aria-label]="'layout.brand.name' | translate">
            <span>{{ 'layout.brand.shortName' | translate }}</span>
          </a>
        </div>
        <div class="layout-sidebar-mini-content">
          <app-menu [compact]="true"></app-menu>
        </div>
      </aside>
    }
    @if (shouldRenderFullSidebar()) {
      <div class="layout-sidebar" (mouseenter)="onDrawerPanelEnter()" (mouseleave)="onDrawerPanelLeave()">
        <div class="sidebar-header">
          <a class="sidebar-logo" routerLink="/">
            <span class="sidebar-app-name">{{ 'layout.brand.name' | translate }}</span>
          </a>
        </div>
        <div class="layout-menu-container">
          <app-sidebar></app-sidebar>
        </div>
      </div>
    }
    <div class="layout-main-container">
      <app-topbar></app-topbar>
      <div class="layout-main">
        @if (breadcrumbs().length > 0) {
          <nav class="mb-0" [attr.aria-label]="'layout.breadcrumb' | translate">
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
    <div class="layout-mask" (click)="closeActiveMenu()"></div>
  </div>`,
})
export class AppLayout {
  layoutService = inject(LayoutService);
  readonly breadcrumbs = inject(BreadcrumbService).items;
  private drawerCloseTimer: ReturnType<typeof setTimeout> | null = null;
  readonly showCompactSidebarRail = computed(() => {
    const menuMode = this.layoutService.layoutConfig().menuMode;
    return menuMode === 'static' && this.layoutService.layoutState().staticMenuDesktopInactive;
  });
  readonly shouldRenderFullSidebar = computed(
    () =>
      this.layoutService.layoutConfig().menuMode === 'drawer' ||
      !this.showCompactSidebarRail() ||
      this.showDrawerSidebar(),
  );
  readonly showDrawerSidebar = computed(
    () =>
      this.layoutService.layoutConfig().menuMode === 'drawer' &&
      this.layoutService.layoutState().overlayMenuActive,
  );

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
      'layout-drawer': config.menuMode === 'drawer',
      'layout-static': config.menuMode === 'static',
      'layout-static-inactive': state.staticMenuDesktopInactive && config.menuMode === 'static',
      'layout-overlay-active': state.overlayMenuActive && config.menuMode === 'overlay',
      'layout-drawer-active': state.overlayMenuActive && config.menuMode === 'drawer',
      'layout-mobile-active': state.mobileMenuActive,
    };
  });

  closeActiveMenu(): void {
    this.layoutService.closeMenu();
  }

  onDrawerRailEnter(): void {
    if (this.layoutService.layoutConfig().menuMode !== 'drawer') {
      return;
    }

    this.clearDrawerCloseTimer();
    this.layoutService.layoutState.update(prev => ({
      ...prev,
      overlayMenuActive: true,
      mobileMenuActive: false,
    }));
  }

  onDrawerRailLeave(): void {
    this.scheduleDrawerClose();
  }

  onDrawerPanelEnter(): void {
    if (this.layoutService.layoutConfig().menuMode !== 'drawer') {
      this.clearDrawerCloseTimer();
      return;
    }

    this.clearDrawerCloseTimer();
    this.layoutService.layoutState.update(prev => ({
      ...prev,
      overlayMenuActive: true,
    }));
  }

  onDrawerPanelLeave(): void {
    this.scheduleDrawerClose();
  }

  private scheduleDrawerClose(): void {
    if (this.layoutService.layoutConfig().menuMode !== 'drawer') {
      return;
    }

    this.clearDrawerCloseTimer();
    this.drawerCloseTimer = setTimeout(() => {
      this.layoutService.closeMenu();
      this.drawerCloseTimer = null;
    }, 120);
  }

  private clearDrawerCloseTimer(): void {
    if (this.drawerCloseTimer) {
      clearTimeout(this.drawerCloseTimer);
      this.drawerCloseTimer = null;
    }
  }
}
