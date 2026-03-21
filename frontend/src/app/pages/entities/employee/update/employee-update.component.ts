import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { finalize, take } from 'rxjs';

import { IDepartment } from '../../department/department.model';
import { DepartmentService } from '../../department/service/department.service';
import { IEmployee, NewEmployee } from '../employee.model';
import { EmployeeService } from '../service/employee.service';
import { ISecuredEntityCapability } from '../../shared/secured-entity-capability.model';
import { SecuredEntityCapabilityService } from '../../shared/service/secured-entity-capability.service';

@Component({
  selector: 'app-employee-update',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    MessageModule,
    SelectModule,
    ToastModule,
  ],
  providers: [MessageService],
  templateUrl: './employee-update.component.html',
})
export default class EmployeeUpdateComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly employeeService = inject(EmployeeService);
  private readonly departmentService = inject(DepartmentService);
  private readonly securedEntityCapabilityService = inject(SecuredEntityCapabilityService);
  private readonly messageService = inject(MessageService);

  form: FormGroup = this.fb.group({
    id: [null as number | null],
    employeeNumber: ['', [Validators.required, Validators.maxLength(50)]],
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    departmentId: [null as number | null],
    email: [''],
    salary: [null as number | null],
  });

  isSaving = signal(false);
  isEdit = signal(false);
  isCapabilityReady = signal(false);
  showSalaryField = signal(false);
  departments = signal<IDepartment[]>([]);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      this.isCapabilityReady.set(false);
      this.showSalaryField.set(false);
      if (idParam) {
        this.isEdit.set(true);
      } else {
        this.isEdit.set(false);
      }
      this.loadCapability(idParam);
    });
  }

  private loadCapability(idParam: string | null): void {
    this.securedEntityCapabilityService
      .getEntityCapability('employee')
      .pipe(take(1))
      .subscribe({
        next: capability => {
          if (!capability) {
            this.navigateToAccessDenied();
            return;
          }

          if (idParam ? !capability.canUpdate : !capability.canCreate) {
            this.navigateToAccessDenied();
            return;
          }

          this.showSalaryField.set(this.canEditAttribute(capability, 'salary'));
          this.loadDepartments();
          this.isCapabilityReady.set(true);

          if (idParam) {
            this.load(Number(idParam));
          }
        },
        error: () => this.navigateToAccessDenied(),
      });
  }

  private loadDepartments(): void {
    this.departmentService.query({ page: 0, size: 1000, sort: ['name,asc'] }).subscribe({
      next: res => this.departments.set(res.body ?? []),
    });
  }

  private load(id: number): void {
    this.employeeService.find(id).subscribe({
      next: res => {
        const emp = res.body;
        if (emp) {
          this.form.patchValue({
            id: emp.id,
            employeeNumber: emp.employeeNumber,
            firstName: emp.firstName,
            lastName: emp.lastName,
            departmentId: emp.department?.id ?? null,
            email: emp.email,
            salary: emp.salary ?? null,
          });
        }
      },
      error: () => {
        this.router.navigate(['/404']);
      },
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSaving.set(true);
    const formValue = this.form.value;
    const selectedDept = formValue.departmentId
      ? this.departments().find(d => d.id === formValue.departmentId)
      : undefined;

    const payload: IEmployee | NewEmployee = {
      id: formValue.id,
      employeeNumber: formValue.employeeNumber,
      firstName: formValue.firstName,
      lastName: formValue.lastName,
      department: selectedDept ? { id: selectedDept.id, name: selectedDept.name } : undefined,
      email: formValue.email || undefined,
      salary: this.showSalaryField() && formValue.salary !== null ? formValue.salary : undefined,
    };

    const request$ = payload.id
      ? this.employeeService.update(payload as IEmployee)
      : this.employeeService.create(payload as NewEmployee);

    request$.pipe(finalize(() => this.isSaving.set(false))).subscribe({
      next: () => {
        this.router.navigate(['/entities/employee']);
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'An unexpected error occurred. Please try again.' });
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/entities/employee']);
  }

  private canEditAttribute(capability: ISecuredEntityCapability, attributeName: string): boolean {
    return capability.attributes.some(attribute => attribute.name === attributeName && attribute.canEdit);
  }

  private navigateToAccessDenied(): void {
    this.router.navigate(['/accessdenied']);
  }

  get departmentOptions(): { label: string; value: number }[] {
    return this.departments()
      .filter(d => d.id !== undefined && d.name !== undefined)
      .map(d => ({ label: d.name!, value: d.id! }));
  }
}
