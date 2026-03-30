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
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { ToastModule } from 'primeng/toast';
import { TreeTableModule } from 'primeng/treetable';

import { handleHttpError } from 'app/shared/error/http-error.utils';
import { ISecMenuDefinition } from '../menu-definitions/sec-menu-definition.model';
import { SecMenuDefinitionService } from '../menu-definitions/service/sec-menu-definition.service';
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
  checked: boolean;
  success: true;
  permissionId?: number;
}

interface MenuFlushErrorResult {
  key: string;
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
    SelectModule,
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
  private readonly defaultMenuApp = 'jhipster-security-platform';
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly catalogService = inject(SecCatalogService);
  private readonly permissionService = inject(SecPermissionService);
  private readonly menuPermissionService = inject(AdminMenuPermissionService);
  private readonly menuDefinitionService = inject(SecMenuDefinitionService);
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
  availableMenuApps: string[] = [];
  selectedMenuApp = this.defaultMenuApp;
  menuDefinitions: ISecMenuDefinition[] = [];
  menuTreeNodes: TreeNode[] = [];
  menuSelectionKeys: Record<string, { checked?: boolean; partialChecked?: boolean }> = {};
  menuGranted = new Map<string, number>(); // appName::menuId -> permission id
  menuPendingChanges = new Map<string, boolean>(); // appName::menuId -> checked state
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
    forkJoin({
      permissions: this.menuPermissionService.query(this.roleName),
      definitions: this.menuDefinitionService.queryAll().pipe(
        map(response => response.body ?? []),
        catchError(() => of([])),
      ),
    }).subscribe({
      next: ({ permissions, definitions }) => {
        this.menuGranted.clear();
        permissions.forEach((p) => {
          if (p.id !== undefined) {
            this.menuGranted.set(this.menuPermissionKey(p.appName, p.menuId), p.id);
          }
        });
        this.availableMenuApps = this.resolveAvailableMenuApps(permissions, definitions);
        const nextSelectedApp = this.availableMenuApps.includes(this.selectedMenuApp)
          ? this.selectedMenuApp
          : (this.availableMenuApps[0] ?? this.defaultMenuApp);
        this.loadMenuDefinitions(nextSelectedApp);
      },
      error: (err) => {
        handleHttpError(
          this.messageService,
          this.translateService,
          err,
          'feedback.security.menuAccess.loadFailed',
        );
        this.menuLoading = false;
        this.menuDefinitions = [];
        this.menuTreeNodes = [];
        this.menuSelectionKeys = {};
        this.cdr.detectChanges();
      },
    });
  }

  loadMenuDefinitions(appName: string): void {
    this.selectedMenuApp = appName;
    this.menuLoading = true;
    this.menuDefinitionService
      .query(appName)
      .pipe(
        finalize(() => {
          this.menuLoading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (response) => {
          this.menuDefinitions = response.body ?? [];
          this.buildMenuTree();
        },
        error: (err) => {
          this.menuDefinitions = [];
          this.menuTreeNodes = [];
          this.menuSelectionKeys = {};
          handleHttpError(
            this.messageService,
            this.translateService,
            err,
            'feedback.security.menuAccess.loadFailed',
          );
        },
      });
  }

  onSelectedMenuAppChange(appName: string | null | undefined): void {
    if (!appName || appName === this.selectedMenuApp) {
      return;
    }
    this.loadMenuDefinitions(appName);
  }

  private buildMenuTree(): void {
    const nodeMap = new Map<string, TreeNode>();
    for (const definition of this.menuDefinitions) {
      nodeMap.set(definition.menuId, {
        key: this.menuPermissionKey(definition.appName, definition.menuId),
        label: this.translateService.instant(definition.label),
        data: definition,
        expanded: true,
        children: [],
      });
    }

    const roots: TreeNode[] = [];
    for (const definition of this.menuDefinitions) {
      const node = nodeMap.get(definition.menuId)!;
      if (definition.parentMenuId && nodeMap.has(definition.parentMenuId)) {
        nodeMap.get(definition.parentMenuId)!.children!.push(node);
      } else {
        roots.push(node);
      }
    }

    const sortNodes = (nodes: TreeNode[]): void => {
      nodes.sort((left, right) => {
        const leftOrdering = (left.data as ISecMenuDefinition).ordering ?? 0;
        const rightOrdering = (right.data as ISecMenuDefinition).ordering ?? 0;
        return leftOrdering - rightOrdering;
      });
      nodes.forEach((node) => {
        if (node.children?.length) {
          sortNodes(node.children);
        }
      });
    };

    sortNodes(roots);
    this.menuTreeNodes = roots;
    this.menuSelectionKeys = this.buildMenuSelectionKeys(roots);
  }

  private buildMenuSelectionKeys(
    nodes: TreeNode[],
  ): Record<string, { checked?: boolean; partialChecked?: boolean }> {
    const selectionKeys: Record<string, { checked?: boolean; partialChecked?: boolean }> = {};

    const visit = (node: TreeNode): boolean => {
      const children = node.children ?? [];
      if (children.length === 0) {
        if (this.isMenuEffectivelyGranted(node.key!)) {
          selectionKeys[node.key!] = { checked: true, partialChecked: false };
          return true;
        }
        return false;
      }

      let checkedChildren = 0;
      let partiallyChecked = false;
      for (const child of children) {
        if (visit(child)) {
          checkedChildren++;
        }
        partiallyChecked = partiallyChecked || !!selectionKeys[child.key!]?.partialChecked;
      }

      if (partiallyChecked || (checkedChildren > 0 && checkedChildren < children.length)) {
        selectionKeys[node.key!] = { partialChecked: true };
        return false;
      }

      if (checkedChildren === children.length && children.length > 0) {
        selectionKeys[node.key!] = { checked: true, partialChecked: false };
        return true;
      }

      return false;
    };

    nodes.forEach(visit);
    return selectionKeys;
  }

  private isMenuEffectivelyGranted(menuKey: string): boolean {
    const pending = this.menuPendingChanges.get(menuKey);
    return pending ?? this.menuGranted.has(menuKey);
  }

  onMenuSelectionKeysChange(
    newKeys: Record<string, { checked?: boolean; partialChecked?: boolean }>,
  ): void {
    for (const leaf of this.collectLeafNodes(this.menuTreeNodes)) {
      const menuKey = leaf.key!;
      const isNowChecked = !!newKeys[menuKey]?.checked;
      const wasGranted = this.menuGranted.has(menuKey);
      if (isNowChecked === wasGranted) {
        this.menuPendingChanges.delete(menuKey);
      } else {
        this.menuPendingChanges.set(menuKey, isNowChecked);
      }
    }
    this.menuSelectionKeys = newKeys;
    this.cdr.detectChanges();
  }

  private collectLeafNodes(nodes: TreeNode[]): TreeNode[] {
    const leaves: TreeNode[] = [];
    const visit = (node: TreeNode): void => {
      if (!node.children?.length) {
        leaves.push(node);
        return;
      }
      node.children.forEach(visit);
    };
    nodes.forEach(visit);
    return leaves;
  }

  private resolveAvailableMenuApps(
    permissions: ISecMenuPermissionAdmin[],
    definitions: ISecMenuDefinition[],
  ): string[] {
    const apps = Array.from(
      new Set([
        ...permissions.map(permission => permission.appName).filter(appName => appName?.trim()),
        ...definitions.map(definition => definition.appName).filter(appName => appName?.trim()),
      ]),
    ).sort((left, right) => {
      if (left === this.defaultMenuApp) {
        return -1;
      }
      if (right === this.defaultMenuApp) {
        return 1;
      }
      return left.localeCompare(right);
    });
    return apps.length > 0 ? apps : [this.defaultMenuApp];
  }

  private menuPermissionKey(appName: string, menuId: string): string {
    return `${appName}::${menuId}`;
  }

  private parseMenuPermissionKey(menuKey: string): { appName: string; menuId: string } {
    const separatorIndex = menuKey.indexOf('::');
    if (separatorIndex < 0) {
      return { appName: this.selectedMenuApp, menuId: menuKey };
    }
    return {
      appName: menuKey.slice(0, separatorIndex),
      menuId: menuKey.slice(separatorIndex + 2),
    };
  }

  currentMenuAppHasDefinitions(): boolean {
    return this.menuDefinitions.length > 0;
  }

  currentMenuAppLabel(appName: string): string {
    return appName;
  }

  menuAppOptions(): Array<{ label: string; value: string }> {
    return this.availableMenuApps.map((appName) => ({
      label: this.currentMenuAppLabel(appName),
      value: appName,
    }));
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

  isAttributeEffectivelyGranted(target: string, action: string, entityCode: string): boolean {
    return (
      this.isEffectivelyGranted(target, action) ||
      this.isWildcardEffectivelyGranted(entityCode, action)
    );
  }

  /**
   * Returns true when EDIT is effectively granted for the given attribute target, either explicitly
   * or via the entity-level wildcard (entityCode.*:EDIT). This mirrors Jmix behaviour where modify
   * access inherently implies view access.
   */
  isViewImpliedByModify(target: string, entityCode: string): boolean {
    return this.isAttributeEffectivelyGranted(target, 'EDIT', entityCode);
  }

  /**
   * Returns true when the entity-level wildcard (`*`) is effectively granted for the given action.
   */
  isEntityWildcardEffectivelyGranted(action: string): boolean {
    return this.isEffectivelyGranted('*', action);
  }

  /**
   * Returns true when either the specific entity or the entity-level wildcard (`*`) is effectively
   * granted for the given action.
   */
  isEntityEffectivelyGranted(entityCode: string, action: string): boolean {
    return this.isEffectivelyGranted(entityCode, action) || this.isEntityWildcardEffectivelyGranted(action);
  }

  /**
   * Returns the catalog entries list prepended with a synthetic wildcard entry representing
   * "grant this action for ALL entities". The wildcard entry has code `*`.
   */
  get catalogEntriesWithWildcard(): ISecCatalogEntry[] {
    const wildcardEntry: ISecCatalogEntry = {
      code: '*',
      displayName: this.translateService.instant('security.permissionMatrix.entity.wildcard'),
      operations: ['CREATE', 'READ', 'UPDATE', 'DELETE'],
      attributes: [],
    };
    return [wildcardEntry, ...this.catalogEntries];
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

    const menuResults$ = Array.from(this.menuPendingChanges.entries()).map(([menuKey, checked]) => {
      const { appName, menuId } = this.parseMenuPermissionKey(menuKey);
      if (checked) {
        const permission: ISecMenuPermissionAdmin = {
          role: this.roleName,
          appName,
          menuId,
          effect: 'ALLOW',
        };
        return this.menuPermissionService.create(permission).pipe(
          map((response) => ({
            key: menuKey,
            checked: true,
            success: true as const,
            permissionId: response.body?.id,
          })),
          catchError((error) =>
            of({
              key: menuKey,
              checked: true,
              success: false as const,
              error,
            }),
          ),
        );
      }
      const permissionId = this.menuGranted.get(menuKey);
      if (permissionId === undefined) {
        return of({
          key: menuKey,
          checked: false,
          success: true as const,
          permissionId: undefined,
        });
      }
      return this.menuPermissionService.delete(permissionId).pipe(
        map(() => ({
          key: menuKey,
          checked: false,
          success: true as const,
          permissionId: undefined,
        })),
        catchError((error) => of({ key: menuKey, checked: false, success: false as const, error })),
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
            if (!('change' in result)) {
              const menuResult = result as MenuFlushResult;
              if (!menuResult.success) {
                this.showSaveError(menuResult.error);
                return;
              }
              if (menuResult.checked && menuResult.permissionId !== undefined) {
                this.menuGranted.set(menuResult.key, menuResult.permissionId);
              } else if (!menuResult.checked) {
                this.menuGranted.delete(menuResult.key);
              }
              this.menuPendingChanges.delete(menuResult.key);
            } else {
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
