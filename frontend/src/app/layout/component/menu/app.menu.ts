import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { MenuItem } from 'primeng/api';
import { merge } from 'rxjs';
import { AccountService } from 'app/core/auth/account.service';
import { Authority } from 'app/config/authority.constants';
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
  private readonly translateService = inject(TranslateService);
  model: MenuItem[] = [];

  ngOnInit(): void {
    merge(this.accountService.getAuthenticationState(), this.translateService.onLangChange)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.buildMenu());
    this.buildMenu();
  }

  buildMenu(): void {
    const isAdmin = this.accountService.hasAnyAuthority(Authority.ADMIN);
    this.model = [
      {
        id: 'home',
        label: this.translateService.instant('global.menu.home'),
        items: [
          {
            label: this.translateService.instant('global.menu.home'),
            icon: 'pi pi-home',
            routerLink: ['/'],
          },
        ],
      },
      {
        id: 'entities',
        label: this.translateService.instant('global.menu.entities.main'),
        items: [
          {
            label: this.translateService.instant('global.menu.entities.organization'),
            icon: 'pi pi-building',
            routerLink: ['/entities/organization'],
          },
          {
            label: this.translateService.instant('global.menu.entities.department'),
            icon: 'pi pi-sitemap',
            routerLink: ['/entities/department'],
          },
          {
            label: this.translateService.instant('layout.menu.entities.employee'),
            icon: 'pi pi-users',
            routerLink: ['/entities/employee'],
          },
        ],
      },
      ...(isAdmin
        ? [
            {
              id: 'security',
              label: this.translateService.instant('layout.menu.security.main'),
              items: [
                {
                  label: this.translateService.instant('global.menu.admin.userManagement'),
                  icon: 'pi pi-users',
                  routerLink: ['/admin/users'],
                },
                {
                  label: this.translateService.instant('layout.menu.security.roles'),
                  icon: 'pi pi-shield',
                  routerLink: ['/admin/security/roles'],
                },
                {
                  label: this.translateService.instant('layout.menu.security.rowPolicies'),
                  icon: 'pi pi-filter',
                  routerLink: ['/admin/security/row-policies'],
                },
              ],
            },
          ]
        : []),
    ];
  }
}
