import { HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of, Subject } from 'rxjs';
import { vi } from 'vitest';

import { ISecuredEntityCapability } from '../../shared/secured-entity-capability.model';
import { SecuredEntityCapabilityService } from '../../shared/service/secured-entity-capability.service';
import { DepartmentService } from '../../department/service/department.service';
import { EmployeeService } from '../service/employee.service';
import EmployeeUpdateComponent from './employee-update.component';

describe('EmployeeUpdateComponent', () => {
  let routeParamMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let capability$: Subject<ISecuredEntityCapability | null>;
  let departmentService: {
    query: ReturnType<typeof vi.fn>;
  };
  let employeeService: {
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
    departmentService = {
      query: vi.fn().mockReturnValue(of(new HttpResponse({ body: [] }))),
    };
    employeeService = {
      find: vi.fn().mockReturnValue(
        of(
          new HttpResponse({
            body: {
              id: 42,
              employeeNumber: 'EMP-42',
              firstName: 'Ada',
              lastName: 'Lovelace',
              salary: 1234,
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
      imports: [EmployeeUpdateComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: routeParamMap$.asObservable() } },
        { provide: DepartmentService, useValue: departmentService },
        { provide: EmployeeService, useValue: employeeService },
        { provide: SecuredEntityCapabilityService, useValue: capabilityService },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('should keep the salary input hidden until capability loads and omit it when salary edit is denied', () => {
    const fixture = TestBed.createComponent(EmployeeUpdateComponent);

    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#salary')).toBeNull();

    capability$.next(buildCapability({ canCreate: true, canUpdate: true, canEditSalary: false }));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#salary')).toBeNull();
  });

  it('should render the salary input when the capability allows editing the salary attribute', () => {
    const fixture = TestBed.createComponent(EmployeeUpdateComponent);

    fixture.detectChanges();
    capability$.next(buildCapability({ canCreate: true, canUpdate: true, canEditSalary: true }));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#salary')).not.toBeNull();
  });

  it('should navigate to /accessdenied when edit capability is denied for an existing employee route', () => {
    routeParamMap$.next(convertToParamMap({ id: '42' }));
    const fixture = TestBed.createComponent(EmployeeUpdateComponent);

    fixture.detectChanges();
    capability$.next(buildCapability({ canCreate: true, canUpdate: false, canEditSalary: false }));
    fixture.detectChanges();

    expect(router.navigate).toHaveBeenCalledWith(['/accessdenied']);
    expect(employeeService.find).not.toHaveBeenCalled();
  });
});

function buildCapability(options: { canCreate: boolean; canUpdate: boolean; canEditSalary: boolean }): ISecuredEntityCapability {
  return {
    code: 'employee',
    canCreate: options.canCreate,
    canRead: true,
    canUpdate: options.canUpdate,
    canDelete: false,
    attributes: [
      {
        name: 'salary',
        canView: options.canEditSalary,
        canEdit: options.canEditSalary,
      },
    ],
  };
}
