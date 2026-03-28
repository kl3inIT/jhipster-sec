import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { ISecMenuPermissionAdmin } from '../sec-menu-permission-admin.model';

@Injectable({ providedIn: 'root' })
export class AdminMenuPermissionService {
  private resourceUrl = inject(ApplicationConfigService).getEndpointFor(
    'api/admin/sec/menu-permissions',
  );
  private http = inject(HttpClient);

  query(role: string, appName?: string): Observable<ISecMenuPermissionAdmin[]> {
    let params = new HttpParams().set('role', role);
    if (appName && appName.trim().length > 0) {
      params = params.append('appName', appName);
    }
    return this.http.get<ISecMenuPermissionAdmin[]>(this.resourceUrl, { params });
  }

  create(permission: ISecMenuPermissionAdmin): Observable<HttpResponse<ISecMenuPermissionAdmin>> {
    return this.http.post<ISecMenuPermissionAdmin>(this.resourceUrl, permission, {
      observe: 'response',
    });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }
}
