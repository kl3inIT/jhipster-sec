import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IShoe, NewShoe } from '../shoe.model';

export type EntityResponseType = HttpResponse<IShoe>;
export type EntityArrayResponseType = HttpResponse<IShoe[]>;

@Injectable({ providedIn: 'root' })
export class ShoeService {
  protected readonly http = inject(HttpClient);
  protected readonly applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/shoes');

  create(shoe: NewShoe): Observable<EntityResponseType> {
    return this.http.post<IShoe>(this.resourceUrl, shoe, { observe: 'response' });
  }

  update(shoe: IShoe): Observable<EntityResponseType> {
    return this.http.put<IShoe>(`${this.resourceUrl}/${shoe.id}`, shoe, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IShoe>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IShoe[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getShoeIdentifier(shoe: Pick<IShoe, 'id'>): number {
    return shoe.id ?? 0;
  }
}
