import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ChangeDetectorRef, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { catchError, finalize, forkJoin, map, of } from 'rxjs';

import { ConfirmationService, MessageService, TreeNode } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { ToastModule } from 'primeng/toast';
import { TreeTableModule } from 'primeng/treetable';

import { APP_NAVIGATION_TREE } from 'app/layout/navigation/navigation-registry';
import { handleHttpError } from 'app/shared/error/http-error.utils';
import { ISecMenuPermissionAdmin } from '../shared/sec-menu-permission-admin.model';
import { ISecCatalogEntry } from '../shared/sec-catalog.model';
import { ISecPermission } from '../shared/sec-permission.model';
import { AdminMenuPermissionService } from '../shared/service/admin-menu-permission.service';
import { SecCatalogService } from '../shared/service/sec-catalog.service';
import { SecPermissionService } from '../shared/service/sec-permission.service';

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

interface MenuFlushSuccessResult {
  key: string;
  menuId: string;
  checked: boolean;
  success: true;
  permissionId?: number;
}

interface MenuFlushErrorResult {
  key: string;
  menuId: string;
  checked: boolean;
  success: false;
  error: unknown;
}

type MenuFlushResult = MenuFlushSuccessResult | MenuFlushErrorResult;

@Component({
  selector: 'app-permission-matrix',
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ButtonModule,
    CheckboxModule,
    ConfirmDialogModule,
    ProgressSpinnerModule,
    TableModule,
    TabsModule,
    ToastModule,
    TranslateModule,
    TreeTableModule,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './permission-matrix.component.html',
})
export default class PermissionMatrixComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly catalogService = inject(SecCatalogService);
  private readonly permissionService = inject(SecPermissionService);
  private readonly menuPermissionService = inject(AdminMenuPermissionService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);
  private readonly cdr = inject(ChangeDetectorRef);

  roleName = '';
  catalogEntries: ISecCatalogEntry[] = [];
  loading = true;
  selectedEntity: ISecCatalogEntry | null = null;
  selectedEntityAttributeRows: AttributeRow[] = [];

  granted = new Map<string, number>();
  pendingChanges = new Map<string, PendingChange>();
  saving = false;

  // Menu access tab state
  menuTreeNodes: TreeNode[] = [];
  menuSelectionKeys: Record<string, { checked?: boolean; partialChecked?: boolean }> = {};
  menuGranted = new Map<string, number>(); // menuId -> permission id
  menuPendingChanges = new Map<string, boolean>(); // menuId -> checked state
  menuLoading = true;
  activeTabValue: string = '0'; // track active tab

  get hasPendingChanges(): boolean {
    return this.pendingChanges.size > 0 || this.menuPendingChanges.size > 0;
  }

  ngOnInit(): void {
    this.translateService.onLangChange.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.selectedEntity) {
        this.selectedEntityAttributeRows = this.buildAttributeRows(this.selectedEntity);
      }
      this.buildMenuTree();
      this.cdr.detectChanges();
    });

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
            this.translateService,
            err,
            'feedback.security.permissionMatrix.loadFailed',
          ),
      });

    this.loadMenuPermissions();
  }

  private loadMenuPermissions(): void {
    this.menuLoading = true;
    this.menuPermissionService.query(this.roleName).subscribe({
      next: (permissions) => {
        this.menuGranted.clear();
        permissions.forEach(p => {
          if (p.id !== undefined) {
            this.menuGranted.set(p.menuId, p.id);
          }
        });
        this.buildMenuTree();
        this.menuLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        handleHttpError(this.messageService, this.translateService, err, 'feedback.security.menuAccess.loadFailed');
        this.menuLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  private buildMenuTree(): void {
    this.menuTreeNodes = APP_NAVIGATION_TREE.map(section => ({
      key: section.id,
      label: this.translateService.instant(section.labelKey),
      expanded: true,
      children: section.children.map(leaf => ({
        key: leaf.id,
        label: this.translateService.instant(leaf.labelKey),
      })),
    }));
    const keys: Record<string, { checked?: boolean; partialChecked?: boolean }> = {};
    for (const section of this.menuTreeNodes) {
      const leaves = section.children ?? [];
      let checkedCount = 0;
      for (const leaf of leaves) {
        if (this.isMenuEffectivelyGranted(leaf.key!)) {
          keys[leaf.key!] = { checked: true, partialChecked: false };
          checkedCount++;
        }
      }
      if (checkedCount > 0 && checkedCount < leaves.length) {
        keys[section.key!] = { partialChecked: true };
      } else if (checkedCount === leaves.length && leaves.length > 0) {
        keys[section.key!] = { checked: true, partialChecked: false };
      }
    }
    this.menuSelectionKeys = keys;
  }

  private isMenuEffectivelyGranted(menuId: string): boolean {
    const pending = this.menuPendingChanges.get(menuId);
    return pending ?? this.menuGranted.has(menuId);
  }

  onMenuSelectionKeysChange(newKeys: Record<string, { checked?: boolean; partialChecked?: boolean }>): void {
    for (const section of this.menuTreeNodes) {
      for (const leaf of section.children ?? []) {
        const menuId = leaf.key!;
        const isNowChecked = !!newKeys[menuId]?.checked;
        const wasGranted = this.menuGranted.has(menuId);
        if (isNowChecked === wasGranted) {
          this.menuPendingChanges.delete(menuId);
        } else {
          this.menuPendingChanges.set(menuId, isNowChecked);
        }
      }
    }
    this.menuSelectionKeys = newKeys;
    this.cdr.detectChanges();
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
      { label: '', target: `${entity.code}.*`, isWildcard: true },
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
      header: this.translateService.instant('security.permissionMatrix.confirmSave.title'),
      message: this.translateService.instant('security.permissionMatrix.confirmSave.message'),
      acceptLabel: this.translateService.instant('entity.action.save'),
      rejectLabel: this.translateService.instant('entity.action.cancel'),
      accept: () => this.flushChanges(),
    });
  }

  discardChanges(): void {
    if (!this.hasPendingChanges || this.saving) {
      return;
    }

    this.pendingChanges.clear();
    this.menuPendingChanges.clear();
    this.buildMenuTree(); // rebuild selection from granted state
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

    const menuResults$ = Array.from(this.menuPendingChanges.entries()).map(([menuId, checked]) => {
      if (checked) {
        const permission: ISecMenuPermissionAdmin = {
          role: this.roleName,
          appName: 'jhipster-security-platform',
          menuId,
          effect: 'ALLOW',
        };
        return this.menuPermissionService.create(permission).pipe(
          map(response => ({
            key: `menu:${menuId}`,
            menuId,
            checked: true,
            success: true as const,
            permissionId: response.body?.id,
          })),
          catchError(error => of({
            key: `menu:${menuId}`,
            menuId,
            checked: true,
            success: false as const,
            error,
          })),
        );
      }
      const permissionId = this.menuGranted.get(menuId);
      if (permissionId === undefined) {
        return of({ key: `menu:${menuId}`, menuId, checked: false, success: true as const, permissionId: undefined });
      }
      return this.menuPermissionService.delete(permissionId).pipe(
        map(() => ({ key: `menu:${menuId}`, menuId, checked: false, success: true as const, permissionId: undefined })),
        catchError(error => of({ key: `menu:${menuId}`, menuId, checked: false, success: false as const, error })),
      );
    });

    this.saving = true;
    this.cdr.detectChanges();

    forkJoin([...operations, ...menuResults$])
      .pipe(
        finalize(() => {
          this.saving = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (results) => {
          results.forEach((result) => {
            if ('menuId' in result) {
              // Menu result
              const menuResult = result as MenuFlushResult;
              if (!menuResult.success) {
                this.showSaveError(menuResult.error);
                return;
              }
              if (menuResult.checked && menuResult.permissionId !== undefined) {
                this.menuGranted.set(menuResult.menuId, menuResult.permissionId);
              } else if (!menuResult.checked) {
                this.menuGranted.delete(menuResult.menuId);
              }
              this.menuPendingChanges.delete(menuResult.menuId);
            } else {
              // Entity permission result
              const entityResult = result as FlushResult;
              if (!entityResult.success) {
                this.showSaveError(entityResult.error);
                return;
              }
              if (entityResult.change.checked) {
                this.granted.set(entityResult.key, entityResult.permissionId!);
              } else {
                this.granted.delete(entityResult.key);
              }
              this.pendingChanges.delete(entityResult.key);
            }
          });

          this.buildMenuTree();
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.showSaveError(err);
          this.cdr.detectChanges();
        },
      });
  }

  private permissionKey(target: string, action: string): string {
    return `${target}:${action}`;
  }

  private showSaveError(
    err: unknown,
    fallback = 'feedback.security.permissionMatrix.saveFailed',
  ): void {
    handleHttpError(this.messageService, this.translateService, err, fallback);
  }
}
