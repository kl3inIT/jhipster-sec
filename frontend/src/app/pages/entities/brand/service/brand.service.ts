import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IBrand, NewBrand } from '../brand.model';

export type EntityResponseType = HttpResponse<IBrand>;
export type EntityArrayResponseType = HttpResponse<IBrand[]>;

@Injectable({ providedIn: 'root' })
export class BrandService {
  protected readonly http = inject(HttpClient);
  protected readonly applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/brands');

  create(brand: NewBrand): Observable<EntityResponseType> {
    return this.http.post<IBrand>(this.resourceUrl, brand, { observe: 'response' });
  }

  update(brand: IBrand): Observable<EntityResponseType> {
    return this.http.put<IBrand>(`${this.resourceUrl}/${brand.id}`, brand, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IBrand>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IBrand[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getBrandIdentifier(brand: Pick<IBrand, 'id'>): number {
    return brand.id ?? 0;
  }
}
