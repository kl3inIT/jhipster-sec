import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { finalize } from 'rxjs';

import { IOrganization, NewOrganization } from '../organization.model';
import { OrganizationService } from '../service/organization.service';
import { ISecuredEntityCapability } from '../../shared/secured-entity-capability.model';

@Component({
  selector: 'app-organization-update',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, CardModule, ButtonModule, InputTextModule, MessageModule, ToastModule],
  providers: [MessageService],
  templateUrl: './organization-update.component.html',
})
export default class OrganizationUpdateComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly organizationService = inject(OrganizationService);
  private readonly messageService = inject(MessageService);

  form: FormGroup = this.fb.group({
    id: [null as number | null],
    code: ['', [Validators.required, Validators.maxLength(50)]],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    ownerLogin: ['', [Validators.required, Validators.maxLength(50)]],
    budget: [null as number | null],
  });

  isSaving = signal(false);
  isEdit = signal(false);
  isCapabilityReady = signal(false);
  showBudgetField = signal(false);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      this.isCapabilityReady.set(false);
      this.showBudgetField.set(false);
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

      this.showBudgetField.set(this.canEditAttribute(capability, 'budget'));
      this.isCapabilityReady.set(true);

      if (idParam) {
        this.load(Number(idParam));
      }
    });
  }

  private load(id: number): void {
    this.organizationService.find(id).subscribe({
      next: res => {
        const org = res.body;
        if (org) {
          this.form.patchValue(org);
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
    const formValue = this.form.getRawValue();
    const payload: IOrganization | NewOrganization = {
      id: formValue.id,
      code: formValue.code,
      name: formValue.name,
      ownerLogin: formValue.ownerLogin,
      ...(this.showBudgetField() && formValue.budget !== null ? { budget: formValue.budget } : {}),
    };
    const request$ = payload.id
      ? this.organizationService.update(payload as IOrganization)
      : this.organizationService.create(payload as NewOrganization);
    request$.pipe(finalize(() => this.isSaving.set(false))).subscribe({
      next: () => {
        this.router.navigate(['/entities/organization']);
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'An unexpected error occurred. Please try again.' });
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/entities/organization']);
  }

  private canEditAttribute(capability: ISecuredEntityCapability, attributeName: string): boolean {
    return capability.attributes.some(attribute => attribute.name === attributeName && attribute.canEdit);
  }

  private navigateToAccessDenied(): void {
    this.router.navigate(['/accessdenied']);
  }
}
