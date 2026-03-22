import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { LayoutService } from 'app/layout/service/layout.service';
import { AccountService } from 'app/core/auth/account.service';
import { StateStorageService } from 'app/core/auth/state-storage.service';
import { AuthServerProvider } from 'app/core/auth/auth-jwt.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, RouterModule, ButtonModule],
  templateUrl: './app.topbar.html',
})
export class AppTopbar {
  layoutService = inject(LayoutService);
  private readonly accountService = inject(AccountService);
  private readonly stateStorageService = inject(StateStorageService);
  private readonly authServerProvider = inject(AuthServerProvider);
  private readonly router = inject(Router);

  get currentAccount() {
    return this.accountService.trackCurrentAccount()();
  }

  toggleDarkMode(): void {
    this.layoutService.layoutConfig.update(state => ({
      ...state,
      darkTheme: !state.darkTheme,
    }));
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  logout(): void {
    this.authServerProvider.logout().subscribe();
    this.accountService.authenticate(null);
    this.router.navigate(['/login']);
  }
}
