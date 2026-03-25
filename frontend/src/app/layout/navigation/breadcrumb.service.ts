import { Injectable, Signal, inject, signal } from '@angular/core';
import { ActivatedRouteSnapshot, NavigationEnd, Router } from '@angular/router';
import { filter, startWith } from 'rxjs/operators';

import { APP_NAVIGATION_TREE, APP_NAVIGATION_LEAVES } from './navigation-registry';

export interface BreadcrumbItem {
  labelKey: string;
  routerLink?: string[];
  current: boolean;
}

const SECTIONS_BY_ID = new Map(
  APP_NAVIGATION_TREE.map((section) => [section.id, section] as const),
);
const LEAVES_BY_ID = new Map(APP_NAVIGATION_LEAVES.map((leaf) => [leaf.id, leaf] as const));

@Injectable({ providedIn: 'root' })
export class BreadcrumbService {
  private readonly router = inject(Router);
  private readonly itemsSignal = signal<BreadcrumbItem[]>([]);

  readonly items: Signal<BreadcrumbItem[]> = this.itemsSignal.asReadonly();

  constructor() {
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        startWith(null),
      )
      .subscribe(() => {
        this.itemsSignal.set(this.buildItems(this.router.routerState.snapshot.root));
      });
  }

  private buildItems(root: ActivatedRouteSnapshot): BreadcrumbItem[] {
    const chain = this.primaryChain(root);
    const activeRoute = [...chain]
      .reverse()
      .find((route) => typeof route.data['navigationNodeId'] === 'string');
    if (!activeRoute) {
      return [];
    }

    const navigationNodeId = activeRoute.data['navigationNodeId'] as string;
    const leaf = LEAVES_BY_ID.get(navigationNodeId);
    if (!leaf || leaf.sectionId === 'home') {
      return [];
    }

    const breadcrumbs: BreadcrumbItem[] = [];
    const section = SECTIONS_BY_ID.get(leaf.sectionId);
    if (section) {
      breadcrumbs.push({
        labelKey: section.breadcrumbKey,
        routerLink: [...section.routerLink],
        current: false,
      });
    }

    const showViewState = this.shouldShowViewState(activeRoute);
    breadcrumbs.push({
      labelKey: leaf.breadcrumbKey,
      routerLink: showViewState ? [...leaf.routerLink] : undefined,
      current: !showViewState,
    });

    if (showViewState) {
      breadcrumbs.push({
        labelKey: (activeRoute.data['pageTitleKey'] as string | undefined) ?? leaf.breadcrumbKey,
        current: true,
      });
    }

    return breadcrumbs;
  }

  private primaryChain(root: ActivatedRouteSnapshot): ActivatedRouteSnapshot[] {
    const chain: ActivatedRouteSnapshot[] = [];
    let current: ActivatedRouteSnapshot | null = root;

    while (current) {
      chain.push(current);
      current = current.firstChild ?? null;
    }

    return chain;
  }

  private shouldShowViewState(route: ActivatedRouteSnapshot): boolean {
    const path = route.routeConfig?.path ?? '';
    return (
      path.includes(':') || path === 'new' || path.includes('edit') || path.includes('permissions')
    );
  }
}
