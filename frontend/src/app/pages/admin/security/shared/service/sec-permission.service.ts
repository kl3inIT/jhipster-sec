import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { ISecPermission } from '../sec-permission.model';

@Injectable({ providedIn: 'root' })
export class SecPermissionService {
  private resourceUrl = inject(ApplicationConfigService).getEndpointFor('api/admin/sec/permissions');
  private http = inject(HttpClient);

  query(authorityName?: string): Observable<ISecPermission[]> {
    let url = this.resourceUrl;
    if (authorityName) {
      url += '?authorityName=' + encodeURIComponent(authorityName);
    }
    return this.http.get<ISecPermission[]>(url);
  }

  create(permission: ISecPermission): Observable<HttpResponse<ISecPermission>> {
    return this.http.post<ISecPermission>(this.resourceUrl, permission, { observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }
}
