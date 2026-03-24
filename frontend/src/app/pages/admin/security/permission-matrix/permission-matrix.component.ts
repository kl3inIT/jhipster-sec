import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { catchError, finalize, forkJoin, map, of } from 'rxjs';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SplitterModule } from 'primeng/splitter';
import { TableModule } from 'primeng/table';
import { ToastModule } from 'primeng/toast';

import { ISecCatalogEntry } from '../shared/sec-catalog.model';
import { ISecPermission } from '../shared/sec-permission.model';
import { SecCatalogService } from '../shared/service/sec-catalog.service';
import { SecPermissionService } from '../shared/service/sec-permission.service';
import { handleHttpError } from 'app/shared/error/http-error.utils';

interface AttributeRow {
  label: string;
  target: string;
  isWildcard: boolean;
}

interface PendingChange {
  checked: boolean;
  targetType: string;
  target: string;
  action: string;
}

interface FlushSuccessResult {
  key: string;
  change: PendingChange;
  success: true;
  permissionId?: number;
}

interface FlushErrorResult {
  key: string;
  change: PendingChange;
  success: false;
  error: unknown;
}

type FlushResult = FlushSuccessResult | FlushErrorResult;

@Component({
  selector: 'app-permission-matrix',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ButtonModule,
    CheckboxModule,
    ConfirmDialogModule,
    ProgressSpinnerModule,
    SplitterModule,
    TableModule,
    ToastModule,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './permission-matrix.component.html',
})
export default class PermissionMatrixComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly catalogService = inject(SecCatalogService);
  private readonly permissionService = inject(SecPermissionService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly cdr = inject(ChangeDetectorRef);

  roleName = '';
  catalogEntries: ISecCatalogEntry[] = [];
  loading = true;
  selectedEntity: ISecCatalogEntry | null = null;
  selectedEntityAttributeRows: AttributeRow[] = [];

  granted = new Map<string, number>();
  pendingChanges = new Map<string, PendingChange>();
  saving = false;

  get hasPendingChanges(): boolean {
    return this.pendingChanges.size > 0;
  }

  ngOnInit(): void {
    this.roleName = this.route.snapshot.paramMap.get('name') ?? '';
    if (!this.roleName) {
      this.loading = false;
      return;
    }

    forkJoin({
      catalogEntries: this.catalogService.query(),
      permissions: this.permissionService.query(this.roleName),
    })
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: ({ catalogEntries, permissions }) => {
          this.catalogEntries = catalogEntries;
          this.granted.clear();
          permissions.forEach((permission) => {
            if (permission.id !== undefined) {
              this.granted.set(
                this.permissionKey(permission.target, permission.action),
                permission.id,
              );
            }
          });
        },
        error: (err: any) =>
          handleHttpError(
            this.messageService,
            err,
            'Could not load the permission matrix. Please try again.',
          ),
      });
  }

  onEntitySelect(entry: ISecCatalogEntry): void {
    this.selectedEntity = entry;
    this.selectedEntityAttributeRows = this.buildAttributeRows(entry);
    this.cdr.detectChanges();
  }

  isGranted(target: string, action: string): boolean {
    return this.granted.has(this.permissionKey(target, action));
  }

  isEffectivelyGranted(target: string, action: string): boolean {
    const key = this.permissionKey(target, action);
    const pendingChange = this.pendingChanges.get(key);
    return pendingChange?.checked ?? this.granted.has(key);
  }

  isWildcardEffectivelyGranted(entityCode: string, action: string): boolean {
    return this.isEffectivelyGranted(`${entityCode}.*`, action);
  }

  isPendingChange(target: string, action: string): boolean {
    return this.pendingChanges.has(this.permissionKey(target, action));
  }

  private buildAttributeRows(entity: ISecCatalogEntry): AttributeRow[] {
    return [
      { label: 'All attributes (*)', target: `${entity.code}.*`, isWildcard: true },
      ...entity.attributes.map((attribute) => ({
        label: attribute,
        target: `${entity.code}.${attribute}`,
        isWildcard: false,
      })),
    ];
  }

  togglePermission(targetType: string, target: string, action: string, checked: boolean): void {
    const key = this.permissionKey(target, action);
    if (checked === this.isGranted(target, action)) {
      this.pendingChanges.delete(key);
    } else {
      this.pendingChanges.set(key, { checked, targetType, target, action });
    }
    this.cdr.detectChanges();
  }

  confirmSave(): void {
    if (!this.hasPendingChanges || this.saving) {
      return;
    }

    this.confirmationService.confirm({
      header: 'Save Permissions',
      message: 'Do you want to save the permission changes?',
      acceptLabel: 'Save',
      rejectLabel: 'Cancel',
      accept: () => this.flushChanges(),
    });
  }

  discardChanges(): void {
    if (!this.hasPendingChanges || this.saving) {
      return;
    }

    this.pendingChanges.clear();
    this.cdr.detectChanges();
  }

  flushChanges(): void {
    if (!this.hasPendingChanges || this.saving) {
      return;
    }

    const operations = Array.from(this.pendingChanges.entries()).map(([key, change]) => {
      if (change.checked) {
        const permission: ISecPermission = {
          authorityName: this.roleName,
          targetType: change.targetType,
          target: change.target,
          action: change.action,
          effect: 'GRANT',
        };

        return this.permissionService.create(permission).pipe(
          map((response) => {
            const permissionId = response.body?.id;
            if (permissionId === undefined) {
              throw new Error('Permission create response did not include an id.');
            }

            return {
              key,
              change,
              success: true as const,
              permissionId,
            };
          }),
          catchError((error) =>
            of({
              key,
              change,
              success: false as const,
              error,
            }),
          ),
        );
      }

      const permissionId = this.granted.get(key);
      if (permissionId === undefined || permissionId < 0) {
        return of({
          key,
          change,
          success: true as const,
          permissionId: undefined,
        });
      }

      return this.permissionService.delete(permissionId).pipe(
        map(() => ({
          key,
          change,
          success: true as const,
          permissionId: undefined,
        })),
        catchError((error) =>
          of({
            key,
            change,
            success: false as const,
            error,
          }),
        ),
      );
    });

    this.saving = true;
    this.cdr.detectChanges();

    forkJoin(operations)
      .pipe(
        finalize(() => {
          this.saving = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (results) => {
          results.forEach((result) => {
            if (!result.success) {
              this.showSaveError(result.error);
              return;
            }

            if (result.change.checked) {
              this.granted.set(result.key, result.permissionId!);
            } else {
              this.granted.delete(result.key);
            }

            this.pendingChanges.delete(result.key);
          });

          this.cdr.detectChanges();
        },
        error: (err) => {
          this.showSaveError(err, 'Could not save the permission changes. Please try again.');
          this.cdr.detectChanges();
        },
      });
  }

  private permissionKey(target: string, action: string): string {
    return `${target}:${action}`;
  }

  private showSaveError(
    err: unknown,
    fallback = 'Could not save the permission changes. Please try again.',
  ): void {
    handleHttpError(this.messageService, err, fallback);
  }
}
