import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MenuItem } from 'primeng/api';
import { Subject, takeUntil } from 'rxjs';
import { AccountService } from 'app/core/auth/account.service';
import { Authority } from 'app/config/authority.constants';
import { AppMenuitem } from './app.menuitem';

@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [CommonModule, AppMenuitem, RouterModule],
  template: `<ul class="layout-menu">
    @for (item of model; track item.label) {
      @if (!item.separator) {
        <li app-menuitem [item]="item" [root]="true"></li>
      } @else {
        <li class="menu-separator"></li>
      }
    }
  </ul>`,
})
export class AppMenu implements OnInit, OnDestroy {
  private readonly accountService = inject(AccountService);
  private readonly destroy$ = new Subject<void>();
  model: MenuItem[] = [];

  ngOnInit(): void {
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.buildMenu());
    this.buildMenu();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  buildMenu(): void {
    const isAdmin = this.accountService.hasAnyAuthority(Authority.ADMIN);
    this.model = [
      {
        label: 'Home',
        items: [{ label: 'Dashboard', icon: 'pi pi-home', routerLink: ['/'] }],
      },
      {
        label: 'Entities',
        items: [
          { label: 'Organizations', icon: 'pi pi-building', routerLink: ['/entities/organization'] },
          { label: 'Departments', icon: 'pi pi-sitemap', routerLink: ['/entities/department'] },
          { label: 'Employees', icon: 'pi pi-users', routerLink: ['/entities/employee'] },
        ],
      },
      ...(isAdmin
        ? [
            {
              label: 'Security Admin',
              items: [
                { label: 'Roles & Permissions', icon: 'pi pi-shield', routerLink: ['/admin/security/roles'] },
                { label: 'Row Policies', icon: 'pi pi-filter', routerLink: ['/admin/security/row-policies'] },
              ],
            },
          ]
        : []),
    ];
  }
}
