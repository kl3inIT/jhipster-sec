import { HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of, Subject } from 'rxjs';
import { vi } from 'vitest';

import { ISecuredEntityCapability } from '../../shared/secured-entity-capability.model';
import { SecuredEntityCapabilityService } from '../../shared/service/secured-entity-capability.service';
import { OrganizationService } from '../service/organization.service';
import OrganizationUpdateComponent from './organization-update.component';

describe('OrganizationUpdateComponent', () => {
  let routeParamMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let capability$: Subject<ISecuredEntityCapability | null>;
  let organizationService: {
    find: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  let capabilityService: {
    getEntityCapability: ReturnType<typeof vi.fn>;
  };
  let router: Router;

  beforeEach(async () => {
    routeParamMap$ = new BehaviorSubject(convertToParamMap({}));
    capability$ = new Subject<ISecuredEntityCapability | null>();
    organizationService = {
      find: vi.fn().mockReturnValue(
        of(
          new HttpResponse({
            body: {
              id: 1,
              code: 'ORG',
              name: 'Organization',
              ownerLogin: 'owner',
              budget: 1000,
            },
          }),
        ),
      ),
      create: vi.fn(),
      update: vi.fn(),
    };
    capabilityService = {
      getEntityCapability: vi.fn().mockReturnValue(capability$.asObservable()),
    };

    await TestBed.configureTestingModule({
      imports: [OrganizationUpdateComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: routeParamMap$.asObservable() } },
        { provide: OrganizationService, useValue: organizationService },
        { provide: SecuredEntityCapabilityService, useValue: capabilityService },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('should keep the budget input hidden until capability loads and omit it when budget edit is denied', () => {
    const fixture = TestBed.createComponent(OrganizationUpdateComponent);

    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#budget')).toBeNull();

    capability$.next(buildCapability(false));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#budget')).toBeNull();
  });

  it('should render the budget input when the capability allows editing the budget attribute', () => {
    const fixture = TestBed.createComponent(OrganizationUpdateComponent);

    fixture.detectChanges();
    capability$.next(buildCapability(true));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#budget')).not.toBeNull();
  });
});

function buildCapability(canEditBudget: boolean): ISecuredEntityCapability {
  return {
    code: 'organization',
    canCreate: true,
    canRead: true,
    canUpdate: true,
    canDelete: false,
    attributes: [
      {
        name: 'budget',
        canView: canEditBudget,
        canEdit: canEditBudget,
      },
    ],
  };
}
