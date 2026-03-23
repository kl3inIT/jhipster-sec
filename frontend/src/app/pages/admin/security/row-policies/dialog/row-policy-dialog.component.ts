import { Component, EventEmitter, Input, OnChanges, OnInit, Output, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

import { ISecRowPolicy } from '../sec-row-policy.model';
import { SecRowPolicyService } from '../service/sec-row-policy.service';
import { SecCatalogService } from '../../shared/service/sec-catalog.service';
import { ISecCatalogEntry } from '../../shared/sec-catalog.model';
import { handleHttpError } from 'app/shared/error/http-error.utils';

@Component({
  selector: 'app-row-policy-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonModule, DialogModule, InputTextModule, SelectModule, TextareaModule, ToastModule],
  providers: [MessageService],
  templateUrl: './row-policy-dialog.component.html',
})
export default class RowPolicyDialogComponent implements OnChanges, OnInit {
  @Input() visible = false;
  @Input() rowPolicy: ISecRowPolicy | null = null;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() saved = new EventEmitter<void>();

  isSaving = false;
  catalogEntries = signal<{ label: string; value: string }[]>([]);

  readonly operationOptions = [
    { label: 'Read', value: 'READ' },
    { label: 'Update', value: 'UPDATE' },
    { label: 'Delete', value: 'DELETE' },
  ];

  readonly policyTypeOptions = [
    { label: 'Specification', value: 'SPECIFICATION' },
    { label: 'JPQL', value: 'JPQL' },
  ];

  editForm = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    entityName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    operation: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    policyType: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    expression: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(1000)] }),
  });

  private readonly secRowPolicyService = inject(SecRowPolicyService);
  private readonly catalogService = inject(SecCatalogService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    this.loadCatalog();
  }

  ngOnChanges(): void {
    if (this.visible) {
      if (this.rowPolicy) {
        this.editForm.patchValue({
          code: this.rowPolicy.code,
          entityName: this.rowPolicy.entityName,
          operation: this.rowPolicy.operation,
          policyType: this.rowPolicy.policyType,
          expression: this.rowPolicy.expression,
        });
      } else {
        this.editForm.reset();
      }
    }
  }

  private loadCatalog(): void {
    this.catalogService.query().subscribe({
      next: (entries: ISecCatalogEntry[]) => {
        this.catalogEntries.set(entries.map(e => ({ label: e.displayName, value: e.code })));
      },
      error: () => {
        this.catalogEntries.set([]);
      },
    });
  }

  save(): void {
    if (this.editForm.invalid) {
      return;
    }
    this.isSaving = true;
    const formValue = this.editForm.getRawValue();
    const policyData: ISecRowPolicy = {
      id: this.rowPolicy?.id,
      code: formValue.code,
      entityName: formValue.entityName,
      operation: formValue.operation,
      policyType: formValue.policyType,
      expression: formValue.expression,
    };
    const request$ = this.rowPolicy?.id ? this.secRowPolicyService.update(policyData) : this.secRowPolicyService.create(policyData);
    request$
      .pipe(finalize(() => (this.isSaving = false)))
      .subscribe({
        next: () => {
          this.saved.emit();
          this.close();
        },
        error: (err: any) => handleHttpError(this.messageService, err, 'Could not save the row policy. Check the expression and try again.'),
      });
  }

  close(): void {
    this.visibleChange.emit(false);
  }
}
