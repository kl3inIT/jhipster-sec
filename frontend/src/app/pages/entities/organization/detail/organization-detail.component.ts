import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

import { IDepartment, NewDepartment } from '../../department/department.model';
import { DepartmentService } from '../../department/service/department.service';
import { IEmployee, NewEmployee } from '../../employee/employee.model';
import { EmployeeService } from '../../employee/service/employee.service';
import { ISecuredEntityCapability } from '../../shared/secured-entity-capability.model';
import { SecuredEntityCapabilityService } from '../../shared/service/secured-entity-capability.service';
import { WorkspaceContextService } from '../../shared/service/workspace-context.service';
import { addTranslatedMessage, handleHttpError } from 'app/shared/error/http-error.utils';
import { IOrganization, NewOrganization } from '../organization.model';
import { OrganizationService } from '../service/organization.service';
import {
  IOrganizationWorkbench,
  IOrganizationWorkbenchDepartment,
  IOrganizationWorkbenchEmployee,
} from './organization-workbench.model';

type DialogMode = 'create' | 'edit';

@Component({
  selector: 'app-organization-detail',
  imports: [
    CommonModule,
    RouterModule,
    TranslatePipe,
    ReactiveFormsModule,
    ButtonModule,
    CardModule,
    ConfirmDialogModule,
    DialogModule,
    InputNumberModule,
    InputTextModule,
    MessageModule,
    SelectModule,
    TableModule,
    ToastModule,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './organization-detail.component.html',
})
export default class OrganizationDetailComponent implements OnInit {
  organization = signal<IOrganizationWorkbench | null>(null);
  loading = signal(false);

  organizationCapability = signal<ISecuredEntityCapability | null>(null);
  departmentCapability = signal<ISecuredEntityCapability>(buildEmptyCapability('department'));
  employeeCapability = signal<ISecuredEntityCapability>(buildEmptyCapability('employee'));

  selectedDepartmentId = signal<number | null>(null);

  organizationDialogVisible = signal(false);
  departmentDialogVisible = signal(false);
  employeeDialogVisible = signal(false);

  departmentDialogMode = signal<DialogMode>('create');
  employeeDialogMode = signal<DialogMode>('create');

  isSavingOrganization = signal(false);
  isSavingDepartment = signal(false);
  isSavingEmployee = signal(false);

  readonly sortedDepartments = computed(() =>
    sortDepartments(this.organization()?.departments ?? []),
  );

  readonly selectedDepartment = computed<IOrganizationWorkbenchDepartment | null>(() => {
    const departments = this.sortedDepartments();
    const selectedId = this.selectedDepartmentId();

    if (selectedId === null) {
      return departments[0] ?? null;
    }

    return departments.find((department) => department.id === selectedId) ?? departments[0] ?? null;
  });

  readonly selectedDepartmentEmployees = computed(() =>
    sortEmployees(this.selectedDepartment()?.employees ?? []),
  );

  readonly departmentCount = computed(() => this.sortedDepartments().length);
  readonly employeeCount = computed(() =>
    this.sortedDepartments().reduce(
      (total, department) => total + (department.employees?.length ?? 0),
      0,
    ),
  );

  readonly canUpdateOrganization = computed(
    () => this.organizationCapability()?.canUpdate ?? false,
  );
  readonly canCreateDepartment = computed(() => this.departmentCapability().canCreate);
  readonly canUpdateDepartment = computed(() => this.departmentCapability().canUpdate);
  readonly canDeleteDepartment = computed(() => this.departmentCapability().canDelete);
  readonly canCreateEmployee = computed(() => this.employeeCapability().canCreate);
  readonly canUpdateEmployee = computed(() => this.employeeCapability().canUpdate);
  readonly canDeleteEmployee = computed(() => this.employeeCapability().canDelete);
  readonly showBudgetSummary = computed(
    () => this.canViewOrganizationField('budget') && this.organization()?.budget !== undefined,
  );
  readonly showEmployeeSalaryColumn = computed(
    () =>
      this.canViewEmployeeField('salary') &&
      this.selectedDepartmentEmployees().some((employee) => employee.salary !== undefined),
  );

  organizationForm: FormGroup;
  departmentForm: FormGroup;
  employeeForm: FormGroup;

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly organizationService = inject(OrganizationService);
  private readonly departmentService = inject(DepartmentService);
  private readonly employeeService = inject(EmployeeService);
  private readonly capabilityService = inject(SecuredEntityCapabilityService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);
  private readonly workspaceContextService = inject(WorkspaceContextService);

  private readonly navigationNodeId = this.route.snapshot.data['navigationNodeId'] as
    | string
    | undefined;

  private currentOrganizationId: number | null = null;

  constructor() {
    this.organizationForm = this.fb.group({
      id: [null as number | null],
      code: ['', [Validators.required, Validators.maxLength(50)]],
      name: ['', [Validators.required, Validators.maxLength(200)]],
      ownerLogin: ['', [Validators.required, Validators.maxLength(50)]],
      budget: [null as number | null],
    });

    this.departmentForm = this.fb.group({
      id: [null as number | null],
      code: ['', [Validators.required, Validators.maxLength(50)]],
      name: ['', [Validators.required, Validators.maxLength(200)]],
      costCenter: ['', [Validators.maxLength(100)]],
    });

    this.employeeForm = this.fb.group({
      id: [null as number | null],
      employeeNumber: ['', [Validators.required, Validators.maxLength(50)]],
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
      departmentId: [null as number | null, [Validators.required]],
      email: ['', [Validators.maxLength(255)]],
      salary: [null as number | null],
    });
  }

  ngOnInit(): void {
    const capability = (this.route.snapshot.data['capability'] ??
      null) as ISecuredEntityCapability | null;
    this.organizationCapability.set(capability);

    if (!capability?.canRead) {
      this.navigateToAccessDenied();
      return;
    }

    this.capabilityService.query().subscribe({
      next: (capabilities) => {
        this.departmentCapability.set(
          findCapability(capabilities, 'department') ?? buildEmptyCapability('department'),
        );
        this.employeeCapability.set(
          findCapability(capabilities, 'employee') ?? buildEmptyCapability('employee'),
        );
      },
      error: (error) => handleHttpError(this.messageService, this.translateService, error),
    });

    this.route.paramMap.subscribe((params) => {
      const idParam = params.get('id');
      if (!idParam) {
        this.router.navigate(['/404']);
        return;
      }

      this.currentOrganizationId = Number(idParam);
      this.loadWorkbench(this.currentOrganizationId);
    });
  }

  back(): void {
    const context = this.navigationNodeId
      ? this.workspaceContextService.get(this.navigationNodeId)
      : null;
    this.router.navigate(
      ['/entities/organization'],
      context ? { queryParams: context.queryParams } : undefined,
    );
  }

  selectDepartment(department: IOrganizationWorkbenchDepartment): void {
    this.selectedDepartmentId.set(department.id ?? null);
  }

  openOrganizationDialog(): void {
    const organization = this.organization();
    if (!organization) {
      return;
    }

    this.organizationForm.reset({
      id: organization.id ?? null,
      code: organization.code ?? '',
      name: organization.name ?? '',
      ownerLogin: organization.ownerLogin ?? '',
      budget: organization.budget ?? null,
    });
    this.organizationDialogVisible.set(true);
  }

  openCreateDepartmentDialog(): void {
    if (!this.canCreateDepartment()) {
      return;
    }

    this.departmentDialogMode.set('create');
    this.departmentForm.reset({
      id: null,
      code: '',
      name: '',
      costCenter: '',
    });
    this.departmentDialogVisible.set(true);
  }

  openEditDepartmentDialog(department: IOrganizationWorkbenchDepartment): void {
    if (!this.canUpdateDepartment()) {
      return;
    }

    this.departmentDialogMode.set('edit');
    this.departmentForm.reset({
      id: department.id ?? null,
      code: department.code ?? '',
      name: department.name ?? '',
      costCenter: department.costCenter ?? '',
    });
    this.departmentDialogVisible.set(true);
  }

  openCreateEmployeeDialog(): void {
    const department = this.selectedDepartment();
    if (!department || !this.canCreateEmployee()) {
      return;
    }

    this.employeeDialogMode.set('create');
    this.employeeForm.reset({
      id: null,
      employeeNumber: '',
      firstName: '',
      lastName: '',
      departmentId: department.id ?? null,
      email: '',
      salary: null,
    });
    this.employeeDialogVisible.set(true);
  }

  openEditEmployeeDialog(employee: IOrganizationWorkbenchEmployee): void {
    if (!this.canUpdateEmployee()) {
      return;
    }

    this.employeeDialogMode.set('edit');
    this.employeeForm.reset({
      id: employee.id ?? null,
      employeeNumber: employee.employeeNumber ?? '',
      firstName: employee.firstName ?? '',
      lastName: employee.lastName ?? '',
      departmentId: employee.department?.id ?? this.selectedDepartment()?.id ?? null,
      email: employee.email ?? '',
      salary: employee.salary ?? null,
    });
    this.employeeDialogVisible.set(true);
  }

  saveOrganization(): void {
    if (this.organizationForm.invalid || this.currentOrganizationId === null) {
      this.organizationForm.markAllAsTouched();
      return;
    }

    const organizationId = this.currentOrganizationId;
    const raw = this.organizationForm.getRawValue();
    const payload: IOrganization | NewOrganization = {
      id: raw.id,
      code: raw.code,
      name: raw.name,
      ownerLogin: raw.ownerLogin,
      ...(this.canEditOrganizationField('budget') && raw.budget !== null
        ? { budget: raw.budget }
        : {}),
    };

    this.isSavingOrganization.set(true);
    this.organizationService
      .update(payload as IOrganization)
      .pipe(finalize(() => this.isSavingOrganization.set(false)))
      .subscribe({
        next: () => {
          this.organizationDialogVisible.set(false);
          addTranslatedMessage(this.messageService, this.translateService, {
            severity: 'success',
            summary: 'feedback.toast.saved',
            detail: 'feedback.entities.organizations.saved',
          });
          this.loadWorkbench(organizationId, this.selectedDepartmentId());
        },
        error: (error) => handleHttpError(this.messageService, this.translateService, error),
      });
  }

  saveDepartment(): void {
    if (this.departmentForm.invalid) {
      this.departmentForm.markAllAsTouched();
      return;
    }

    const organization = this.organization();
    if (organization?.id == null) {
      return;
    }

    const organizationId = organization.id;
    const raw = this.departmentForm.getRawValue();
    const payload: IDepartment | NewDepartment = {
      id: raw.id,
      code: raw.code,
      name: raw.name,
      organization: {
        id: organizationId,
        name: organization.name,
      },
      costCenter: raw.costCenter || undefined,
    };

    const request$ = payload.id
      ? this.departmentService.update(payload as IDepartment)
      : this.departmentService.create(payload as NewDepartment);

    this.isSavingDepartment.set(true);
    request$.pipe(finalize(() => this.isSavingDepartment.set(false))).subscribe({
      next: (response) => {
        this.departmentDialogVisible.set(false);
        addTranslatedMessage(this.messageService, this.translateService, {
          severity: 'success',
          summary: 'feedback.toast.saved',
          detail: 'feedback.entities.departments.saved',
        });
        this.loadWorkbench(
          organizationId,
          response.body?.id ?? raw.id ?? this.selectedDepartmentId(),
        );
      },
      error: (error) => handleHttpError(this.messageService, this.translateService, error),
    });
  }

  saveEmployee(): void {
    if (this.employeeForm.invalid) {
      this.employeeForm.markAllAsTouched();
      return;
    }

    const raw = this.employeeForm.getRawValue();
    const selectedDepartment = this.findDepartmentById(raw.departmentId);

    if (!selectedDepartment?.id || this.currentOrganizationId === null) {
      this.employeeForm.get('departmentId')?.setErrors({ required: true });
      this.employeeForm.markAllAsTouched();
      return;
    }

    const organizationId = this.currentOrganizationId;
    const payload: IEmployee | NewEmployee = {
      id: raw.id,
      employeeNumber: raw.employeeNumber,
      firstName: raw.firstName,
      lastName: raw.lastName,
      department: {
        id: selectedDepartment.id,
        name: selectedDepartment.name,
      },
      email: raw.email || undefined,
      ...(this.canEditEmployeeField('salary') && raw.salary !== null ? { salary: raw.salary } : {}),
    };

    const request$ = payload.id
      ? this.employeeService.update(payload as IEmployee)
      : this.employeeService.create(payload as NewEmployee);

    this.isSavingEmployee.set(true);
    request$.pipe(finalize(() => this.isSavingEmployee.set(false))).subscribe({
      next: () => {
        this.employeeDialogVisible.set(false);
        addTranslatedMessage(this.messageService, this.translateService, {
          severity: 'success',
          summary: 'feedback.toast.saved',
          detail: 'feedback.entities.employees.saved',
        });
        this.loadWorkbench(organizationId, selectedDepartment.id);
      },
      error: (error) => handleHttpError(this.messageService, this.translateService, error),
    });
  }

  confirmDeleteDepartment(department: IOrganizationWorkbenchDepartment): void {
    if (!this.canDeleteDepartment()) {
      return;
    }

    this.confirmationService.confirm({
      message: this.translateService.instant('angappApp.department.delete.question'),
      header: this.translateService.instant('angappApp.department.delete.title'),
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.translateService.instant('entity.action.delete'),
      acceptButtonStyleClass: 'p-button-danger',
      rejectLabel: this.translateService.instant('entity.action.cancel'),
      accept: () => this.deleteDepartment(department),
    });
  }

  deleteDepartment(department: IOrganizationWorkbenchDepartment): void {
    if (!department.id || this.currentOrganizationId === null) {
      return;
    }

    const organizationId = this.currentOrganizationId;
    this.departmentService.delete(department.id).subscribe({
      next: () => {
        addTranslatedMessage(this.messageService, this.translateService, {
          severity: 'success',
          summary: 'feedback.toast.deleted',
          detail: 'feedback.entities.departments.deleted',
        });
        const preferredDepartmentId =
          this.selectedDepartmentId() === department.id ? null : this.selectedDepartmentId();
        this.loadWorkbench(organizationId, preferredDepartmentId);
      },
      error: (error) => handleHttpError(this.messageService, this.translateService, error),
    });
  }

  confirmDeleteEmployee(employee: IOrganizationWorkbenchEmployee): void {
    if (!this.canDeleteEmployee()) {
      return;
    }

    this.confirmationService.confirm({
      message: this.translateService.instant('angappApp.employee.delete.question'),
      header: this.translateService.instant('angappApp.employee.delete.title'),
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.translateService.instant('entity.action.delete'),
      acceptButtonStyleClass: 'p-button-danger',
      rejectLabel: this.translateService.instant('entity.action.cancel'),
      accept: () => this.deleteEmployee(employee),
    });
  }

  deleteEmployee(employee: IOrganizationWorkbenchEmployee): void {
    if (!employee.id || this.currentOrganizationId === null) {
      return;
    }

    const organizationId = this.currentOrganizationId;
    this.employeeService.delete(employee.id).subscribe({
      next: () => {
        addTranslatedMessage(this.messageService, this.translateService, {
          severity: 'success',
          summary: 'feedback.toast.deleted',
          detail: 'feedback.entities.employees.deleted',
        });
        this.loadWorkbench(organizationId, employee.department?.id ?? this.selectedDepartmentId());
      },
      error: (error) => handleHttpError(this.messageService, this.translateService, error),
    });
  }

  canViewOrganizationField(fieldName: string): boolean {
    return this.canViewField(this.organizationCapability(), fieldName);
  }

  canEditOrganizationField(fieldName: string): boolean {
    return this.canEditField(this.organizationCapability(), fieldName);
  }

  canViewDepartmentField(fieldName: string): boolean {
    return this.canViewField(this.departmentCapability(), fieldName);
  }

  canViewEmployeeField(fieldName: string): boolean {
    return this.canViewField(this.employeeCapability(), fieldName);
  }

  canEditEmployeeField(fieldName: string): boolean {
    return this.canEditField(this.employeeCapability(), fieldName);
  }

  get departmentOptions(): Array<{ label: string; value: number }> {
    return this.sortedDepartments()
      .filter((department) => department.id !== undefined)
      .map((department) => ({
        label: department.name ?? department.code ?? String(department.id),
        value: department.id!,
      }));
  }

  private loadWorkbench(
    organizationId: number,
    preferredDepartmentId: number | null = this.selectedDepartmentId(),
  ): void {
    this.loading.set(true);
    this.organizationService
      .findWorkbench(organizationId)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          const workbench = normalizeWorkbench(response.body);
          if (!workbench) {
            this.router.navigate(['/404']);
            return;
          }

          this.organization.set(workbench);
          this.currentOrganizationId = workbench.id ?? organizationId;
          this.syncSelectedDepartment(preferredDepartmentId, workbench.departments ?? []);
        },
        error: (error) => {
          if (hasHttpStatus(error, 404)) {
            this.router.navigate(['/404']);
            return;
          }

          if (hasHttpStatus(error, 403)) {
            this.navigateToAccessDenied();
            return;
          }

          handleHttpError(this.messageService, this.translateService, error);
        },
      });
  }

  private syncSelectedDepartment(
    preferredDepartmentId: number | null,
    departments: IOrganizationWorkbenchDepartment[],
  ): void {
    if (
      preferredDepartmentId !== null &&
      departments.some((department) => department.id === preferredDepartmentId)
    ) {
      this.selectedDepartmentId.set(preferredDepartmentId);
      return;
    }

    this.selectedDepartmentId.set(departments[0]?.id ?? null);
  }

  private findDepartmentById(departmentId: number | null): IOrganizationWorkbenchDepartment | null {
    if (departmentId === null) {
      return null;
    }

    return this.sortedDepartments().find((department) => department.id === departmentId) ?? null;
  }

  private canViewField(capability: ISecuredEntityCapability | null, fieldName: string): boolean {
    if (!capability) {
      return true;
    }

    const attribute = capability.attributes.find((item) => item.name === fieldName);
    return attribute?.canView ?? true;
  }

  private canEditField(capability: ISecuredEntityCapability | null, fieldName: string): boolean {
    if (!capability) {
      return false;
    }

    const attribute = capability.attributes.find((item) => item.name === fieldName);
    return attribute?.canEdit ?? false;
  }

  private navigateToAccessDenied(): void {
    this.router.navigate(['/accessdenied']);
  }
}

function normalizeWorkbench(
  workbench: IOrganizationWorkbench | null,
): IOrganizationWorkbench | null {
  if (!workbench) {
    return null;
  }

  return {
    ...workbench,
    departments: (workbench.departments ?? []).map((department) => ({
      ...department,
      employees: department.employees ?? [],
    })),
  };
}

function sortDepartments(
  departments: IOrganizationWorkbenchDepartment[],
): IOrganizationWorkbenchDepartment[] {
  return [...departments].sort((left, right) => {
    const byName = compareNullableText(left.name, right.name);
    if (byName !== 0) {
      return byName;
    }

    return (left.id ?? 0) - (right.id ?? 0);
  });
}

function sortEmployees(
  employees: IOrganizationWorkbenchEmployee[],
): IOrganizationWorkbenchEmployee[] {
  return [...employees].sort((left, right) => {
    const byLastName = compareNullableText(left.lastName, right.lastName);
    if (byLastName !== 0) {
      return byLastName;
    }

    const byFirstName = compareNullableText(left.firstName, right.firstName);
    if (byFirstName !== 0) {
      return byFirstName;
    }

    return compareNullableText(left.employeeNumber, right.employeeNumber);
  });
}

function compareNullableText(left?: string, right?: string): number {
  return (left ?? '').localeCompare(right ?? '');
}

function findCapability(
  capabilities: ISecuredEntityCapability[],
  code: string,
): ISecuredEntityCapability | null {
  return capabilities.find((capability) => capability.code === code) ?? null;
}

function buildEmptyCapability(code: string): ISecuredEntityCapability {
  return {
    code,
    canCreate: false,
    canRead: false,
    canUpdate: false,
    canDelete: false,
    attributes: [],
  };
}

function hasHttpStatus(error: unknown, status: number): boolean {
  return typeof error === 'object' && error !== null && 'status' in error
    ? (error as { status?: number }).status === status
    : false;
}
