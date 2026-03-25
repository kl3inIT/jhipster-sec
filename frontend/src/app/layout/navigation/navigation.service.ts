import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { distinctUntilChanged, map, shareReplay, tap } from 'rxjs/operators';

import { AccountService } from 'app/core/auth/account.service';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { NAVIGATION_STORAGE_KEY, SHELL_APP_NAME } from './navigation.constants';
import { APP_NAVIGATION_LEAVES, APP_NAVIGATION_TREE } from './navigation-registry';
import { AppNavigationLeaf, AppNavigationSection } from './navigation.model';

interface NavigationGrantResponse {
  appName: string;
  allowedNodeIds: string[];
}

const STORAGE_LOGIN_KEY = `${NAVIGATION_STORAGE_KEY}:login`;
const LEAF_IDS = new Set(APP_NAVIGATION_LEAVES.map(leaf => leaf.id));
const LEAVES_BY_ID = new Map(APP_NAVIGATION_LEAVES.map(leaf => [leaf.id, leaf] as const));
const SECTIONS_BY_ID = new Map(APP_NAVIGATION_TREE.map(section => [section.id, section] as const));

@Injectable({ providedIn: 'root' })
export class NavigationService {
  private readonly resourceUrl = inject(ApplicationConfigService).getEndpointFor('api/security/navigation-grants');
  private readonly http = inject(HttpClient);
  private readonly accountService = inject(AccountService);

  private cachedGrantResponse$?: Observable<NavigationGrantResponse>;
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

  query(): Observable<NavigationGrantResponse> {
    if (!this.cachedGrantResponse$) {
      const stored = this.readFromStorage();
      if (stored) {
        this.cachedGrantResponse$ = of(stored).pipe(shareReplay(1));
      } else {
        this.cachedGrantResponse$ = this.http
          .get<NavigationGrantResponse>(this.resourceUrl, {
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

  allowedNodeIds(): Observable<string[]> {
    return this.query().pipe(map(response => response.allowedNodeIds));
  }

  visibleTree(): Observable<AppNavigationSection[]> {
    return this.allowedNodeIds().pipe(
      map(allowedNodeIds => {
        const allowed = new Set(allowedNodeIds);

        return APP_NAVIGATION_TREE.map(section => ({
          ...section,
          children: section.children.filter(child => allowed.has(child.id)),
        })).filter(section => section.children.length > 0);
      }),
    );
  }

  isNodeVisible(nodeId: string): Observable<boolean> {
    return this.allowedNodeIds().pipe(
      map(allowedNodeIds => {
        if (LEAF_IDS.has(nodeId)) {
          return allowedNodeIds.includes(nodeId);
        }

        const section = SECTIONS_BY_ID.get(nodeId);
        return section ? section.children.some(child => allowedNodeIds.includes(child.id)) : false;
      }),
    );
  }

  resolveFallbackRoute(sectionId?: string): Observable<string> {
    return this.allowedNodeIds().pipe(
      map(allowedNodeIds => {
        const preferredLeaf =
          (sectionId ? this.findFirstAllowedLeafInSection(sectionId, allowedNodeIds) : null) ??
          this.findFirstAllowedLeaf(allowedNodeIds);

        return preferredLeaf?.routePrefix ?? '/';
      }),
    );
  }

  getLeaf(nodeId: string): AppNavigationLeaf | null {
    return LEAVES_BY_ID.get(nodeId) ?? null;
  }

  private findFirstAllowedLeafInSection(sectionId: string, allowedNodeIds: string[]): AppNavigationLeaf | null {
    const section = SECTIONS_BY_ID.get(sectionId);
    if (!section) {
      return null;
    }

    return section.children.find(child => allowedNodeIds.includes(child.id)) ?? null;
  }

  private findFirstAllowedLeaf(allowedNodeIds: string[]): AppNavigationLeaf | null {
    return APP_NAVIGATION_LEAVES.find(leaf => allowedNodeIds.includes(leaf.id)) ?? null;
  }

  private normalizeResponse(response: NavigationGrantResponse): NavigationGrantResponse {
    const allowedNodeIds = new Set((response.allowedNodeIds ?? []).filter(nodeId => LEAF_IDS.has(nodeId)));

    return {
      appName: response.appName,
      allowedNodeIds: APP_NAVIGATION_LEAVES.map(leaf => leaf.id).filter(nodeId => allowedNodeIds.has(nodeId)),
    };
  }

  private readFromStorage(): NavigationGrantResponse | null {
    try {
      const raw = sessionStorage.getItem(NAVIGATION_STORAGE_KEY);
      if (!raw) {
        return null;
      }

      const parsed: unknown = JSON.parse(raw);
      if (!this.isGrantResponse(parsed)) {
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

  private writeToStorage(response: NavigationGrantResponse): void {
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

  private isGrantResponse(value: unknown): value is NavigationGrantResponse {
    const candidate = value as Partial<NavigationGrantResponse>;

    return (
      typeof value === 'object' &&
      value !== null &&
      typeof candidate.appName === 'string' &&
      Array.isArray(candidate.allowedNodeIds) &&
      candidate.allowedNodeIds.every(nodeId => typeof nodeId === 'string')
    );
  }
}
