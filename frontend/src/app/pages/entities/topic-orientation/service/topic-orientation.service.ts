import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { ITopicOrientation, NewTopicOrientation } from '../topic-orientation.model';

export type EntityResponseType = HttpResponse<ITopicOrientation>;
export type EntityArrayResponseType = HttpResponse<ITopicOrientation[]>;

@Injectable({ providedIn: 'root' })
export class TopicOrientationService {
  protected readonly http = inject(HttpClient);
  protected readonly applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/topic-orientations');

  create(topicOrientation: NewTopicOrientation): Observable<EntityResponseType> {
    return this.http.post<ITopicOrientation>(this.resourceUrl, topicOrientation, { observe: 'response' });
  }

  update(topicOrientation: ITopicOrientation): Observable<EntityResponseType> {
    return this.http.put<ITopicOrientation>(`${this.resourceUrl}/${topicOrientation.id}`, topicOrientation, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<ITopicOrientation>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<ITopicOrientation[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getTopicOrientationIdentifier(topicOrientation: Pick<ITopicOrientation, 'id'>): number {
    return topicOrientation.id ?? 0;
  }
}
