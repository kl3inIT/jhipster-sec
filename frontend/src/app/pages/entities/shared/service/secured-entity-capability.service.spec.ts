import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';

import { AccountService } from 'app/core/auth/account.service';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { SecuredEntityCapabilityService } from './secured-entity-capability.service';
import { ISecuredEntityCapability } from '../secured-entity-capability.model';

describe('SecuredEntityCapabilityService', () => {
  let service: SecuredEntityCapabilityService;
  let httpMock: HttpTestingController;

  const capabilities: ISecuredEntityCapability[] = [
    {
      code: 'organization',
      canCreate: true,
      canRead: true,
      canUpdate: false,
      canDelete: false,
      attributes: [{ name: 'budget', canView: false, canEdit: false }],
    },
    {
      code: 'department',
      canCreate: false,
      canRead: true,
      canUpdate: false,
      canDelete: false,
      attributes: [],
    },
  ];

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ApplicationConfigService, useValue: { getEndpointFor: (path: string) => path } },
        { provide: AccountService, useValue: { getAuthenticationState: () => of(null) } },
      ],
    });
    service = TestBed.inject(SecuredEntityCapabilityService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it("should return getEntityCapability('organization') from the cached capability list", () => {
    let organizationCapability: ISecuredEntityCapability | null | undefined;

    service.getEntityCapability('organization').subscribe(capability => {
      organizationCapability = capability;
    });

    const request = httpMock.expectOne('api/security/entity-capabilities');
    expect(request.request.method).toBe('GET');
    request.flush(capabilities);

    expect(organizationCapability?.code).toBe('organization');
    expect(organizationCapability?.attributes[0]?.name).toBe('budget');
  });

  it('should reuse one cached HTTP response across repeated subscriptions', () => {
    let firstResponse: ISecuredEntityCapability[] | undefined;
    let secondResponse: ISecuredEntityCapability[] | undefined;

    service.query().subscribe(response => {
      firstResponse = response;
    });

    const request = httpMock.expectOne('api/security/entity-capabilities');
    expect(request.request.method).toBe('GET');
    request.flush(capabilities);

    service.query().subscribe(response => {
      secondResponse = response;
    });

    httpMock.expectNone('api/security/entity-capabilities');
    expect(firstResponse).toEqual(capabilities);
    expect(secondResponse).toEqual(capabilities);
  });
});
