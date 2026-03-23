import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { distinctUntilChanged, map, shareReplay, tap } from 'rxjs/operators';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { AccountService } from 'app/core/auth/account.service';
import { ISecuredEntityCapability } from '../secured-entity-capability.model';

@Injectable({ providedIn: 'root' })
export class SecuredEntityCapabilityService {
  private readonly resourceUrl = inject(ApplicationConfigService).getEndpointFor('api/security/entity-capabilities');
  private readonly http = inject(HttpClient);
  private readonly accountService = inject(AccountService);

  private readonly STORAGE_KEY = 'sec-entity-capabilities';

  private cachedCapabilities$?: Observable<ISecuredEntityCapability[]>;

  constructor() {
    this.accountService
      .getAuthenticationState()
      .pipe(distinctUntilChanged((a, b) => a?.login === b?.login))
      .subscribe(() => {
        this.cachedCapabilities$ = undefined;
        try {
          sessionStorage.removeItem(this.STORAGE_KEY);
        } catch (_) {
          /* SSR safety */
        }
      });
  }

  query(): Observable<ISecuredEntityCapability[]> {
    if (!this.cachedCapabilities$) {
      const stored = this.readFromStorage();
      if (stored) {
        this.cachedCapabilities$ = of(stored).pipe(shareReplay(1));
      } else {
        this.cachedCapabilities$ = this.http.get<ISecuredEntityCapability[]>(this.resourceUrl).pipe(
          tap(data => this.writeToStorage(data)),
          shareReplay(1),
        );
      }
    }
    return this.cachedCapabilities$;
  }

  getEntityCapability(code: string): Observable<ISecuredEntityCapability | null> {
    return this.query().pipe(map(capabilities => capabilities.find(capability => capability.code === code) ?? null));
  }

  private readFromStorage(): ISecuredEntityCapability[] | null {
    try {
      const raw = sessionStorage.getItem(this.STORAGE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }

  private writeToStorage(data: ISecuredEntityCapability[]): void {
    try {
      sessionStorage.setItem(this.STORAGE_KEY, JSON.stringify(data));
    } catch {
      // Storage full or unavailable -- degrade gracefully
    }
  }
}
