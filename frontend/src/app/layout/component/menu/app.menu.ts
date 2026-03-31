import { Component, DestroyRef, OnInit, computed, inject, input, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { RouterModule } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { MenuItem } from 'primeng/api';
import { TieredMenu, TieredMenuModule } from 'primeng/tieredmenu';
import { merge } from 'rxjs';
import { startWith, switchMap } from 'rxjs/operators';
import { AccountService } from 'app/core/auth/account.service';
import { LayoutService } from 'app/layout/service/layout.service';
import { AppNavigationLeaf, AppNavigationSection } from 'app/layout/navigation/navigation.model';
import { NavigationService } from 'app/layout/navigation/navigation.service';
import { AppMenuitem } from './app.menuitem';

@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [CommonModule, AppMenuitem, RouterModule, TieredMenuModule],
  template: `
    @if (compact()) {
      <ul class="layout-menu layout-menu-compact" [class.layout-menu-compact-drawer]="drawerCompact()">
        @for (item of model(); track item.id ?? $index) {
          <li class="layout-root-menuitem" [class.active-menuitem]="activeCompactRootId() === item.id">
            <a
              [attr.href]="item.url"
              [attr.title]="item.label"
              [attr.aria-label]="item.label"
              (mouseenter)="onCompactItemHover($event, item)"
              (click)="onCompactItemClick($event, item)"
              tabindex="0"
            >
              <i [ngClass]="item.icon" class="layout-menuitem-icon"></i>
              @if (!drawerCompact()) {
                <span class="layout-menuitem-text layout-menuitem-text-compact">{{ item.label }}</span>
              }
            </a>
          </li>
        }
      </ul>
      @if (!drawerCompact()) {
        <p-tieredmenu
          #compactPopupMenu
          [model]="compactPopupItems()"
          [popup]="true"
          appendTo="body"
          styleClass="app-compact-tieredmenu"
          (onHide)="onCompactPopupHide()"
        />
      }
    } @else {
      <ul class="layout-menu">
        @for (item of model(); track item.id ?? $index) {
          @if (!item.separator) {
            <li app-menuitem [item]="item" [root]="true"></li>
          } @else {
            <li class="menu-separator"></li>
          }
        }
      </ul>
    }
  `,
})
export class AppMenu implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly accountService = inject(AccountService);
  private readonly layoutService = inject(LayoutService);
  private readonly navigationService = inject(NavigationService);
  private readonly router = inject(Router);
  private readonly translateService = inject(TranslateService);
  readonly compact = input(false);
  readonly compactPopupMenu = viewChild<TieredMenu>('compactPopupMenu');
  readonly model = signal<MenuItem[]>([]);
  readonly activeCompactRootId = signal<string | null>(null);
  readonly compactPopupItems = signal<MenuItem[]>([]);
  readonly drawerCompact = computed(
    () => this.compact() && this.layoutService.layoutConfig().menuMode === 'drawer',
  );

  ngOnInit(): void {
    merge(this.accountService.getAuthenticationState(), this.translateService.onLangChange)
      .pipe(
        startWith(null),
        switchMap(() => this.navigationService.visibleTree()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(tree => {
        this.model.set(tree.map(section => this.toMenuSection(section)));
      });

  }

  onCompactItemClick(event: MouseEvent, item: MenuItem): void {
    event.preventDefault();
    event.stopPropagation();

    if (this.drawerCompact()) {
      this.activeCompactRootId.set(item.id ?? null);
      this.layoutService.layoutState.update(prev => ({
        ...prev,
        overlayMenuActive: true,
        menuHoverActive: true,
      }));
      return;
    }

    if (item.items?.length) {
      this.openCompactPopup(event, item, true);
      return;
    }

    if (item.routerLink) {
      void this.router.navigate(item.routerLink as string[]);
    }
  }

  onCompactItemHover(event: MouseEvent, item: MenuItem): void {
    if (this.drawerCompact()) {
      this.activeCompactRootId.set(item.id ?? null);
      this.layoutService.layoutState.update(prev => ({
        ...prev,
        overlayMenuActive: true,
        menuHoverActive: true,
      }));
      return;
    }

    if (!item.items?.length) {
      return;
    }

    this.openCompactPopup(event, item, false);
  }

  onCompactPopupHide(): void {
    this.activeCompactRootId.set(null);
  }

  private openCompactPopup(event: MouseEvent, item: MenuItem, toggleIfSameRoot: boolean): void {
    const popup = this.compactPopupMenu();
    if (!popup) {
      return;
    }
    const popupEvent = this.toPopupEvent(event);

    const isSameRoot = this.activeCompactRootId() === item.id;
    const isVisible = popup.visible;

    if (isSameRoot && isVisible && !toggleIfSameRoot) {
      return;
    }

    this.compactPopupItems.set(item.items as MenuItem[]);

    if (isSameRoot && isVisible && toggleIfSameRoot) {
      popup.hide(popupEvent, true);
      this.activeCompactRootId.set(null);
      return;
    }

    if (isVisible) {
      popup.hide(popupEvent, true);
    }

    this.activeCompactRootId.set(item.id ?? null);
    popup.show(popupEvent);
  }

  private toPopupEvent(event: MouseEvent): MouseEvent {
    const anchor = (event.currentTarget as HTMLElement | null) ?? (event.target as HTMLElement | null);

    if (!anchor) {
      return event;
    }

    return {
      ...event,
      currentTarget: anchor,
      target: anchor,
    } as MouseEvent;
  }

  private toMenuSection(section: AppNavigationSection): MenuItem {
    return {
      id: section.id,
      label: this.translateService.instant(section.labelKey),
      icon: section.icon,
      path: section.id,
      routerLink: [...section.routerLink],
      items: section.children.map(child => this.toMenuLeaf(child)),
    };
  }

  private toMenuLeaf(node: AppNavigationLeaf): MenuItem {
    return {
      id: node.id,
      label: this.translateService.instant(node.labelKey),
      icon: node.icon,
      path: node.id,
      routerLink: [...node.routerLink],
    };
  }
}
