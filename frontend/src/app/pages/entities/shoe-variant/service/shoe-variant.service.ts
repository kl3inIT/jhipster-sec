import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IShoeVariant, NewShoeVariant } from '../shoe-variant.model';

export type EntityResponseType = HttpResponse<IShoeVariant>;
export type EntityArrayResponseType = HttpResponse<IShoeVariant[]>;

@Injectable({ providedIn: 'root' })
export class ShoeVariantService {
  protected readonly http = inject(HttpClient);
  protected readonly applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/shoe-variants');

  create(shoeVariant: NewShoeVariant): Observable<EntityResponseType> {
    return this.http.post<IShoeVariant>(this.resourceUrl, shoeVariant, { observe: 'response' });
  }

  update(shoeVariant: IShoeVariant): Observable<EntityResponseType> {
    return this.http.put<IShoeVariant>(`${this.resourceUrl}/${shoeVariant.id}`, shoeVariant, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IShoeVariant>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IShoeVariant[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getShoeVariantIdentifier(shoeVariant: Pick<IShoeVariant, 'id'>): number {
    return shoeVariant.id ?? 0;
  }
}
