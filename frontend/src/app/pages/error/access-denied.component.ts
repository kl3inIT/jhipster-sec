import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';

import { APP_NAVIGATION_LEAVES } from 'app/layout/navigation/navigation-registry';
import { NavigationService } from 'app/layout/navigation/navigation.service';

interface BlockedRouteState {
  blockedUrl?: string;
  blockedLabelKey?: string;
  sectionId?: string;
}

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [CardModule, ButtonModule, RouterModule, TranslatePipe],
  templateUrl: './access-denied.component.html',
})
export default class AccessDeniedComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly navigationService = inject(NavigationService);

  readonly blockedLabelKey = signal<string | null>(null);
  readonly fallbackRoute = signal('/');
  readonly fallbackLabelKey = signal('global.menu.home');

  ngOnInit(): void {
    const state = (this.router.getCurrentNavigation()?.extras.state ??
      history.state ??
      {}) as BlockedRouteState;
    this.blockedLabelKey.set(
      typeof state.blockedLabelKey === 'string' ? state.blockedLabelKey : null,
    );

    const sectionId = typeof state.sectionId === 'string' ? state.sectionId : undefined;
    this.navigationService.resolveFallbackRoute(sectionId).subscribe((route) => {
      this.fallbackRoute.set(route);
      this.fallbackLabelKey.set(
        APP_NAVIGATION_LEAVES.find((leaf) => leaf.routePrefix === route)?.labelKey ??
          'global.menu.home',
      );
    });
  }
}
