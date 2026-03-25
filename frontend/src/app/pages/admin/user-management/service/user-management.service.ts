import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { SearchWithPagination } from 'app/core/request/request.model';
import { createRequestOption } from 'app/core/request/request-util';
import { IUser } from '../user-management.model';

@Injectable({ providedIn: 'root' })
export class UserManagementService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  private readonly resourceUrl = this.applicationConfigService.getEndpointFor('api/admin/users');
  private readonly authorityUrl = this.applicationConfigService.getEndpointFor('api/authorities');

  create(user: IUser): Observable<IUser> {
    return this.http.post<IUser>(this.resourceUrl, user);
  }

  update(user: IUser): Observable<IUser> {
    return this.http.put<IUser>(this.resourceUrl, user);
  }

  find(login: string): Observable<IUser> {
    return this.http.get<IUser>(`${this.resourceUrl}/${login}`);
  }

  query(req?: SearchWithPagination): Observable<HttpResponse<IUser[]>> {
    return this.http.get<IUser[]>(this.resourceUrl, { params: createRequestOption(req), observe: 'response' });
  }

  delete(login: string): Observable<{}> {
    return this.http.delete(`${this.resourceUrl}/${login}`);
  }

  authorities(): Observable<string[]> {
    return this.http.get<Array<{ name: string }>>(this.authorityUrl).pipe(map(authorities => authorities.map(authority => authority.name)));
  }
}
