import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { vi, beforeAll, afterAll } from 'vitest';

import { SortService } from 'app/shared/sort/sort.service';
import { WorkspaceContextService } from '../../shared/service/workspace-context.service';
import { ISecuredEntityCapability } from '../../shared/secured-entity-capability.model';
import { IDepartment } from '../department.model';
import { DepartmentService } from '../service/department.service';
import DepartmentListComponent from './department-list.component';

describe('DepartmentListComponent', () => {
  beforeAll(() => {
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 1280 });
  });

  afterAll(() => {
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 0 });
  });
  const department: IDepartment = {
    id: 200,
    code: 'DEPT-OWNED',
    name: 'Owned Department',
    organization: {
      id: 100,
      name: 'Owned Org',
    },
    costCenter: 'FIN',
  };

  const restrictiveCapability: ISecuredEntityCapability = {
    code: 'department',
    canCreate: false,
    canRead: true,
    canUpdate: false,
    canDelete: false,
    attributes: [
      { name: 'organization', canView: false, canEdit: false },
      { name: 'costCenter', canView: false, canEdit: false },
    ],
  };

  let departmentService: {
    query: ReturnType<typeof vi.fn>;
    getDepartmentIdentifier: (item: Pick<IDepartment, 'id'>) => number;
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
    departmentService = {
      query: vi.fn(() =>
        of(
          new HttpResponse<IDepartment[]>({
            body: [department],
            headers: new HttpHeaders({ 'X-Total-Count': '1' }),
          }),
        ),
      ),
      getDepartmentIdentifier: (item: Pick<IDepartment, 'id'>) => item.id ?? 0,
      delete: vi.fn(() => of(new HttpResponse({ status: 204 }))),
    };
    router.navigate.mockClear();
    workspaceContextService.store.mockClear();
    routeSnapshot = {
      data: {
        capability: restrictiveCapability,
        navigationNodeId: 'entities.department',
      },
      queryParams: {},
    };

    TestBed.configureTestingModule({
      imports: [DepartmentListComponent],
      providers: [
        provideTranslateService({ lang: 'en', fallbackLang: 'en' }),
        { provide: DepartmentService, useValue: departmentService },
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
        list: {
          actions: 'Actions',
        },
        pagination: {
          report: 'Showing records',
        },
      },
      angappApp: {
        department: {
          id: 'ID',
          code: 'Code',
          name: 'Name',
          organization: 'Organization',
          costCenter: 'Cost Center',
          home: {
            title: 'Departments',
            createLabel: 'New Department',
            denied: {
              title: 'Department access is limited',
              message: 'You cannot view departments.',
            },
            notFound: 'No departments found',
          },
        },
      },
    });
  });

  it('hides permission-denied organization and cost center columns', async () => {
    const fixture = TestBed.createComponent(DepartmentListComponent);

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const nativeElement = fixture.nativeElement as HTMLElement;

    expect(nativeElement.textContent).not.toContain('Organization');
    expect(nativeElement.textContent).not.toContain('Cost Center');
    expect(nativeElement.textContent).not.toContain('Owned Org');
    expect(nativeElement.textContent).not.toContain('FIN');
  });

  it('shows organization and cost center columns when capability allows them', async () => {
    routeSnapshot.data.capability = {
      ...restrictiveCapability,
      attributes: [
        { name: 'organization', canView: true, canEdit: false },
        { name: 'costCenter', canView: true, canEdit: false },
      ],
    };
    const fixture = TestBed.createComponent(DepartmentListComponent);

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const nativeElement = fixture.nativeElement as HTMLElement;

    expect(nativeElement.textContent).toContain('Organization');
    expect(nativeElement.textContent).toContain('Cost Center');
    expect(nativeElement.textContent).toContain('Owned Org');
    expect(nativeElement.textContent).toContain('FIN');
  });
});
