import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { IProductionEkip } from '../production-ekip.model';

@Injectable({ providedIn: 'root' })
export class ProductionEkipService {
  private readonly http = inject(HttpClient);
  private readonly appConfig = inject(ApplicationConfigService);

  private endpoint(movieProfileId: number): string {
    return this.appConfig.getEndpointFor(`api/movie-profiles/${movieProfileId}/production-ekips`);
  }

  query(movieProfileId: number): Observable<IProductionEkip[]> {
    return this.http.get<IProductionEkip[]>(this.endpoint(movieProfileId));
  }

  replace(movieProfileId: number, items: IProductionEkip[]): Observable<IProductionEkip[]> {
    return this.http.put<IProductionEkip[]>(this.endpoint(movieProfileId), items);
  }
}

