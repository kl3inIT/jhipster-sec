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
  styles: [
    `
      :host {
        display: block;
      }

      .permission-section {
        margin: 0 1rem 1rem;
        border: 1px solid rgba(148, 163, 184, 0.18);
        border-radius: 1rem;
        background:
          linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.96));
        box-shadow: 0 18px 36px rgba(15, 23, 42, 0.06);
        overflow: hidden;
      }

      .permission-section--attributes {
        margin-top: 0;
      }

      .permission-section__header {
        padding: 1.1rem 1.25rem 0.35rem;
      }

      .permission-section__title {
        margin: 0;
        font-size: 1rem;
        font-weight: 700;
        color: #0f172a;
      }

      .permission-empty-state {
        margin: 0 1rem 1rem;
        padding: 1rem 1.25rem;
        border: 1px dashed rgba(148, 163, 184, 0.35);
        border-radius: 1rem;
        color: #64748b;
        background: rgba(248, 250, 252, 0.82);
      }

      :host ::ng-deep .permission-tabs {
        padding: 0 1rem;
      }

      :host ::ng-deep .permission-tabs .p-tablist {
        margin: 0 0 0.85rem;
        padding: 0;
        border: 0;
        border-radius: 0;
        background: transparent;
        box-shadow: none;
      }

      :host ::ng-deep .permission-tabs .p-tablist::before,
      :host ::ng-deep .permission-tabs .p-tablist::after,
      :host ::ng-deep .permission-tabs .p-tab::before,
      :host ::ng-deep .permission-tabs .p-tab::after {
        display: none !important;
        content: none !important;
      }

      :host ::ng-deep .permission-tabs .p-tablist-tab-list {
        gap: 1.25rem;
        border: 0;
        box-shadow: none;
      }

      :host ::ng-deep .permission-tabs .p-tab {
        border: 0 !important;
        border-bottom: 0 !important;
        border-radius: 0;
        background: transparent;
        color: #475569;
        font-weight: 700;
        padding: 0.9rem 0.15rem 0.8rem;
        box-shadow: none;
        transition:
          background-color 0.18s ease,
          box-shadow 0.18s ease,
          color 0.18s ease,
          border-color 0.18s ease;
      }

      :host ::ng-deep .permission-tabs .p-tab:hover {
        color: #0f766e;
        background: transparent;
      }

      :host ::ng-deep .permission-tabs .p-tab.p-tab-active {
        color: #0f766e;
        background: transparent;
        border: 0 !important;
        border-bottom: 0 !important;
        box-shadow: none !important;
      }

      :host ::ng-deep .permission-tabs .p-tablist-content,
      :host ::ng-deep .permission-tabs .p-tabpanels,
      :host ::ng-deep .permission-tabs .p-tabpanel {
        border: 0;
      }

      :host ::ng-deep .permission-tabs .p-tablist-active-bar {
        display: none !important;
        opacity: 0;
        height: 0;
      }

      .permission-matrix__entity-col {
        min-width: 20rem;
      }

      .permission-matrix__action-col {
        width: 5.5rem;
        min-width: 5.5rem;
        white-space: nowrap;
        text-align: center;
      }

      .permission-entity-cell {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
      }

      .permission-entity-cell__label {
        color: #0f172a;
        font-weight: 600;
      }

      .permission-entity-cell__label--wildcard {
        font-weight: 700;
      }

      .permission-entity-cell__code {
        flex-shrink: 0;
        padding: 0.2rem 0.6rem;
        border-radius: 999px;
        background: rgba(148, 163, 184, 0.16);
        color: #475569;
        font-size: 0.75rem;
        line-height: 1.2;
      }

      .permission-attribute-cell {
        color: #0f172a;
      }

      .permission-cell {
        text-align: center;
      }

      .permission-toggle {
        display: inline-flex;
        min-width: 1.7rem;
        min-height: 1.7rem;
        align-items: center;
        justify-content: center;
        transition: filter 0.18s ease;
      }

      :host ::ng-deep .permission-toggle .p-checkbox {
        width: 1.45rem;
        height: 1.45rem;
      }

      :host ::ng-deep .permission-toggle .p-checkbox-box {
        width: 1.45rem;
        height: 1.45rem;
        border-radius: 999px;
        transition:
          border-color 0.18s ease,
          box-shadow 0.18s ease,
          background-color 0.18s ease;
      }

      :host ::ng-deep .permission-toggle .p-checkbox-icon {
        font-size: 0.72rem;
      }

      :host ::ng-deep .permission-toggle--pending .p-checkbox-box {
        border-color: rgba(245, 158, 11, 0.55);
        box-shadow: 0 0 0 2px rgba(251, 191, 36, 0.14);
      }

      :host ::ng-deep .permission-matrix-table .p-datatable-header {
        border: 0;
      }

      :host ::ng-deep .permission-matrix-table .p-datatable-table-container {
        border-radius: 0 0 1rem 1rem;
      }

      :host ::ng-deep .permission-matrix-table .p-datatable-thead > tr > th {
        padding: 0.85rem 1rem;
        border-width: 0 0 1px;
        border-color: rgba(226, 232, 240, 0.9);
        background: transparent;
        color: #64748b;
        font-size: 0.77rem;
        font-weight: 700;
        letter-spacing: 0.08em;
        text-transform: uppercase;
      }

      :host ::ng-deep .permission-matrix-table .p-datatable-tbody > tr > td {
        padding: 0.8rem 1rem;
        border-width: 0 0 1px;
        border-color: rgba(226, 232, 240, 0.75);
        background: rgba(255, 255, 255, 0.86);
        transition:
          background-color 0.18s ease,
          box-shadow 0.18s ease;
      }

      :host ::ng-deep .permission-matrix-table .p-datatable-tbody > tr:last-child > td {
        border-bottom: 0;
      }

      :host ::ng-deep .permission-matrix-table .p-datatable-tbody > tr.permission-row--clickable:hover > td {
        background: rgba(239, 246, 255, 0.92);
      }

      :host ::ng-deep .permission-matrix-table .p-datatable-tbody > tr.permission-row--selected > td {
        background: rgba(14, 165, 233, 0.08);
      }

      :host ::ng-deep .permission-matrix-table .p-datatable-tbody > tr.permission-row--active > td:first-child {
        box-shadow: inset 3px 0 0 rgba(16, 185, 129, 0.85);
      }

      :host ::ng-deep .permission-matrix-table .p-datatable-tbody > tr.permission-row--selected > td:first-child {
        box-shadow: inset 3px 0 0 var(--p-primary-color);
      }

      :host ::ng-deep .permission-matrix-table .p-datatable-tbody > tr.permission-row--wildcard > td {
        background: rgba(15, 23, 42, 0.035);
      }

      .implied-icon {
        width: 1.55rem;
        height: 1.55rem;
        border: 1px solid rgba(13, 148, 136, 0.24);
        border-radius: 999px;
        background: rgba(20, 184, 166, 0.14);
        color: #0f766e;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        font-size: 0.78rem;
        transition:
          background-color 0.18s ease,
          border-color 0.18s ease;
      }

      .permission-toggle--pending .implied-icon {
        border-color: rgba(245, 158, 11, 0.55);
        box-shadow: 0 0 0 2px rgba(251, 191, 36, 0.14);
      }

      .implied-icon:not(:disabled):hover {
        border-color: rgba(13, 148, 136, 0.38);
        background: rgba(20, 184, 166, 0.2);
      }

      .implied-icon:disabled {
        opacity: 0.75;
        cursor: not-allowed;
      }

      .implied-icon--readonly {
        border-color: rgba(148, 163, 184, 0.22);
        background: rgba(148, 163, 184, 0.14);
        color: #64748b;
      }
    `,
  ],
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
  catalogEntriesWithWildcard: ISecCatalogEntry[] = [];

  granted = new Map<string, number>();
  pendingChanges = new Map<string, PendingChange>();
  saving = false;

  // Menu access tab state
  availableMenuApps: string[] = [];
  menuAppOptions: Array<{ label: string; value: string }> = [];
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
    this.initializeComputedCollections();

    this.translateService.onLangChange.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.onLanguageChanged();
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
          this.onPermissionDataLoaded(catalogEntries, permissions);
        },
        error: (err: unknown) =>
          handleHttpError(
            this.messageService,
            this.translateService,
            err,
            'feedback.security.permissionMatrix.loadFailed',
          ),
      });

    this.loadMenuPermissions();
  }

  private onLanguageChanged(): void {
    if (this.selectedEntity) {
      this.selectedEntityAttributeRows = this.buildAttributeRows(this.selectedEntity);
    }
    this.initializeComputedCollections();
    this.buildMenuTree();
    this.cdr.detectChanges();
  }

  private initializeComputedCollections(): void {
    this.catalogEntriesWithWildcard = this.buildCatalogEntriesWithWildcard();
    this.updateMenuAppOptions();
  }

  private buildCatalogEntriesWithWildcard(): ISecCatalogEntry[] {
    const wildcardEntry: ISecCatalogEntry = {
      code: '*',
      displayName: this.translateService.instant('security.permissionMatrix.entity.wildcard'),
      operations: ['CREATE', 'READ', 'UPDATE', 'DELETE'],
      attributes: [],
    };
    return [wildcardEntry, ...this.catalogEntries];
  }

  private onPermissionDataLoaded(catalogEntries: ISecCatalogEntry[], permissions: ISecPermission[]): void {
    this.catalogEntries = catalogEntries;
    this.catalogEntriesWithWildcard = this.buildCatalogEntriesWithWildcard();
    this.granted.clear();
    permissions.forEach((permission) => {
      if (permission.id !== undefined) {
        this.granted.set(this.permissionKey(permission.target, permission.action), permission.id);
      }
    });
  }

  private loadMenuPermissions(): void {
    this.menuLoading = true;
    forkJoin({
      permissions: this.menuPermissionService.query(this.roleName),
      definitions: this.menuDefinitionService.queryAll().pipe(
        map((response) => response.body ?? []),
        catchError(() => of([])),
      ),
    }).subscribe({
      next: ({ permissions, definitions }) => {
        this.onMenuPermissionsLoaded(permissions, definitions);
      },
      error: (err: unknown) => {
        this.onMenuLoadError(err);
      },
    });
  }

  private onMenuPermissionsLoaded(
    permissions: ISecMenuPermissionAdmin[],
    definitions: ISecMenuDefinition[],
  ): void {
    this.menuGranted.clear();
    permissions.forEach((p) => {
      if (p.id !== undefined) {
        this.menuGranted.set(this.menuPermissionKey(p.appName, p.menuId), p.id);
      }
    });
    this.availableMenuApps = this.resolveAvailableMenuApps(permissions, definitions);
    this.updateMenuAppOptions();
    const nextSelectedApp = this.availableMenuApps.includes(this.selectedMenuApp)
      ? this.selectedMenuApp
      : (this.availableMenuApps[0] ?? this.defaultMenuApp);
    this.loadMenuDefinitions(nextSelectedApp);
  }

  private onMenuLoadError(err: unknown): void {
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
    this.availableMenuApps = [this.defaultMenuApp];
    this.updateMenuAppOptions();
    this.cdr.detectChanges();
  }

  private updateMenuAppOptions(): void {
    this.menuAppOptions = this.availableMenuApps.map((appName) => ({
      label: this.currentMenuAppLabel(appName),
      value: appName,
    }));
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

  isPendingChange(target: string, action: string): boolean {
    return this.pendingChanges.has(this.permissionKey(target, action));
  }

  /**
   * Returns true when the permission is granted only because the wildcard covers it (not explicitly
   * granted on its own). Used to render the Jmix-style dash icon on implied cells.
   */
  isImpliedByEntityWildcard(entityCode: string, action: string): boolean {
    if (entityCode === '*') return false;
    return this.isEntityWildcardEffectivelyGranted(action) && !this.isEffectivelyGranted(entityCode, action);
  }

  isImpliedByAttributeWildcard(target: string, action: string, entityCode: string): boolean {
    if (target === `${entityCode}.*`) return false;
    return this.isWildcardEffectivelyGranted(entityCode, action) && !this.isEffectivelyGranted(target, action);
  }

  /** True when any CRUD op is effectively granted for the entity row. */
  isEntityRowGranted(entityCode: string): boolean {
    if (entityCode === '*') {
      return ['CREATE', 'READ', 'UPDATE', 'DELETE'].some((op) => this.isEffectivelyGranted('*', op));
    }
    return ['CREATE', 'READ', 'UPDATE', 'DELETE'].some((op) => this.isEntityEffectivelyGranted(entityCode, op));
  }

  /** True when VIEW or EDIT is effectively granted for the attribute row. */
  isAttributeRowGranted(target: string, entityCode: string): boolean {
    const viewGranted =
      this.isAttributeEffectivelyGranted(target, 'VIEW', entityCode) ||
      this.isViewImpliedByModify(target, entityCode);
    const editGranted = this.isAttributeEffectivelyGranted(target, 'EDIT', entityCode);
    return viewGranted || editGranted;
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
    if (targetType === 'ENTITY') {
      this.toggleEntityPermission(target, action, checked);
    } else if (targetType === 'ATTRIBUTE') {
      this.toggleAttributePermission(target, action, checked);
    } else {
      this.applySimpleToggle(targetType, target, action, checked);
    }
    this.cdr.detectChanges();
  }

  private applySimpleToggle(targetType: string, target: string, action: string, checked: boolean): void {
    const key = this.permissionKey(target, action);
    if (checked === this.isGranted(target, action)) {
      this.pendingChanges.delete(key);
    } else {
      this.pendingChanges.set(key, { checked, targetType, target, action });
    }
  }

  private toggleEntityPermission(target: string, action: string, checked: boolean): void {
    if (target === '*') {
      this.applySimpleToggle('ENTITY', '*', action, checked);
      if (checked) {
        // wildcard turned ON: mark all granted specifics as pending-delete; remove any pending-adds
        for (const entry of this.catalogEntries) {
          const key = this.permissionKey(entry.code, action);
          const pending = this.pendingChanges.get(key);
          if (pending?.checked) {
            this.pendingChanges.delete(key);
          } else if (!pending && this.granted.has(key)) {
            this.pendingChanges.set(key, { checked: false, targetType: 'ENTITY', target: entry.code, action });
          }
        }
      } else {
        // wildcard turned OFF: remove auto-generated pending-deletes for specifics
        for (const entry of this.catalogEntries) {
          const key = this.permissionKey(entry.code, action);
          const pending = this.pendingChanges.get(key);
          if (pending && !pending.checked && !this.granted.has(key)) {
            // it was never in DB — only pending-delete due to wildcard expansion; remove it
            this.pendingChanges.delete(key);
          }
        }
      }
    } else {
      // Specific entity toggled
      const wildcardEffective = this.isEntityWildcardEffectivelyGranted(action);
      if (!checked && wildcardEffective) {
        // Unticking a specific while wildcard is active: expand wildcard into individual permissions
        // 1. Remove wildcard (or mark wildcard as pending-delete)
        this.applySimpleToggle('ENTITY', '*', action, false);
        // 2. Remove auto-generated pending-deletes from any prior wildcard-on that are still there
        //    and add pending-add for all OTHER specifics that are not already in DB
        for (const entry of this.catalogEntries) {
          if (entry.code === target) {
            // This one is being unticked — ensure it's marked unchecked (not granted by wildcard anymore)
            const key = this.permissionKey(entry.code, action);
            if (this.granted.has(key)) {
              this.pendingChanges.set(key, { checked: false, targetType: 'ENTITY', target: entry.code, action });
            } else {
              this.pendingChanges.delete(key);
            }
          } else {
            // All others should remain checked — add pending-add only if not already in DB
            const key = this.permissionKey(entry.code, action);
            if (!this.granted.has(key)) {
              this.pendingChanges.set(key, { checked: true, targetType: 'ENTITY', target: entry.code, action });
            } else {
              // Already in DB — remove any pending-delete that may have been set
              this.pendingChanges.delete(key);
            }
          }
        }
      } else {
        this.applySimpleToggle('ENTITY', target, action, checked);
        if (checked) {
          this.checkAutoPromoteEntity(action);
        }
      }
    }
  }

  private checkAutoPromoteEntity(action: string): void {
    const allGranted = this.catalogEntries.every((entry) =>
      this.isEffectivelyGranted(entry.code, action),
    );
    if (!allGranted) return;
    // All specifics are now checked — promote to wildcard
    this.applySimpleToggle('ENTITY', '*', action, true);
    // Mark all specifically-granted entries as pending-delete (wildcard supersedes)
    for (const entry of this.catalogEntries) {
      const key = this.permissionKey(entry.code, action);
      const pending = this.pendingChanges.get(key);
      if (pending?.checked) {
        // Was pending-add, no longer needed
        this.pendingChanges.delete(key);
      } else if (!pending && this.granted.has(key)) {
        this.pendingChanges.set(key, { checked: false, targetType: 'ENTITY', target: entry.code, action });
      }
    }
  }

  private toggleAttributePermission(target: string, action: string, checked: boolean): void {
    const entity = this.selectedEntity;
    if (!entity) {
      this.applySimpleToggle('ATTRIBUTE', target, action, checked);
      return;
    }
    const wildcardTarget = `${entity.code}.*`;
    if (target === wildcardTarget) {
      this.applySimpleToggle('ATTRIBUTE', wildcardTarget, action, checked);
      if (checked) {
        // wildcard turned ON: mark all granted specifics as pending-delete; remove pending-adds
        for (const attr of entity.attributes) {
          const specificTarget = `${entity.code}.${attr}`;
          const key = this.permissionKey(specificTarget, action);
          const pending = this.pendingChanges.get(key);
          if (pending?.checked) {
            this.pendingChanges.delete(key);
          } else if (!pending && this.granted.has(key)) {
            this.pendingChanges.set(key, { checked: false, targetType: 'ATTRIBUTE', target: specificTarget, action });
          }
        }
      } else {
        // wildcard turned OFF: remove auto-generated pending-deletes for specifics
        for (const attr of entity.attributes) {
          const specificTarget = `${entity.code}.${attr}`;
          const key = this.permissionKey(specificTarget, action);
          const pending = this.pendingChanges.get(key);
          if (pending && !pending.checked && !this.granted.has(key)) {
            this.pendingChanges.delete(key);
          }
        }
      }
    } else {
      // Specific attribute toggled
      const wildcardEffective = this.isWildcardEffectivelyGranted(entity.code, action);
      if (!checked && wildcardEffective) {
        // Unticking a specific while wildcard is active
        this.applySimpleToggle('ATTRIBUTE', wildcardTarget, action, false);
        for (const attr of entity.attributes) {
          const specificTarget = `${entity.code}.${attr}`;
          if (specificTarget === target) {
            const key = this.permissionKey(specificTarget, action);
            if (this.granted.has(key)) {
              this.pendingChanges.set(key, { checked: false, targetType: 'ATTRIBUTE', target: specificTarget, action });
            } else {
              this.pendingChanges.delete(key);
            }
          } else {
            const key = this.permissionKey(specificTarget, action);
            if (!this.granted.has(key)) {
              this.pendingChanges.set(key, { checked: true, targetType: 'ATTRIBUTE', target: specificTarget, action });
            } else {
              this.pendingChanges.delete(key);
            }
          }
        }
      } else {
        this.applySimpleToggle('ATTRIBUTE', target, action, checked);
        if (checked) {
          this.checkAutoPromoteAttribute(entity.code, action);
        }
      }
    }
  }

  private checkAutoPromoteAttribute(entityCode: string, action: string): void {
    const entity = this.catalogEntries.find((e) => e.code === entityCode);
    if (!entity || entity.attributes.length === 0) return;
    const allGranted = entity.attributes.every((attr) =>
      this.isEffectivelyGranted(`${entityCode}.${attr}`, action),
    );
    if (!allGranted) return;
    const wildcardTarget = `${entityCode}.*`;
    this.applySimpleToggle('ATTRIBUTE', wildcardTarget, action, true);
    for (const attr of entity.attributes) {
      const specificTarget = `${entityCode}.${attr}`;
      const key = this.permissionKey(specificTarget, action);
      const pending = this.pendingChanges.get(key);
      if (pending?.checked) {
        this.pendingChanges.delete(key);
      } else if (!pending && this.granted.has(key)) {
        this.pendingChanges.set(key, { checked: false, targetType: 'ATTRIBUTE', target: specificTarget, action });
      }
    }
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
