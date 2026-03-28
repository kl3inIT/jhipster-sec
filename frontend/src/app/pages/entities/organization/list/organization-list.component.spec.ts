import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { vi, beforeAll, afterAll } from 'vitest';

import { SortService } from 'app/shared/sort/sort.service';
import { IOrganization } from '../organization.model';
import { OrganizationService } from '../service/organization.service';
import OrganizationListComponent from './organization-list.component';
import { ISecuredEntityCapability } from '../../shared/secured-entity-capability.model';
import { WorkspaceContextService } from '../../shared/service/workspace-context.service';

describe('OrganizationListComponent', () => {
  beforeAll(() => {
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 1280 });
  });

  afterAll(() => {
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 0 });
  });
  const organization: IOrganization = {
    id: 1,
    code: 'ORG-001',
    name: 'Platform',
    ownerLogin: 'admin',
  };

  const restrictiveCapability: ISecuredEntityCapability = {
    code: 'organization',
    canCreate: false,
    canRead: true,
    canUpdate: false,
    canDelete: false,
    attributes: [],
  };

  let organizationService: {
    query: ReturnType<typeof vi.fn>;
    getOrganizationIdentifier: (item: Pick<IOrganization, 'id'>) => number;
    delete: ReturnType<typeof vi.fn>;
  };

  const sortService = {
    parseSortParam: () => ({ predicate: 'id', order: 'asc' as const }),
    buildSortParam: () => ['id,asc'],
  };

  const router = {
    navigate: vi.fn(async () => true),
  };

  const workspaceContextService = {
    store: vi.fn(),
  };

  let routeSnapshot: {
    data: { capability: ISecuredEntityCapability | null; navigationNodeId?: string };
    queryParams: Record<string, string>;
  };

  beforeEach(() => {
    organizationService = {
      query: vi.fn(() =>
        of(
          new HttpResponse<IOrganization[]>({
            body: [organization],
            headers: new HttpHeaders({ 'X-Total-Count': '1' }),
          }),
        ),
      ),
      getOrganizationIdentifier: (item: Pick<IOrganization, 'id'>) => item.id ?? 0,
      delete: vi.fn(() => of(new HttpResponse({ status: 204 }))),
    };
    router.navigate.mockClear();
    workspaceContextService.store.mockClear();
    routeSnapshot = {
      data: {
        capability: restrictiveCapability,
        navigationNodeId: 'entities.organization',
      },
      queryParams: {},
    };

    TestBed.configureTestingModule({
      imports: [OrganizationListComponent],
      providers: [
        provideTranslateService({ lang: 'en', fallbackLang: 'en' }),
        { provide: OrganizationService, useValue: organizationService },
        { provide: SortService, useValue: sortService },
        { provide: Router, useValue: router },
        { provide: WorkspaceContextService, useValue: workspaceContextService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: routeSnapshot,
            queryParamMap: of(convertToParamMap({})),
            data: of({ defaultSort: 'id,asc' }),
          },
        },
      ],
    });

    TestBed.inject(TranslateService).setTranslation('en', {
      entity: {
        action: {
          view: 'View',
          edit: 'Edit',
          delete: 'Delete',
        },
      },
      angappApp: {
        organization: {
          home: {
            createLabel: 'New Organization',
            denied: {
              title: 'Organization access is limited',
              message:
                'You can open this workspace from the shell, but you do not have permission to view organization records.',
            },
          },
        },
      },
    });
  });

  it('shows only the View action when capability denies create, edit, and delete', async () => {
    const fixture = TestBed.createComponent(OrganizationListComponent);

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const nativeElement = fixture.nativeElement as HTMLElement;
    const actionLabels = Array.from(nativeElement.querySelectorAll('button[aria-label]')).map(
      (button) => button.getAttribute('aria-label'),
    );

    expect(nativeElement.textContent).not.toContain('New Organization');
    expect(actionLabels).toContain('View');
    expect(actionLabels).not.toContain('Edit');
    expect(actionLabels).not.toContain('Delete');
  });

  it('renders no action buttons while capability is still null', async () => {
    routeSnapshot.data.capability = null;
    const fixture = TestBed.createComponent(OrganizationListComponent);

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const nativeElement = fixture.nativeElement as HTMLElement;
    expect(nativeElement.textContent).not.toContain('New Organization');
    expect(nativeElement.querySelector('button[aria-label="View"]')).toBeNull();
    expect(nativeElement.querySelector('button[aria-label="Edit"]')).toBeNull();
    expect(nativeElement.querySelector('button[aria-label="Delete"]')).toBeNull();
  });

  it('shows an in-shell denied state and skips the list query when read access is denied', async () => {
    routeSnapshot.data.capability = {
      ...restrictiveCapability,
      canRead: false,
    };
    const fixture = TestBed.createComponent(OrganizationListComponent);

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(organizationService.query).not.toHaveBeenCalled();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Organization access is limited',
    );
  });

  it('stores the current list context before navigating to a detail route', async () => {
    routeSnapshot.queryParams = {
      page: '2',
      sort: 'name,desc',
      owner: 'admin',
    };
    const fixture = TestBed.createComponent(OrganizationListComponent);

    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.view(organization);

    expect(workspaceContextService.store).toHaveBeenCalledWith(
      'entities.organization',
      routeSnapshot.queryParams,
    );
    expect(router.navigate).toHaveBeenCalledWith(['/entities/organization', 1, 'view']);
  });
});
