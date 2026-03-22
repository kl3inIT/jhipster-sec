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

import { IOrganization } from '../../organization/organization.model';
import { OrganizationService } from '../../organization/service/organization.service';
import { IDepartment, NewDepartment } from '../department.model';
import { DepartmentService } from '../service/department.service';
import { ISecuredEntityCapability } from '../../shared/secured-entity-capability.model';

@Component({
  selector: 'app-department-update',
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
  templateUrl: './department-update.component.html',
})
export default class DepartmentUpdateComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly departmentService = inject(DepartmentService);
  private readonly organizationService = inject(OrganizationService);
  private readonly messageService = inject(MessageService);

  form: FormGroup = this.fb.group({
    id: [null as number | null],
    code: ['', [Validators.required, Validators.maxLength(50)]],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    organizationId: [null as number | null],
    costCenter: [''],
  });

  isSaving = signal(false);
  isEdit = signal(false);
  isCapabilityReady = signal(false);
  organizations = signal<IOrganization[]>([]);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      this.isCapabilityReady.set(false);
      if (idParam) {
        this.isEdit.set(true);
      } else {
        this.isEdit.set(false);
      }

      const capability = (this.route.snapshot.data['capability'] ?? null) as ISecuredEntityCapability | null;

      if (!capability) {
        this.navigateToAccessDenied();
        return;
      }

      if (idParam ? !capability.canUpdate : !capability.canCreate) {
        this.navigateToAccessDenied();
        return;
      }

      this.loadOrganizations();
      this.isCapabilityReady.set(true);

      if (idParam) {
        this.load(Number(idParam));
      }
    });
  }

  private loadOrganizations(): void {
    this.organizationService.query({ page: 0, size: 1000, sort: ['name,asc'] }).subscribe({
      next: res => this.organizations.set(res.body ?? []),
    });
  }

  private load(id: number): void {
    this.departmentService.find(id).subscribe({
      next: res => {
        const dept = res.body;
        if (dept) {
          this.form.patchValue({
            id: dept.id,
            code: dept.code,
            name: dept.name,
            organizationId: dept.organization?.id ?? null,
            costCenter: dept.costCenter,
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
    const selectedOrg = formValue.organizationId
      ? this.organizations().find(o => o.id === formValue.organizationId)
      : undefined;

    const payload: IDepartment | NewDepartment = {
      id: formValue.id,
      code: formValue.code,
      name: formValue.name,
      organization: selectedOrg ? { id: selectedOrg.id, name: selectedOrg.name } : undefined,
      costCenter: formValue.costCenter || undefined,
    };

    const request$ = payload.id
      ? this.departmentService.update(payload as IDepartment)
      : this.departmentService.create(payload as NewDepartment);

    request$.pipe(finalize(() => this.isSaving.set(false))).subscribe({
      next: () => {
        this.router.navigate(['/entities/department']);
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'An unexpected error occurred. Please try again.' });
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/entities/department']);
  }

  private navigateToAccessDenied(): void {
    this.router.navigate(['/accessdenied']);
  }

  get organizationOptions(): { label: string; value: number }[] {
    return this.organizations()
      .filter(o => o.id !== undefined && o.name !== undefined)
      .map(o => ({ label: o.name!, value: o.id! }));
  }
}
