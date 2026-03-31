import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IMovieProfile, NewMovieProfile } from '../movie-profile.model';

export type EntityResponseType = HttpResponse<IMovieProfile>;
export type EntityArrayResponseType = HttpResponse<IMovieProfile[]>;

@Injectable({ providedIn: 'root' })
export class MovieProfileService {

  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  private readonly resourceUrl = this.applicationConfigService.getEndpointFor('api/movie-profiles');

  create(movieProfile: NewMovieProfile): Observable<EntityResponseType> {
    return this.http.post<IMovieProfile>(this.resourceUrl, movieProfile, { observe: 'response' });
  }

  update(movieProfile: IMovieProfile): Observable<EntityResponseType> {
    return this.http.put<IMovieProfile>(`${this.resourceUrl}/${movieProfile.id}`, movieProfile, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IMovieProfile>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IMovieProfile[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  count(req?: any): Observable<HttpResponse<number>> {
    const options = createRequestOption(req);
    return this.http.get<number>(`${this.resourceUrl}/count`, { params: options, observe: 'response' });
  }
}
