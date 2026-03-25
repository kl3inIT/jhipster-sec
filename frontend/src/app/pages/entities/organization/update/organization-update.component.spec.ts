import { HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { BehaviorSubject, of } from 'rxjs';
import { vi } from 'vitest';

import { ISecuredEntityCapability } from '../../shared/secured-entity-capability.model';
import { WorkspaceContextService } from '../../shared/service/workspace-context.service';
import { OrganizationService } from '../service/organization.service';
import OrganizationUpdateComponent from './organization-update.component';

describe('OrganizationUpdateComponent', () => {
  let routeParamMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let routeSnapshot: {
    data: { capability: ISecuredEntityCapability | null; navigationNodeId?: string };
  };
  let organizationService: {
    find: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  let router: Router;
  let workspaceContextService: {
    get: ReturnType<typeof vi.fn>;
  };
  let navigateSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    routeParamMap$ = new BehaviorSubject(convertToParamMap({}));
    routeSnapshot = {
      data: {
        capability: buildCapability(false),
        navigationNodeId: 'entities.organization',
      },
    };
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
    workspaceContextService = {
      get: vi.fn<() => { queryParams: Record<string, string> } | null>(() => null),
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
        { provide: WorkspaceContextService, useValue: workspaceContextService },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
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

  it('redirects create routes to /accessdenied when create permission is denied', () => {
    routeSnapshot.data.capability = {
      ...buildCapability(true),
      canCreate: false,
    };
    const fixture = TestBed.createComponent(OrganizationUpdateComponent);

    fixture.detectChanges();

    expect(navigateSpy).toHaveBeenCalledWith(['/accessdenied']);
  });

  it('restores the saved workspace query params when canceling', () => {
    workspaceContextService.get.mockReturnValue({
      queryParams: {
        page: '4',
        sort: 'name,desc',
      },
    });
    const fixture = TestBed.createComponent(OrganizationUpdateComponent);

    fixture.detectChanges();
    navigateSpy.mockClear();

    fixture.componentInstance.cancel();

    expect(workspaceContextService.get).toHaveBeenCalledWith('entities.organization');
    expect(navigateSpy).toHaveBeenCalledWith(['/entities/organization'], {
      queryParams: {
        page: '4',
        sort: 'name,desc',
      },
    });
  });

  it('restores the saved workspace query params after a successful create', () => {
    workspaceContextService.get.mockReturnValue({
      queryParams: {
        page: '2',
        sort: 'name,desc',
      },
    });
    organizationService.create.mockReturnValue(of(new HttpResponse({ body: { id: 10 } })));
    const fixture = TestBed.createComponent(OrganizationUpdateComponent);

    fixture.detectChanges();
    navigateSpy.mockClear();
    fixture.componentInstance.form.patchValue({
      code: 'ORG-010',
      name: 'Platform',
      ownerLogin: 'admin',
    });

    fixture.componentInstance.save();

    expect(organizationService.create).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/entities/organization'], {
      queryParams: {
        page: '2',
        sort: 'name,desc',
      },
    });
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
