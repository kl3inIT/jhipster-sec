import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IDepartment, NewDepartment } from '../department.model';

export type EntityResponseType = HttpResponse<IDepartment>;
export type EntityArrayResponseType = HttpResponse<IDepartment[]>;

@Injectable({ providedIn: 'root' })
export class DepartmentService {
  protected readonly http = inject(HttpClient);
  protected readonly applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/departments');

  create(department: NewDepartment): Observable<EntityResponseType> {
    return this.http.post<IDepartment>(this.resourceUrl, department, { observe: 'response' });
  }

  update(department: IDepartment): Observable<EntityResponseType> {
    return this.http.put<IDepartment>(`${this.resourceUrl}/${department.id}`, department, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IDepartment>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IDepartment[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getDepartmentIdentifier(department: Pick<IDepartment, 'id'>): number {
    return department.id ?? 0;
  }
}
