import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { ISecMenuDefinition, ISyncNode, ISyncResult } from '../sec-menu-definition.model';

@Injectable({ providedIn: 'root' })
export class SecMenuDefinitionService {
  private resourceUrl = inject(ApplicationConfigService).getEndpointFor('api/admin/sec/menu-definitions');
  private http = inject(HttpClient);

  query(appName = 'jhipster-security-platform'): Observable<HttpResponse<ISecMenuDefinition[]>> {
    const params = new HttpParams().set('appName', appName);
    return this.http.get<ISecMenuDefinition[]>(this.resourceUrl, { observe: 'response', params });
  }

  queryAll(): Observable<HttpResponse<ISecMenuDefinition[]>> {
    return this.http.get<ISecMenuDefinition[]>(this.resourceUrl, { observe: 'response' });
  }

  find(id: number): Observable<HttpResponse<ISecMenuDefinition>> {
    return this.http.get<ISecMenuDefinition>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  create(def: ISecMenuDefinition): Observable<HttpResponse<ISecMenuDefinition>> {
    return this.http.post<ISecMenuDefinition>(this.resourceUrl, def, { observe: 'response' });
  }

  update(id: number, def: ISecMenuDefinition): Observable<HttpResponse<ISecMenuDefinition>> {
    return this.http.put<ISecMenuDefinition>(`${this.resourceUrl}/${id}`, def, { observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  sync(nodes: ISyncNode[]): Observable<HttpResponse<ISyncResult>> {
    return this.http.post<ISyncResult>(`${this.resourceUrl}/sync`, nodes, { observe: 'response' });
  }
}
