import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { ISecCatalogEntry } from '../sec-catalog.model';

@Injectable({ providedIn: 'root' })
export class SecCatalogService {
  private resourceUrl = inject(ApplicationConfigService).getEndpointFor('api/admin/sec/catalog');
  private http = inject(HttpClient);

  query(): Observable<ISecCatalogEntry[]> {
    return this.http.get<ISecCatalogEntry[]>(this.resourceUrl);
  }
}
