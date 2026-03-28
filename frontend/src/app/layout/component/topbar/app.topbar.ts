import { Component, DestroyRef, ElementRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule, DOCUMENT } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { filter, fromEvent } from 'rxjs';
import { LayoutService } from 'app/layout/service/layout.service';
import { AccountService } from 'app/core/auth/account.service';
import { AuthServerProvider } from 'app/core/auth/auth-jwt.service';
import { AppConfigurator } from './app.configurator';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, RouterModule, ButtonModule, TranslatePipe, AppConfigurator],
  templateUrl: './app.topbar.html',
})
export class AppTopbar {
  readonly settingsOpen = signal(false);
  readonly settingsWrapper = viewChild<ElementRef<HTMLElement>>('settingsWrapper');

  layoutService = inject(LayoutService);
  private readonly accountService = inject(AccountService);
  private readonly authServerProvider = inject(AuthServerProvider);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);

  constructor() {
    fromEvent<MouseEvent>(this.document, 'click')
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        filter(() => this.settingsOpen()),
      )
      .subscribe(event => {
        const target = event.target;
        const wrapper = this.settingsWrapper()?.nativeElement;

        if (target instanceof Node && wrapper && !wrapper.contains(target)) {
          this.settingsOpen.set(false);
        }
      });
  }

  get currentAccount() {
    return this.accountService.trackCurrentAccount()();
  }

  toggleSettings(event: MouseEvent): void {
    event.stopPropagation();
    this.settingsOpen.update(open => !open);
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
