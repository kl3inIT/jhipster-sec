import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { MenuItem } from 'primeng/api';
import { merge } from 'rxjs';
import { startWith, switchMap } from 'rxjs/operators';
import { AccountService } from 'app/core/auth/account.service';
import { AppNavigationLeaf, AppNavigationSection } from 'app/layout/navigation/navigation.model';
import { NavigationService } from 'app/layout/navigation/navigation.service';
import { AppMenuitem } from './app.menuitem';

@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [CommonModule, AppMenuitem, RouterModule],
  template: `<ul class="layout-menu">
    @for (item of model; track item.id ?? $index) {
      @if (!item.separator) {
        <li app-menuitem [item]="item" [root]="true"></li>
      } @else {
        <li class="menu-separator"></li>
      }
    }
  </ul>`,
})
export class AppMenu implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly accountService = inject(AccountService);
  private readonly navigationService = inject(NavigationService);
  private readonly translateService = inject(TranslateService);
  model: MenuItem[] = [];

  ngOnInit(): void {
    merge(this.accountService.getAuthenticationState(), this.translateService.onLangChange)
      .pipe(
        startWith(null),
        switchMap(() => this.navigationService.visibleTree()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(tree => {
        this.model = tree.map(section => this.toMenuSection(section));
      });
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
