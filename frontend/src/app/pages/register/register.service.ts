import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';

export interface RegisterRequest {
  login: string;
  email: string;
  password: string;
  firstName?: string | null;
  lastName?: string | null;
  imageUrl?: string | null;
  langKey?: string | null;
  authorities?: string[];
}

@Injectable({ providedIn: 'root' })
export class RegisterService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  private readonly resourceUrl = this.applicationConfigService.getEndpointFor('api/register');

  register(payload: RegisterRequest): Observable<void> {
    return this.http.post<void>(this.resourceUrl, payload);
  }
}
