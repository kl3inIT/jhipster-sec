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
import { finalize } from 'rxjs';

import { IDepartment } from '../../department/department.model';
import { DepartmentService } from '../../department/service/department.service';
import { IEmployee, NewEmployee } from '../employee.model';
import { EmployeeService } from '../service/employee.service';

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
  showSalaryField = signal(false);
  departments = signal<IDepartment[]>([]);

  ngOnInit(): void {
    this.loadDepartments();
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      if (idParam) {
        const id = Number(idParam);
        this.isEdit.set(true);
        this.load(id);
      } else {
        this.isEdit.set(false);
        this.showSalaryField.set(true);
      }
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
          this.showSalaryField.set(emp.salary !== undefined);
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
      salary: formValue.salary !== null ? formValue.salary : undefined,
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

  get departmentOptions(): { label: string; value: number }[] {
    return this.departments()
      .filter(d => d.id !== undefined && d.name !== undefined)
      .map(d => ({ label: d.name!, value: d.id! }));
  }
}
