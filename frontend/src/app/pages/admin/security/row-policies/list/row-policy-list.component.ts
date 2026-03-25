import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
  inject,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { finalize } from 'rxjs/operators';

import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';

import { ISecRowPolicy } from '../sec-row-policy.model';
import { SecRowPolicyService } from '../service/sec-row-policy.service';
import RowPolicyDialogComponent from '../dialog/row-policy-dialog.component';
import { addTranslatedMessage, handleHttpError } from 'app/shared/error/http-error.utils';

@Component({
  selector: 'app-row-policy-list',
  standalone: true,
  imports: [
    ButtonModule,
    TableModule,
    CardModule,
    ToastModule,
    ConfirmDialogModule,
    RowPolicyDialogComponent,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './row-policy-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class RowPolicyListComponent implements OnInit {
  rowPolicies: ISecRowPolicy[] = [];
  isLoading = false;
  dialogVisible = false;
  selectedPolicy: ISecRowPolicy | null = null;

  private readonly secRowPolicyService = inject(SecRowPolicyService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);
  private readonly cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.loadPolicies();
  }

  loadPolicies(): void {
    this.isLoading = true;
    this.secRowPolicyService
      .query()
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (response) => (this.rowPolicies = response.body ?? []),
        error: (err: unknown) =>
          handleHttpError(
            this.messageService,
            this.translateService,
            err,
            'feedback.security.rowPolicies.loadFailed',
          ),
      });
  }

  openCreate(): void {
    this.selectedPolicy = null;
    this.dialogVisible = true;
  }

  openEdit(policy: ISecRowPolicy): void {
    this.selectedPolicy = { ...policy };
    this.dialogVisible = true;
  }

  onSaved(): void {
    this.loadPolicies();
  }

  confirmDelete(policy: ISecRowPolicy): void {
    this.confirmationService.confirm({
      header: 'Delete Row Policy',
      message: 'Are you sure you want to delete this row policy? This action cannot be undone.',
      acceptLabel: 'Delete',
      rejectLabel: 'Keep Row Policy',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.deletePolicy(policy),
    });
  }

  truncate(text: string, maxLength = 50): string {
    if (!text) {
      return '';
    }
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
  }

  private deletePolicy(policy: ISecRowPolicy): void {
    if (policy.id === undefined) {
      return;
    }
    this.secRowPolicyService.delete(policy.id).subscribe({
      next: () => {
        addTranslatedMessage(this.messageService, this.translateService, {
          severity: 'success',
          summary: 'feedback.toast.deleted',
          detail: 'feedback.security.rowPolicies.deleted',
          detailParams: { param: policy.code },
        });
        this.loadPolicies();
      },
      error: (err: unknown) =>
        handleHttpError(
          this.messageService,
          this.translateService,
          err,
          'feedback.security.rowPolicies.deleteFailed',
        ),
    });
  }
}
