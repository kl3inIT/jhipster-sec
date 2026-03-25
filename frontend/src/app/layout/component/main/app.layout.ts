import { Component, computed, effect, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AppTopbar } from '../topbar/app.topbar';
import { AppSidebar } from '../sidebar/app.sidebar';
import { AppFooter } from '../footer/app.footer';
import { LayoutService } from 'app/layout/service/layout.service';
import { AlertComponent } from 'app/shared/alert/alert.component';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, AppTopbar, AppSidebar, RouterModule, AppFooter, AlertComponent],
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
