import { Component, computed, inject, input, signal } from '@angular/core';
import { NavigationEnd, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { RippleModule } from 'primeng/ripple';
import { LayoutService } from 'app/layout/service/layout.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: '[app-menuitem]',
  imports: [CommonModule, RouterModule, RippleModule],
  template: `
    @if (root() && isVisible() && !compact()) {
      <div class="layout-menuitem-root-text">{{ item().label }}</div>
    }
    @if ((!hasRouterLink() || hasChildren()) && isVisible()) {
      <a
        [attr.href]="item().url"
        (click)="itemClick($event)"
        [ngClass]="item().class"
        [attr.target]="item().target"
        [attr.title]="compact() ? item().label : null"
        [attr.aria-label]="compact() ? item().label : null"
        tabindex="0"
        pRipple
      >
        <i [ngClass]="item().icon" class="layout-menuitem-icon"></i>
        @if (!compact() || root()) {
          <span class="layout-menuitem-text" [class.layout-menuitem-text-compact]="compact()">{{ item().label }}</span>
        }
        @if (hasChildren() && !compact()) {
          <i class="pi pi-fw pi-angle-down layout-submenu-toggler"></i>
        }
      </a>
    }
    @if (hasRouterLink() && !hasChildren() && isVisible()) {
      <a
        (click)="itemClick($event)"
        [ngClass]="item().class"
        [routerLink]="item().routerLink"
        routerLinkActive="active-route"
        [routerLinkActiveOptions]="item().routerLinkActiveOptions || { paths: 'exact', queryParams: 'ignored', matrixParams: 'ignored', fragment: 'ignored' }"
        [fragment]="item().fragment"
        [queryParamsHandling]="item().queryParamsHandling"
        [preserveFragment]="item().preserveFragment"
        [skipLocationChange]="item().skipLocationChange"
        [replaceUrl]="item().replaceUrl"
        [state]="item().state"
        [queryParams]="item().queryParams"
        [attr.target]="item().target"
        [attr.title]="compact() ? item().label : null"
        [attr.aria-label]="compact() ? item().label : null"
        tabindex="0"
        pRipple
      >
        <i [ngClass]="item().icon" class="layout-menuitem-icon"></i>
        @if (!compact() || root()) {
          <span class="layout-menuitem-text" [class.layout-menuitem-text-compact]="compact()">{{ item().label }}</span>
        }
        @if (hasChildren() && !compact()) {
          <i class="pi pi-fw pi-angle-down layout-submenu-toggler"></i>
        }
      </a>
    }
    @if (!compact() && hasChildren() && isVisible() && (root() || isActive())) {
      <ul [class.layout-root-submenulist]="root()">
        @for (child of item().items; track child?.label) {
          <li app-menuitem [item]="child" [parentPath]="fullPath()" [root]="false" [compact]="compact()" [class]="child['badgeClass']"></li>
        }
      </ul>
    }
  `,
  host: {
    '[class.active-menuitem]': 'isActive()',
    '[class.layout-root-menuitem]': 'root()',
  },
  styles: [
    `
      .p-submenu-enter {
        animation: p-animate-submenu-expand 450ms cubic-bezier(0.86, 0, 0.07, 1) forwards;
      }
      .p-submenu-leave {
        animation: p-animate-submenu-collapse 450ms cubic-bezier(0.86, 0, 0.07, 1) forwards;
      }
      @keyframes p-animate-submenu-expand {
        from { max-height: 0; overflow: hidden; }
        to { max-height: 1000px; overflow: visible; }
      }
      @keyframes p-animate-submenu-collapse {
        from { max-height: 1000px; overflow: hidden; }
        to { max-height: 0; overflow: hidden; }
      }
    `,
  ],
})
export class AppMenuitem {
  layoutService = inject(LayoutService);
  router = inject(Router);

  item = input<any>(null);
  root = input<boolean>(false);
  parentPath = input<string | null>(null);
  compact = input(false);

  isVisible = computed(() => this.item()?.visible !== false);
  hasChildren = computed(() => this.item()?.items && this.item()?.items.length > 0);
  hasRouterLink = computed(() => !!this.item()?.routerLink);

  fullPath = computed(() => {
    const itemPath = this.item()?.path;
    if (!itemPath) return this.parentPath();
    const parent = this.parentPath();
    if (parent && !itemPath.startsWith(parent)) {
      return parent + itemPath;
    }
    return itemPath;
  });

  isActive = computed(() => {
    const activePath = this.layoutService.layoutState().activePath;
    if (this.item()?.path) {
      return activePath?.startsWith(this.fullPath() ?? '') ?? false;
    }
    return false;
  });

  initialized = signal<boolean>(false);

  constructor() {
    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(() => {
      if (this.item()?.routerLink) {
        this.updateActiveStateFromRoute();
      }
    });
  }

  ngOnInit() {
    if (this.item()?.routerLink) {
      this.updateActiveStateFromRoute();
    }
  }

  ngAfterViewInit() {
    setTimeout(() => {
      this.initialized.set(true);
    });
  }

  updateActiveStateFromRoute() {
    const item = this.item();
    if (!item?.routerLink) return;

    const isRouteActive = this.router.isActive(item.routerLink[0], {
      paths: 'exact',
      queryParams: 'ignored',
      matrixParams: 'ignored',
      fragment: 'ignored',
    });

    if (isRouteActive) {
      const parentPath = this.parentPath();
      if (parentPath) {
        this.layoutService.layoutState.update(val => ({
          ...val,
          activePath: parentPath,
        }));
      }
    }
  }

  itemClick(event: Event) {
    const item = this.item();
    const config = this.layoutService.layoutConfig();
    const state = this.layoutService.layoutState();
    const shouldOpenCompactSubmenu =
      this.compact() &&
      this.root() &&
      this.layoutService.isDesktop() &&
      (config.menuMode === 'overlay' || (config.menuMode === 'static' && state.staticMenuDesktopInactive));

    if (item?.disabled) {
      event.preventDefault();
      return;
    }

    if (item?.command) {
      item.command({ originalEvent: event, item: item });
    }

    if (this.hasChildren()) {
      if (shouldOpenCompactSubmenu) {
        event.preventDefault();
        const currentTarget = event.currentTarget as HTMLElement | null;
        const itemCount = this.item()?.items?.length ?? 0;
        const estimatedPopoverHeight = Math.min(56 + itemCount * 52, 360);
        const rawTop = currentTarget ? currentTarget.getBoundingClientRect().top - 8 : 84;
        const clampedTop = Math.max(16, Math.min(rawTop, window.innerHeight - estimatedPopoverHeight - 16));
        const nextPath = this.fullPath();
        const shouldCloseCurrent = state.overlayMenuActive && state.activePath === nextPath;

        this.layoutService.layoutState.update(val => ({
          ...val,
          overlayMenuActive: !shouldCloseCurrent,
          activePath: nextPath,
          menuHoverActive: true,
          compactMenuPopoverTop: shouldCloseCurrent ? null : clampedTop,
        }));
      } else if (this.isActive()) {
        this.layoutService.layoutState.update(val => ({
          ...val,
          activePath: this.parentPath(),
        }));
      } else {
        this.layoutService.layoutState.update(val => ({
          ...val,
          activePath: this.fullPath(),
          menuHoverActive: true,
        }));
      }
    } else {
      this.layoutService.layoutState.update(val => ({
        ...val,
        overlayMenuActive: false,
        staticMenuMobileActive: false,
        mobileMenuActive: false,
        menuHoverActive: false,
        compactMenuPopoverTop: null,
      }));
    }
  }
}
