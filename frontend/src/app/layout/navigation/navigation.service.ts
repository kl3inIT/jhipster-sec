import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { distinctUntilChanged, map, shareReplay, tap } from 'rxjs/operators';

import { AccountService } from 'app/core/auth/account.service';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { NAVIGATION_STORAGE_KEY, SHELL_APP_NAME } from './navigation.constants';
import { APP_NAVIGATION_LEAVES, APP_NAVIGATION_TREE } from './navigation-registry';
import { AppNavigationLeaf, AppNavigationSection } from './navigation.model';

interface MenuPermissionResponse {
  appName: string;
  allowedMenuIds: string[];
}

const STORAGE_LOGIN_KEY = `${NAVIGATION_STORAGE_KEY}:login`;
const LEAF_IDS = new Set(APP_NAVIGATION_LEAVES.map(leaf => leaf.id));
const LEAVES_BY_ID = new Map(APP_NAVIGATION_LEAVES.map(leaf => [leaf.id, leaf] as const));
const SECTIONS_BY_ID = new Map(APP_NAVIGATION_TREE.map(section => [section.id, section] as const));

@Injectable({ providedIn: 'root' })
export class NavigationService {
  private readonly resourceUrl = inject(ApplicationConfigService).getEndpointFor('api/security/menu-permissions');
  private readonly http = inject(HttpClient);
  private readonly accountService = inject(AccountService);

  private cachedGrantResponse$?: Observable<MenuPermissionResponse>;
  private currentLogin: string | null = null;
  private seenAuthState = false;

  constructor() {
    this.accountService
      .getAuthenticationState()
      .pipe(
        map(account => account?.login ?? null),
        distinctUntilChanged(),
      )
      .subscribe(login => {
        const previousLogin = this.currentLogin;
        this.currentLogin = login;

        if (!this.seenAuthState) {
          this.seenAuthState = true;
          if (this.readStoredLogin() !== this.serializeLogin(login)) {
            this.clearCache();
          }
          return;
        }

        if (previousLogin !== login) {
          this.clearCache();
        }
      });
  }

  query(): Observable<MenuPermissionResponse> {
    if (!this.cachedGrantResponse$) {
      const stored = this.readFromStorage();
      if (stored) {
        this.cachedGrantResponse$ = of(stored).pipe(shareReplay(1));
      } else {
        this.cachedGrantResponse$ = this.http
          .get<MenuPermissionResponse>(this.resourceUrl, {
            params: new HttpParams().set('appName', SHELL_APP_NAME),
          })
          .pipe(
            map(response => this.normalizeResponse(response)),
            tap(response => this.writeToStorage(response)),
            shareReplay(1),
          );
      }
    }

    return this.cachedGrantResponse$;
  }

  allowedMenuIds(): Observable<string[]> {
    return this.query().pipe(map(response => response.allowedMenuIds));
  }

  visibleTree(): Observable<AppNavigationSection[]> {
    return this.allowedMenuIds().pipe(
      map(allowedMenuIds => {
        const allowed = new Set(allowedMenuIds);

        return APP_NAVIGATION_TREE.map(section => ({
          ...section,
          children: section.children.filter(child => allowed.has(child.id)),
        })).filter(section => section.children.length > 0);
      }),
    );
  }

  isNodeVisible(nodeId: string): Observable<boolean> {
    return this.allowedMenuIds().pipe(
      map(allowedMenuIds => {
        if (LEAF_IDS.has(nodeId)) {
          return allowedMenuIds.includes(nodeId);
        }

        const section = SECTIONS_BY_ID.get(nodeId);
        return section ? section.children.some(child => allowedMenuIds.includes(child.id)) : false;
      }),
    );
  }

  resolveFallbackRoute(sectionId?: string): Observable<string> {
    return this.allowedMenuIds().pipe(
      map(allowedMenuIds => {
        const preferredLeaf =
          (sectionId ? this.findFirstAllowedLeafInSection(sectionId, allowedMenuIds) : null) ??
          this.findFirstAllowedLeaf(allowedMenuIds);

        return preferredLeaf?.routePrefix ?? '/';
      }),
    );
  }

  getLeaf(nodeId: string): AppNavigationLeaf | null {
    return LEAVES_BY_ID.get(nodeId) ?? null;
  }

  private findFirstAllowedLeafInSection(sectionId: string, allowedMenuIds: string[]): AppNavigationLeaf | null {
    const section = SECTIONS_BY_ID.get(sectionId);
    if (!section) {
      return null;
    }

    return section.children.find(child => allowedMenuIds.includes(child.id)) ?? null;
  }

  private findFirstAllowedLeaf(allowedMenuIds: string[]): AppNavigationLeaf | null {
    return APP_NAVIGATION_LEAVES.find(leaf => allowedMenuIds.includes(leaf.id)) ?? null;
  }

  private normalizeResponse(response: MenuPermissionResponse): MenuPermissionResponse {
    const allowedMenuIds = new Set((response.allowedMenuIds ?? []).filter(menuId => LEAF_IDS.has(menuId)));

    return {
      appName: response.appName,
      allowedMenuIds: APP_NAVIGATION_LEAVES.map(leaf => leaf.id).filter(menuId => allowedMenuIds.has(menuId)),
    };
  }

  private readFromStorage(): MenuPermissionResponse | null {
    try {
      const raw = sessionStorage.getItem(NAVIGATION_STORAGE_KEY);
      if (!raw) {
        return null;
      }

      const parsed: unknown = JSON.parse(raw);
      if (!this.isMenuPermissionResponse(parsed)) {
        return null;
      }

      if (parsed.appName !== SHELL_APP_NAME) {
        return null;
      }

      const storedLogin = this.readStoredLogin();
      if (storedLogin !== null && storedLogin !== this.serializeLogin(this.currentLogin)) {
        return null;
      }

      return this.normalizeResponse(parsed);
    } catch {
      return null;
    }
  }

  private writeToStorage(response: MenuPermissionResponse): void {
    try {
      sessionStorage.setItem(NAVIGATION_STORAGE_KEY, JSON.stringify(response));
      sessionStorage.setItem(STORAGE_LOGIN_KEY, this.serializeLogin(this.currentLogin));
    } catch {
      // Storage unavailable or full. The in-memory cache still shields repeated requests.
    }
  }

  private clearCache(): void {
    this.cachedGrantResponse$ = undefined;
    try {
      sessionStorage.removeItem(NAVIGATION_STORAGE_KEY);
      sessionStorage.removeItem(STORAGE_LOGIN_KEY);
    } catch {
      // Storage unavailable. The next query still refreshes the in-memory cache.
    }
  }

  private readStoredLogin(): string | null {
    try {
      return sessionStorage.getItem(STORAGE_LOGIN_KEY);
    } catch {
      return null;
    }
  }

  private serializeLogin(login: string | null): string {
    return login ?? '';
  }

  private isMenuPermissionResponse(value: unknown): value is MenuPermissionResponse {
    const candidate = value as Partial<MenuPermissionResponse>;

    return (
      typeof value === 'object' &&
      value !== null &&
      typeof candidate.appName === 'string' &&
      Array.isArray(candidate.allowedMenuIds) &&
      candidate.allowedMenuIds.every(menuId => typeof menuId === 'string')
    );
  }
}
