import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterModule } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { ApplicationConfigService } from './core/config/application-config.service';
import { AccountService } from './core/auth/account.service';
import { StateStorageService } from './core/auth/state-storage.service';
import { Router, TitleStrategy } from '@angular/router';
import { environment } from 'environments/environment';
import { applyDayjsLocale } from './config/dayjs';
import { LANGUAGE_DEFAULT, LANGUAGES } from './config/language.constants';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterModule],
  template: '<router-outlet></router-outlet>',
})
export class App {
  private readonly destroyRef = inject(DestroyRef);
  private readonly applicationConfigService = inject(ApplicationConfigService);
  private readonly translateService = inject(TranslateService);
  private readonly accountService = inject(AccountService);
  private readonly stateStorageService = inject(StateStorageService);
  private readonly router = inject(Router);
  private readonly titleStrategy = inject(TitleStrategy);

  constructor() {
    this.applicationConfigService.setEndpointPrefix(environment.SERVER_API_URL ?? '');

    this.translateService.addLangs([...LANGUAGES]);
    this.translateService.onLangChange.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(({ lang }) => {
      this.applyLanguageState(lang);
    });

    const initialLanguage = this.stateStorageService.getLocale() ?? LANGUAGE_DEFAULT;
    this.translateService.use(initialLanguage);
    this.applyLanguageState(initialLanguage);

    this.accountService
      .identity()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(account => {
        if (!this.stateStorageService.getLocale() && account?.langKey) {
          this.translateService.use(account.langKey);
        }
      });
  }

  private applyLanguageState(language: string): void {
    document.documentElement.lang = language;
    applyDayjsLocale(language);
    this.titleStrategy.updateTitle(this.router.routerState.snapshot);
  }
}
