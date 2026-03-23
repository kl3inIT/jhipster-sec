import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { finalize } from 'rxjs/operators';

import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TagModule } from 'primeng/tag';
import { ConfirmationService, MessageService } from 'primeng/api';

import { ISecRole } from '../sec-role.model';
import { SecRoleService } from '../service/sec-role.service';
import RoleDialogComponent from '../dialog/role-dialog.component';
import { handleHttpError } from 'app/shared/error/http-error.utils';

@Component({
  selector: 'app-role-list',
  standalone: true,
  imports: [RouterModule, ButtonModule, TableModule, CardModule, ToastModule, ConfirmDialogModule, TagModule, RoleDialogComponent],
  providers: [ConfirmationService, MessageService],
  templateUrl: './role-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class RoleListComponent implements OnInit {
  roles: ISecRole[] = [];
  isLoading = false;
  dialogVisible = false;
  selectedRole: ISecRole | null = null;

  private readonly secRoleService = inject(SecRoleService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.loadRoles();
  }

  loadRoles(): void {
    this.isLoading = true;
    this.secRoleService
      .query()
      .pipe(finalize(() => { this.isLoading = false; this.cdr.markForCheck(); }))
      .subscribe({
        next: response => (this.roles = response.body ?? []),
        error: (err: any) => handleHttpError(this.messageService, err, 'Failed to load roles.'),
      });
  }

  openCreate(): void {
    this.selectedRole = null;
    this.dialogVisible = true;
  }

  openEdit(role: ISecRole): void {
    this.selectedRole = { ...role };
    this.dialogVisible = true;
  }

  onSaved(): void {
    this.loadRoles();
  }

  confirmDelete(role: ISecRole): void {
    this.confirmationService.confirm({
      header: 'Delete Role',
      message: 'Are you sure you want to delete this role? All permissions assigned to this role will also be removed.',
      acceptLabel: 'Delete',
      rejectLabel: 'Keep Role',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.deleteRole(role),
    });
  }

  private deleteRole(role: ISecRole): void {
    this.secRoleService.delete(role.name).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Deleted', detail: `Role "${role.name}" has been deleted.` });
        this.loadRoles();
      },
      error: (err: any) => handleHttpError(this.messageService, err, 'Failed to delete role.'),
    });
  }
}
