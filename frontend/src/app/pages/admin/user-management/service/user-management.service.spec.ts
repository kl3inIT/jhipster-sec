import { HttpErrorResponse, HttpResponse, provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { Authority } from 'app/config/authority.constants';
import { User } from '../user-management.model';
import { UserManagementService } from './user-management.service';

describe('UserManagementService', () => {
  let service: UserManagementService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ApplicationConfigService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(UserManagementService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads a user by login', () => {
    let expectedLogin: string | undefined;

    service.find('user').subscribe(user => {
      expectedLogin = user.login;
    });

    const request = httpMock.expectOne({ method: 'GET', url: 'api/admin/users/user' });
    request.flush(new User(1, 'user'));

    expect(expectedLogin).toBe('user');
  });

  it('queries users with typed request params', () => {
    let response: HttpResponse<User[]> | undefined;

    service.query({ page: 1, size: 20, sort: ['login,asc'] }).subscribe(result => {
      response = result as HttpResponse<User[]>;
    });

    const request = httpMock.expectOne(req => req.method === 'GET' && req.url === 'api/admin/users');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.getAll('sort')).toEqual(['login,asc']);
    request.flush([new User(1, 'user')], { status: 200, statusText: 'OK' });

    expect(response?.body?.[0]?.login).toBe('user');
  });

  it('maps authority resources into string names', () => {
    let authorities: string[] = [];

    service.authorities().subscribe(result => {
      authorities = result;
    });

    const request = httpMock.expectOne({ method: 'GET', url: 'api/authorities' });
    request.flush([{ name: Authority.USER }, { name: Authority.ADMIN }]);

    expect(authorities).toEqual([Authority.USER, Authority.ADMIN]);
  });

  it('propagates backend errors', () => {
    let status = 0;

    service.find('missing').subscribe({
      error: (error: HttpErrorResponse) => {
        status = error.status;
      },
    });

    const request = httpMock.expectOne({ method: 'GET', url: 'api/admin/users/missing' });
    request.flush('Missing', { status: 404, statusText: 'Not Found' });

    expect(status).toBe(404);
  });
});
