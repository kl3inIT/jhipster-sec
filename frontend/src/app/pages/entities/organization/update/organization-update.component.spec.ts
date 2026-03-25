import { HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { BehaviorSubject, of } from 'rxjs';
import { vi } from 'vitest';

import { ISecuredEntityCapability } from '../../shared/secured-entity-capability.model';
import { OrganizationService } from '../service/organization.service';
import OrganizationUpdateComponent from './organization-update.component';

describe('OrganizationUpdateComponent', () => {
  let routeParamMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let routeSnapshot: { data: { capability: ISecuredEntityCapability | null } };
  let organizationService: {
    find: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  let router: Router;

  beforeEach(async () => {
    routeParamMap$ = new BehaviorSubject(convertToParamMap({}));
    routeSnapshot = { data: { capability: buildCapability(false) } };
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

    await TestBed.configureTestingModule({
      imports: [OrganizationUpdateComponent],
      providers: [
        provideTranslateService({ lang: 'en', fallbackLang: 'en' }),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: routeSnapshot, paramMap: routeParamMap$.asObservable() },
        },
        { provide: OrganizationService, useValue: organizationService },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('should keep the budget input hidden when budget edit is denied', () => {
    const fixture = TestBed.createComponent(OrganizationUpdateComponent);

    fixture.detectChanges();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#budget')).toBeNull();
  });

  it('should render the budget input when the capability allows editing the budget attribute', () => {
    routeSnapshot.data.capability = buildCapability(true);
    const fixture = TestBed.createComponent(OrganizationUpdateComponent);

    fixture.detectChanges();
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
