import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { LANGUAGES } from 'app/config/language.constants';
import { addTranslatedMessage, handleHttpError } from 'app/shared/error/http-error.utils';
import { WorkspaceContextService } from 'app/pages/entities/shared/service/workspace-context.service';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { MessageService } from 'primeng/api';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { ToastModule } from 'primeng/toast';

import { IUser, IUserRoleRow } from '../user-management.model';
import { UserManagementService } from '../service/user-management.service';
import { buildAuthorityRows } from '../shared/authority-label.util';
import { UserManagementFormGroup, UserManagementFormService } from './user-management-form.service';

@Component({
  selector: 'app-user-management-update',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    TranslatePipe,
    ButtonModule,
    CardModule,
    CheckboxModule,
    InputTextModule,
    MessageModule,
    SelectModule,
    TableModule,
    ToastModule,
  ],
  providers: [MessageService],
  templateUrl: './user-management-update.component.html',
})
export default class UserManagementUpdateComponent implements OnInit {
  readonly editForm: UserManagementFormGroup = inject(UserManagementFormService).createUserFormGroup();
  readonly roleRows = signal<IUserRoleRow[]>([]);
  readonly isSaving = signal(false);
  readonly isEdit = signal(false);
  readonly languages = [...LANGUAGES].map(language => ({ label: language.toUpperCase(), value: language }));

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly userManagementService = inject(UserManagementService);
  private readonly userManagementFormService = inject(UserManagementFormService);
  private readonly workspaceContextService = inject(WorkspaceContextService);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);

  private readonly navigationNodeId = this.route.snapshot.data['navigationNodeId'] as string | undefined;
  private readonly allAuthorities = signal<string[]>([]);
  private readonly originalUser = signal<IUser | null>(null);

  ngOnInit(): void {
    const resolvedUser = (this.route.snapshot.data['user'] ?? null) as IUser | null;
    this.originalUser.set(resolvedUser);
    this.isEdit.set(!!resolvedUser?.id);
    this.userManagementFormService.resetForm(this.editForm, resolvedUser);

    this.editForm.controls.authorities.valueChanges.subscribe(() => this.refreshRoleRows());

    this.userManagementService.authorities().subscribe({
      next: authorities => {
        this.allAuthorities.set(authorities);
        this.refreshRoleRows();
      },
      error: err => handleHttpError(this.messageService, this.translateService, err),
    });
  }

  save(): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    const payload = this.userManagementFormService.getUser(this.editForm);
    const request$ = payload.id ? this.userManagementService.update(payload) : this.userManagementService.create(payload);
    const failureKey = payload.id ? 'userManagement.feedback.updateFailed' : 'userManagement.feedback.createFailed';

    request$.pipe(finalize(() => this.isSaving.set(false))).subscribe({
      next: savedUser => {
        const login = savedUser.login ?? payload.login;
        if (!login) {
          return;
        }

        addTranslatedMessage(this.messageService, this.translateService, {
          severity: 'success',
          summary: 'feedback.toast.saved',
          detail: 'userManagement.feedback.saved',
          detailParams: { login },
        });
        this.navigateToDetail(login);
      },
      error: err => handleHttpError(this.messageService, this.translateService, err, failureKey),
    });
  }

  cancel(): void {
    if (this.isEdit()) {
      this.navigateToDetail(this.originalUser()?.login ?? this.editForm.controls.login.value);
      return;
    }

    const context = this.navigationNodeId ? this.workspaceContextService.get(this.navigationNodeId) : null;
    this.router.navigate(['/admin/users'], context ? { queryParams: context.queryParams } : undefined);
  }

  toggleAuthority(authority: string, selected: boolean): void {
    const currentAuthorities = new Set(this.editForm.controls.authorities.value);
    if (selected) {
      currentAuthorities.add(authority);
    } else {
      currentAuthorities.delete(authority);
    }

    this.editForm.controls.authorities.setValue([...currentAuthorities].sort());
    this.editForm.controls.authorities.markAsDirty();
  }

  hasError(controlName: keyof UserManagementFormGroup['controls'], errorCode: string): boolean {
    const control = this.editForm.controls[controlName];
    return control.invalid && (control.dirty || control.touched) && !!control.errors?.[errorCode];
  }

  private refreshRoleRows(): void {
    this.roleRows.set(
      buildAuthorityRows(
        this.allAuthorities(),
        this.editForm.controls.authorities.value,
        this.translateService,
        false,
      ),
    );
  }

  private navigateToDetail(login: string): void {
    this.router.navigate(['/admin/users', login, 'view']);
  }
}
