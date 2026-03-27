import { Component, EventEmitter, Input, OnChanges, Output, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { finalize } from 'rxjs/operators';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';

import { ISecRole } from '../sec-role.model';
import { SecRoleService } from '../service/sec-role.service';
import { handleHttpError } from 'app/shared/error/http-error.utils';

@Component({
  selector: 'app-role-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    ConfirmDialogModule,
    DialogModule,
    InputTextModule,
    SelectModule,
    TranslatePipe,
    ToastModule,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './role-dialog.component.html',
})
export default class RoleDialogComponent implements OnChanges {
  @Input() visible = false;
  @Input() role: ISecRole | null = null;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() saved = new EventEmitter<void>();

  isSaving = false;

  editForm = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(50)],
    }),
    displayName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(255)],
    }),
    type: new FormControl('RESOURCE', { nonNullable: true, validators: [Validators.required] }),
  });

  private readonly secRoleService = inject(SecRoleService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);

  get typeOptions(): Array<{ label: string; value: string }> {
    return [{ label: this.translateService.instant('security.roles.type.resource'), value: 'RESOURCE' }];
  }

  ngOnChanges(): void {
    if (this.visible) {
      if (this.role) {
        this.editForm.patchValue({
          name: this.role.name,
          displayName: this.role.displayName ?? '',
          type: this.role.type,
        });
        this.editForm.get('name')?.disable();
      } else {
        this.editForm.reset({ name: '', displayName: '', type: 'RESOURCE' });
        this.editForm.get('name')?.enable();
      }
    }
  }

  save(): void {
    if (this.editForm.invalid) {
      return;
    }
    this.confirmationService.confirm({
      header: this.translateService.instant('security.roles.confirmSave.title'),
      message: this.translateService.instant('security.roles.confirmSave.message'),
      acceptLabel: this.translateService.instant('entity.action.save'),
      rejectLabel: this.translateService.instant('entity.action.cancel'),
      accept: () => this.performSave(),
    });
  }

  private performSave(): void {
    this.isSaving = true;
    const roleData: ISecRole = {
      name: this.role ? this.role.name : this.editForm.getRawValue().name,
      displayName: this.editForm.getRawValue().displayName || undefined,
      type: this.editForm.getRawValue().type,
    };
    const request$ = this.role
      ? this.secRoleService.update(roleData)
      : this.secRoleService.create(roleData);
    request$.pipe(finalize(() => (this.isSaving = false))).subscribe({
      next: () => {
        this.saved.emit();
        this.close();
      },
      error: (err: unknown) => handleHttpError(this.messageService, this.translateService, err),
    });
  }

  close(): void {
    this.visibleChange.emit(false);
  }
}
