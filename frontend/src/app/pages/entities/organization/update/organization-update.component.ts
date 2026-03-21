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
  showBudgetField = signal(false);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      if (idParam) {
        const id = Number(idParam);
        this.isEdit.set(true);
        this.load(id);
      } else {
        this.isEdit.set(false);
        this.showBudgetField.set(true);
      }
    });
  }

  private load(id: number): void {
    this.organizationService.find(id).subscribe({
      next: res => {
        const org = res.body;
        if (org) {
          this.form.patchValue(org);
          this.showBudgetField.set(org.budget !== undefined);
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
    const value = this.form.value as IOrganization | NewOrganization;
    const request$ = value.id
      ? this.organizationService.update(value as IOrganization)
      : this.organizationService.create(value as NewOrganization);
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
}
