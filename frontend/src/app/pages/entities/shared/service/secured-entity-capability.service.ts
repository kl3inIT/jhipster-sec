import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { distinctUntilChanged, map, shareReplay } from 'rxjs/operators';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { AccountService } from 'app/core/auth/account.service';
import { ISecuredEntityCapability } from '../secured-entity-capability.model';

@Injectable({ providedIn: 'root' })
export class SecuredEntityCapabilityService {
  private readonly resourceUrl = inject(ApplicationConfigService).getEndpointFor('api/security/entity-capabilities');
  private readonly http = inject(HttpClient);
  private readonly accountService = inject(AccountService);

  private cachedCapabilities$?: Observable<ISecuredEntityCapability[]>;

  constructor() {
    this.accountService
      .getAuthenticationState()
      .pipe(distinctUntilChanged((a, b) => a?.login === b?.login))
      .subscribe(() => {
        this.cachedCapabilities$ = undefined;
      });
  }

  query(): Observable<ISecuredEntityCapability[]> {
    if (!this.cachedCapabilities$) {
      this.cachedCapabilities$ = this.http.get<ISecuredEntityCapability[]>(this.resourceUrl).pipe(shareReplay(1));
    }
    return this.cachedCapabilities$;
  }

  getEntityCapability(code: string): Observable<ISecuredEntityCapability | null> {
    return this.query().pipe(map(capabilities => capabilities.find(capability => capability.code === code) ?? null));
  }
}
