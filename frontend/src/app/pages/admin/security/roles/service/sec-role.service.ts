import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { ISecRole } from '../sec-role.model';

@Injectable({ providedIn: 'root' })
export class SecRoleService {
  private resourceUrl = inject(ApplicationConfigService).getEndpointFor('api/admin/sec/roles');
  private http = inject(HttpClient);

  query(): Observable<HttpResponse<ISecRole[]>> {
    return this.http.get<ISecRole[]>(this.resourceUrl, { observe: 'response' });
  }

  find(name: string): Observable<ISecRole> {
    return this.http.get<ISecRole>(`${this.resourceUrl}/${encodeURIComponent(name)}`);
  }

  create(role: ISecRole): Observable<HttpResponse<ISecRole>> {
    return this.http.post<ISecRole>(this.resourceUrl, role, { observe: 'response' });
  }

  update(role: ISecRole): Observable<HttpResponse<ISecRole>> {
    return this.http.put<ISecRole>(`${this.resourceUrl}/${encodeURIComponent(role.name)}`, role, { observe: 'response' });
  }

  delete(name: string): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${encodeURIComponent(name)}`, { observe: 'response' });
  }
}
