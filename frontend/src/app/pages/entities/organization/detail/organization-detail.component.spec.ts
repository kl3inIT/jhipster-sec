import { HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { BehaviorSubject, of } from 'rxjs';
import { vi } from 'vitest';

import { DepartmentService } from '../../department/service/department.service';
import { EmployeeService } from '../../employee/service/employee.service';
import { ISecuredEntityCapability } from '../../shared/secured-entity-capability.model';
import { SecuredEntityCapabilityService } from '../../shared/service/secured-entity-capability.service';
import { WorkspaceContextService } from '../../shared/service/workspace-context.service';
import { OrganizationService } from '../service/organization.service';
import OrganizationDetailComponent from './organization-detail.component';
import { IOrganizationWorkbench } from './organization-workbench.model';

describe('OrganizationDetailComponent', () => {
  let routeParamMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let routeSnapshot: {
    data: { capability: ISecuredEntityCapability | null; navigationNodeId?: string };
  };
  let organizationService: {
    findWorkbench: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  let departmentService: {
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };
  let employeeService: {
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };
  let capabilityService: {
    query: ReturnType<typeof vi.fn>;
  };
  let childCapabilities: ISecuredEntityCapability[];
  let router: Router;

  beforeEach(async () => {
    routeParamMap$ = new BehaviorSubject(convertToParamMap({ id: '1' }));
    routeSnapshot = {
      data: {
        capability: buildCapability('organization', {
          canRead: true,
          canUpdate: true,
          attributes: [
            { name: 'code', canView: true, canEdit: true },
            { name: 'name', canView: true, canEdit: true },
            { name: 'ownerLogin', canView: true, canEdit: true },
            { name: 'budget', canView: true, canEdit: false },
          ],
        }),
        navigationNodeId: 'entities.organization',
      },
    };

    organizationService = {
      findWorkbench: vi.fn().mockReturnValue(of(new HttpResponse({ body: buildWorkbench() }))),
      update: vi.fn().mockReturnValue(of(new HttpResponse({ body: buildWorkbench() }))),
    };
    departmentService = {
      create: vi
        .fn()
        .mockReturnValue(of(new HttpResponse({ body: { id: 401, name: 'Workbench Department' } }))),
      update: vi.fn().mockReturnValue(of(new HttpResponse({ body: { id: 200 } }))),
      delete: vi.fn().mockReturnValue(of(new HttpResponse({}))),
    };
    employeeService = {
      create: vi.fn().mockReturnValue(of(new HttpResponse({ body: { id: 402 } }))),
      update: vi.fn().mockReturnValue(of(new HttpResponse({ body: { id: 300 } }))),
      delete: vi.fn().mockReturnValue(of(new HttpResponse({}))),
    };
    childCapabilities = [
      buildCapability('department', {
        canCreate: true,
        canRead: true,
        canUpdate: true,
        canDelete: true,
      }),
      buildCapability('employee', {
        canCreate: true,
        canRead: true,
        canUpdate: true,
        canDelete: true,
        attributes: [{ name: 'salary', canView: true, canEdit: true }],
      }),
    ];
    capabilityService = {
      query: vi.fn().mockImplementation(() => of(childCapabilities)),
    };

    await TestBed.configureTestingModule({
      imports: [OrganizationDetailComponent],
      providers: [
        provideTranslateService({ lang: 'en', fallbackLang: 'en' }),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: routeSnapshot,
            paramMap: routeParamMap$.asObservable(),
          },
        },
        { provide: OrganizationService, useValue: organizationService },
        { provide: DepartmentService, useValue: departmentService },
        { provide: EmployeeService, useValue: employeeService },
        { provide: SecuredEntityCapabilityService, useValue: capabilityService },
        {
          provide: WorkspaceContextService,
          useValue: { get: vi.fn(() => null) },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('loads the organization workbench and renders nested departments and employees', async () => {
    const fixture = TestBed.createComponent(OrganizationDetailComponent);

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(organizationService.findWorkbench).toHaveBeenCalledWith(1);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Owned Department');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Alice');
  });

  it('hides child create, edit, and delete affordances when child capabilities are denied', async () => {
    childCapabilities = [
      buildCapability('department', {
        canCreate: false,
        canRead: true,
        canUpdate: false,
        canDelete: false,
      }),
      buildCapability('employee', {
        canCreate: false,
        canRead: true,
        canUpdate: false,
        canDelete: false,
        attributes: [{ name: 'salary', canView: false, canEdit: false }],
      }),
    ];

    const fixture = TestBed.createComponent(OrganizationDetailComponent);

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const nativeElement = fixture.nativeElement as HTMLElement;
    expect(findByTestId(nativeElement, 'department-create-button')).toBeNull();
    expect(findByTestId(nativeElement, 'employee-create-button')).toBeNull();
    expect(findByTestId(nativeElement, 'selected-department-edit-button')).toBeNull();
    expect(findByTestId(nativeElement, 'employee-edit-button-300')).toBeNull();
    expect(findByTestId(nativeElement, 'employee-delete-button-300')).toBeNull();
  });

  it('reloads the root workbench after creating a department from the same screen', async () => {
    const fixture = TestBed.createComponent(OrganizationDetailComponent);

    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openCreateDepartmentDialog();
    fixture.componentInstance.departmentForm.patchValue({
      code: 'DEPT-WB',
      name: 'Workbench Department',
      costCenter: 'OPS',
    });

    fixture.componentInstance.saveDepartment();

    expect(departmentService.create).toHaveBeenCalledWith({
      id: null,
      code: 'DEPT-WB',
      name: 'Workbench Department',
      organization: { id: 1, name: 'Owned Org' },
      costCenter: 'OPS',
    });
    expect(organizationService.findWorkbench).toHaveBeenCalledTimes(2);
    expect(router.navigate).not.toHaveBeenCalledWith(['/entities/department'], expect.anything());
  });

  it('reloads the root workbench after updating an employee from the same screen', async () => {
    const fixture = TestBed.createComponent(OrganizationDetailComponent);

    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.openEditEmployeeDialog(
      buildWorkbench().departments![0].employees![0],
    );
    fixture.componentInstance.employeeForm.patchValue({
      id: 300,
      employeeNumber: 'EMP-001',
      firstName: 'Alice',
      lastName: 'Owner',
      departmentId: 200,
      email: 'alice.updated@example.com',
      salary: 9000,
    });

    fixture.componentInstance.saveEmployee();

    expect(employeeService.update).toHaveBeenCalledWith({
      id: 300,
      employeeNumber: 'EMP-001',
      firstName: 'Alice',
      lastName: 'Owner',
      department: { id: 200, name: 'Owned Department' },
      email: 'alice.updated@example.com',
      salary: 9000,
    });
    expect(organizationService.findWorkbench).toHaveBeenCalledTimes(2);
  });

  it('reloads the root workbench after deleting an employee from the same screen', async () => {
    const fixture = TestBed.createComponent(OrganizationDetailComponent);

    fixture.detectChanges();
    await fixture.whenStable();

    fixture.componentInstance.deleteEmployee(buildWorkbench().departments![0].employees![0]);

    expect(employeeService.delete).toHaveBeenCalledWith(300);
    expect(organizationService.findWorkbench).toHaveBeenCalledTimes(2);
  });
});

function buildWorkbench(): IOrganizationWorkbench {
  return {
    id: 1,
    code: 'ORG-001',
    name: 'Owned Org',
    ownerLogin: 'proof-owner',
    budget: 100000,
    departments: [
      {
        id: 200,
        code: 'DEPT-OWNED',
        name: 'Owned Department',
        costCenter: 'FIN',
        employees: [
          {
            id: 300,
            employeeNumber: 'EMP-001',
            firstName: 'Alice',
            lastName: 'Owner',
            email: 'alice@example.com',
            salary: 9000,
            department: {
              id: 200,
              code: 'DEPT-OWNED',
              name: 'Owned Department',
            },
          },
        ],
      },
    ],
  };
}

function buildCapability(
  code: string,
  options: Partial<ISecuredEntityCapability> & {
    attributes?: ISecuredEntityCapability['attributes'];
  } = {},
): ISecuredEntityCapability {
  return {
    code,
    canCreate: options.canCreate ?? false,
    canRead: options.canRead ?? false,
    canUpdate: options.canUpdate ?? false,
    canDelete: options.canDelete ?? false,
    attributes: options.attributes ?? [],
  };
}

function findByTestId(nativeElement: HTMLElement, testId: string): HTMLElement | null {
  return nativeElement.querySelector(`[data-testid="${testId}"]`);
}
