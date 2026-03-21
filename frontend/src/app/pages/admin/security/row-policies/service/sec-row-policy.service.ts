import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { ISecRowPolicy } from '../sec-row-policy.model';

@Injectable({ providedIn: 'root' })
export class SecRowPolicyService {
  private resourceUrl = inject(ApplicationConfigService).getEndpointFor('api/admin/sec/row-policies');
  private http = inject(HttpClient);

  query(): Observable<HttpResponse<ISecRowPolicy[]>> {
    return this.http.get<ISecRowPolicy[]>(this.resourceUrl, { observe: 'response' });
  }

  find(id: number): Observable<ISecRowPolicy> {
    return this.http.get<ISecRowPolicy>(`${this.resourceUrl}/${id}`);
  }

  create(policy: ISecRowPolicy): Observable<HttpResponse<ISecRowPolicy>> {
    return this.http.post<ISecRowPolicy>(this.resourceUrl, policy, { observe: 'response' });
  }

  update(policy: ISecRowPolicy): Observable<HttpResponse<ISecRowPolicy>> {
    return this.http.put<ISecRowPolicy>(`${this.resourceUrl}/${policy.id}`, policy, { observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }
}
