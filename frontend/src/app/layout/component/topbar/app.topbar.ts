import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { LayoutService } from 'app/layout/service/layout.service';
import { AccountService } from 'app/core/auth/account.service';
import { StateStorageService } from 'app/core/auth/state-storage.service';
import { AuthServerProvider } from 'app/core/auth/auth-jwt.service';
import { LANGUAGE_DEFAULT, LANGUAGES } from 'app/config/language.constants';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, RouterModule, ButtonModule, TranslatePipe],
  templateUrl: './app.topbar.html',
})
export class AppTopbar {
  readonly languages = LANGUAGES;
  readonly activeLanguage = signal<string>(LANGUAGE_DEFAULT);

  private readonly destroyRef = inject(DestroyRef);
  layoutService = inject(LayoutService);
  private readonly accountService = inject(AccountService);
  private readonly stateStorageService = inject(StateStorageService);
  private readonly authServerProvider = inject(AuthServerProvider);
  private readonly router = inject(Router);
  private readonly translateService = inject(TranslateService);

  constructor() {
    this.activeLanguage.set(this.translateService.currentLang || this.stateStorageService.getLocale() || LANGUAGE_DEFAULT);
    this.translateService.onLangChange.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(({ lang }) => {
      this.activeLanguage.set(lang);
    });
  }

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

  changeLanguage(language: string): void {
    this.stateStorageService.storeLocale(language);
    this.translateService.use(language);
    this.activeLanguage.set(language);
  }

  logout(): void {
    this.authServerProvider.logout().subscribe();
    this.accountService.authenticate(null);
    this.router.navigate(['/login']);
  }
}
