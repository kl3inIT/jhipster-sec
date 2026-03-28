import { HttpResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { NEVER, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { SecMenuDefinitionService } from '../menu-definitions/service/sec-menu-definition.service';
import { AdminMenuPermissionService } from '../shared/service/admin-menu-permission.service';
import { ISecCatalogEntry } from '../shared/sec-catalog.model';
import { ISecPermission } from '../shared/sec-permission.model';
import { SecCatalogService } from '../shared/service/sec-catalog.service';
import { SecPermissionService } from '../shared/service/sec-permission.service';
import PermissionMatrixComponent from './permission-matrix.component';

const CATALOG_ENTRIES: ISecCatalogEntry[] = [
  {
    code: 'organization',
    displayName: 'Organization',
    operations: ['CREATE', 'READ', 'UPDATE', 'DELETE'],
    attributes: ['budget', 'name'],
  },
  {
    code: 'department',
    displayName: 'Department',
    operations: ['CREATE', 'READ', 'UPDATE', 'DELETE'],
    attributes: [],
  },
];

const MENU_DEFINITIONS_BY_APP = {
  'jhipster-security-platform': [
    {
      id: 1,
      menuId: 'platform-root',
      appName: 'jhipster-security-platform',
      menuName: 'platform-root',
      label: 'Platform Root',
      ordering: 0,
    },
    {
      id: 2,
      menuId: 'shared-menu',
      appName: 'jhipster-security-platform',
      menuName: 'shared-menu',
      label: 'Platform Shared Menu',
      parentMenuId: 'platform-root',
      ordering: 0,
    },
    {
      id: 3,
      menuId: 'platform-reports',
      appName: 'jhipster-security-platform',
      menuName: 'platform-reports',
      label: 'Platform Reports',
      parentMenuId: 'platform-root',
      ordering: 1,
    },
  ],
  'sales-console': [
    {
      id: 4,
      menuId: 'sales-root',
      appName: 'sales-console',
      menuName: 'sales-root',
      label: 'Sales Root',
      ordering: 0,
    },
    {
      id: 5,
      menuId: 'shared-menu',
      appName: 'sales-console',
      menuName: 'shared-menu',
      label: 'Sales Shared Menu',
      parentMenuId: 'sales-root',
      ordering: 0,
    },
    {
      id: 6,
      menuId: 'sales-reports',
      appName: 'sales-console',
      menuName: 'sales-reports',
      label: 'Sales Reports',
      parentMenuId: 'sales-root',
      ordering: 1,
    },
  ],
} as const;

const MULTI_APP_MENU_PERMISSIONS = [
  {
    id: 11,
    role: 'ROLE_PROOF_NONE',
    appName: 'jhipster-security-platform',
    menuId: 'shared-menu',
    effect: 'ALLOW',
  },
  {
    id: 22,
    role: 'ROLE_PROOF_NONE',
    appName: 'sales-console',
    menuId: 'shared-menu',
    effect: 'ALLOW',
  },
];

describe('PermissionMatrixComponent', () => {
  let permissionService: {
    query: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };
  let menuPermissionService: {
    query: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };
  let menuDefinitionService: {
    query: ReturnType<typeof vi.fn>;
    queryAll: ReturnType<typeof vi.fn>;
  };

  function createFixture(): ComponentFixture<PermissionMatrixComponent> {
    const fixture = TestBed.createComponent(PermissionMatrixComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    globalThis.ResizeObserver =
      globalThis.ResizeObserver ??
      class ResizeObserver {
        observe(): void {}
        unobserve(): void {}
        disconnect(): void {}
      };

    permissionService = {
      query: vi.fn().mockReturnValue(of([])),
      create: vi.fn().mockReturnValue(of(new HttpResponse({ body: { id: 123 } }))),
      delete: vi.fn().mockReturnValue(of(new HttpResponse({ status: 204 }))),
    };
    menuPermissionService = {
      query: vi.fn().mockReturnValue(of([])),
      create: vi.fn().mockReturnValue(of(new HttpResponse({ body: { id: 456 } }))),
      delete: vi.fn().mockReturnValue(of(new HttpResponse({ status: 204 }))),
    };
    menuDefinitionService = {
      query: vi
        .fn()
        .mockImplementation((appName: keyof typeof MENU_DEFINITIONS_BY_APP) =>
          of(new HttpResponse({ body: MENU_DEFINITIONS_BY_APP[appName] ?? [] })),
        ),
      queryAll: vi
        .fn()
        .mockReturnValue(
          of(new HttpResponse({ body: Object.values(MENU_DEFINITIONS_BY_APP).flat() })),
        ),
    };

    TestBed.configureTestingModule({
      imports: [PermissionMatrixComponent],
      providers: [
        provideTranslateService({ lang: 'en', fallbackLang: 'en' }),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ name: 'ROLE_PROOF_NONE' }),
            },
          },
        },
        {
          provide: SecCatalogService,
          useValue: {
            query: () => of(CATALOG_ENTRIES),
          },
        },
        {
          provide: SecPermissionService,
          useValue: permissionService,
        },
        {
          provide: AdminMenuPermissionService,
          useValue: menuPermissionService,
        },
        {
          provide: SecMenuDefinitionService,
          useValue: menuDefinitionService,
        },
      ],
    });

    TestBed.inject(TranslateService).setTranslation('en', {
      security: {
        permissionMatrix: {
          menu: {
            appLabel: 'Application',
            emptyDefinitions: 'No menu definitions found for {{ appName }}.',
            noApps: 'No applications available.',
            node: 'Navigation Node',
          },
          entity: {
            emptySelection: 'Select an entity above',
          },
          attribute: {
            heading: 'Attribute Permissions: {{ entity }}',
            none: 'This entity has no enumerated attributes.',
            wildcard: 'All attributes (*)',
          },
        },
      },
    });
  });

  it('loads catalog entries and clears loading flag on init', () => {
    const fixture = createFixture();
    const component = fixture.componentInstance;

    expect(component.loading).toBe(false);
    expect(component.catalogEntries.length).toBe(2);
    expect(component.roleName).toBe('ROLE_PROOF_NONE');
  });

  it('populates the granted map from the permissions response', () => {
    permissionService.query.mockReturnValue(
      of<ISecPermission[]>([
        {
          id: 10,
          authorityName: 'ROLE_PROOF_NONE',
          targetType: 'ENTITY',
          target: 'organization',
          action: 'READ',
          effect: 'GRANT',
        },
        {
          id: 20,
          authorityName: 'ROLE_PROOF_NONE',
          targetType: 'ENTITY',
          target: 'organization',
          action: 'UPDATE',
          effect: 'GRANT',
        },
      ]),
    );

    const fixture = createFixture();
    const component = fixture.componentInstance;

    expect(component.isGranted('organization', 'READ')).toBe(true);
    expect(component.isGranted('organization', 'UPDATE')).toBe(true);
    expect(component.isGranted('organization', 'DELETE')).toBe(false);
  });

  it('loads explicit attribute grants from the permissions response', () => {
    permissionService.query.mockReturnValue(
      of<ISecPermission[]>([
        {
          id: 30,
          authorityName: 'ROLE_PROOF_NONE',
          targetType: 'ATTRIBUTE',
          target: 'department.costCenter',
          action: 'VIEW',
          effect: 'GRANT',
        },
        {
          id: 31,
          authorityName: 'ROLE_PROOF_NONE',
          targetType: 'ATTRIBUTE',
          target: 'organization.name',
          action: 'VIEW',
          effect: 'GRANT',
        },
      ]),
    );

    const fixture = createFixture();
    const component = fixture.componentInstance;

    expect(component.isGranted('department.costCenter', 'VIEW')).toBe(true);
    expect(component.isGranted('organization.name', 'VIEW')).toBe(true);
  });

  it('onEntitySelect sets the selected entity and attribute rows', () => {
    const fixture = createFixture();
    const component = fixture.componentInstance;

    expect(component.selectedEntity).toBeNull();
    component.onEntitySelect(CATALOG_ENTRIES[0]);

    expect(component.selectedEntity).toBe(CATALOG_ENTRIES[0]);
    expect(component.selectedEntityAttributeRows).toEqual([
      { label: '', target: 'organization.*', isWildcard: true },
      { label: 'budget', target: 'organization.budget', isWildcard: false },
      { label: 'name', target: 'organization.name', isWildcard: false },
    ]);
  });

  it('renders the attribute panel after selecting an entity', () => {
    const fixture = createFixture();
    const component = fixture.componentInstance;

    expect(fixture.debugElement.nativeElement.textContent).toContain('Select an entity above');

    component.onEntitySelect(CATALOG_ENTRIES[0]);
    fixture.detectChanges();

    const headings = fixture.debugElement.queryAll(By.css('h2'));
    const attributeHeading = headings.find((heading) =>
      heading.nativeElement.textContent.includes('Attribute Permissions'),
    );

    expect(attributeHeading).toBeDefined();
    expect(attributeHeading!.nativeElement.textContent).toContain('Organization');
  });

  it('shows the no attributes message for entities without attributes', () => {
    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.onEntitySelect(CATALOG_ENTRIES[1]);

    expect(fixture.debugElement.nativeElement.textContent).toContain('no enumerated attributes');
  });

  it('stages a new permission change without calling the API immediately', () => {
    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.togglePermission('ENTITY', 'organization', 'READ', true);

    expect(permissionService.create).not.toHaveBeenCalled();
    expect(component.pendingChanges.get('organization:READ')).toEqual({
      checked: true,
      targetType: 'ENTITY',
      target: 'organization',
      action: 'READ',
    });
    expect(component.hasPendingChanges).toBe(true);
  });

  it('flushes a staged create and stores the returned permission id', () => {
    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.togglePermission('ENTITY', 'organization', 'CREATE', true);
    component.flushChanges();

    expect(permissionService.create).toHaveBeenCalledWith({
      authorityName: 'ROLE_PROOF_NONE',
      targetType: 'ENTITY',
      target: 'organization',
      action: 'CREATE',
      effect: 'GRANT',
    });
    expect(component.granted.get('organization:CREATE')).toBe(123);
    expect(component.pendingChanges.has('organization:CREATE')).toBe(false);
    expect(component.saving).toBe(false);
  });

  it('keeps the pending create staged when save fails', () => {
    permissionService.create.mockReturnValue(throwError(() => new Error('network error')));
    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.togglePermission('ENTITY', 'organization', 'CREATE', true);
    component.flushChanges();

    expect(component.isGranted('organization', 'CREATE')).toBe(false);
    expect(component.pendingChanges.has('organization:CREATE')).toBe(true);
    expect(component.saving).toBe(false);
  });

  it('flushes a staged delete and removes the granted permission', () => {
    permissionService.query.mockReturnValue(
      of<ISecPermission[]>([
        {
          id: 42,
          authorityName: 'ROLE_PROOF_NONE',
          targetType: 'ENTITY',
          target: 'organization',
          action: 'DELETE',
          effect: 'GRANT',
        },
      ]),
    );

    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.togglePermission('ENTITY', 'organization', 'DELETE', false);
    component.flushChanges();

    expect(permissionService.delete).toHaveBeenCalledWith(42);
    expect(component.isGranted('organization', 'DELETE')).toBe(false);
    expect(component.pendingChanges.has('organization:DELETE')).toBe(false);
  });

  it('keeps the original grant and pending delete when delete fails', () => {
    permissionService.query.mockReturnValue(
      of<ISecPermission[]>([
        {
          id: 77,
          authorityName: 'ROLE_PROOF_NONE',
          targetType: 'ENTITY',
          target: 'organization',
          action: 'UPDATE',
          effect: 'GRANT',
        },
      ]),
    );
    permissionService.delete.mockReturnValue(throwError(() => new Error('network error')));

    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.togglePermission('ENTITY', 'organization', 'UPDATE', false);
    component.flushChanges();

    expect(component.isGranted('organization', 'UPDATE')).toBe(true);
    expect(component.pendingChanges.has('organization:UPDATE')).toBe(true);
    expect(component.granted.get('organization:UPDATE')).toBe(77);
  });

  it('discards staged changes without calling the API', () => {
    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.togglePermission('ENTITY', 'organization', 'READ', true);
    component.discardChanges();

    expect(component.hasPendingChanges).toBe(false);
    expect(permissionService.create).not.toHaveBeenCalled();
    expect(permissionService.delete).not.toHaveBeenCalled();
  });

  it('ignores a second save while a previous save is still pending', () => {
    permissionService.create.mockReturnValue(NEVER);
    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.togglePermission('ENTITY', 'organization', 'READ', true);
    component.flushChanges();
    component.flushChanges();

    expect(permissionService.create).toHaveBeenCalledTimes(1);
    expect(component.saving).toBe(true);
  });

  it('treats attribute permissions as wildcard-granted when the wildcard is already granted', () => {
    permissionService.query.mockReturnValue(
      of<ISecPermission[]>([
        {
          id: 99,
          authorityName: 'ROLE_PROOF_NONE',
          targetType: 'ATTRIBUTE',
          target: 'organization.*',
          action: 'VIEW',
          effect: 'GRANT',
        },
      ]),
    );

    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.onEntitySelect(CATALOG_ENTRIES[0]);

    expect(
      component.selectedEntityAttributeRows.find((row) => row.target === 'organization.budget'),
    ).toEqual({
      label: 'budget',
      target: 'organization.budget',
      isWildcard: false,
    });
    expect(component.isWildcardEffectivelyGranted('organization', 'VIEW')).toBe(true);
  });

  it('treats child attribute rows as checked when a wildcard grant is already stored', () => {
    permissionService.query.mockReturnValue(
      of<ISecPermission[]>([
        {
          id: 99,
          authorityName: 'ROLE_PROOF_NONE',
          targetType: 'ATTRIBUTE',
          target: 'organization.*',
          action: 'EDIT',
          effect: 'GRANT',
        },
      ]),
    );

    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.onEntitySelect(CATALOG_ENTRIES[0]);

    expect(component.isEffectivelyGranted('organization.name', 'EDIT')).toBe(false);
    expect(
      component.isAttributeEffectivelyGranted('organization.name', 'EDIT', 'organization'),
    ).toBe(true);
    expect(
      component.isAttributeEffectivelyGranted('organization.budget', 'EDIT', 'organization'),
    ).toBe(true);
  });

  it('loadsMenuPermissionsAcrossApps', () => {
    menuPermissionService.query.mockReturnValue(of(MULTI_APP_MENU_PERMISSIONS));

    const fixture = createFixture();
    const component = fixture.componentInstance;

    expect(menuPermissionService.query).toHaveBeenCalledWith('ROLE_PROOF_NONE');
    expect(menuDefinitionService.queryAll).toHaveBeenCalled();
    expect(component.availableMenuApps).toEqual(['jhipster-security-platform', 'sales-console']);
    expect(component.selectedMenuApp).toBe('jhipster-security-platform');
    expect(component.menuGranted.get('jhipster-security-platform::shared-menu')).toBe(11);
    expect(component.menuGranted.get('sales-console::shared-menu')).toBe(22);
    expect(menuDefinitionService.query).toHaveBeenCalledWith('jhipster-security-platform');
  });

  it('switchingSelectedMenuAppRebuildsTheTreeFromBackendDefinitions', () => {
    menuPermissionService.query.mockReturnValue(of(MULTI_APP_MENU_PERMISSIONS));

    const fixture = createFixture();
    const component = fixture.componentInstance;

    expect(component.menuTreeNodes[0]?.key).toBe('jhipster-security-platform::platform-root');

    component.onSelectedMenuAppChange('sales-console');
    fixture.detectChanges();

    expect(menuDefinitionService.query).toHaveBeenLastCalledWith('sales-console');
    expect(component.menuTreeNodes[0]?.key).toBe('sales-console::sales-root');
    expect(component.menuTreeNodes[0]?.children?.map((child) => child.key)).toEqual([
      'sales-console::shared-menu',
      'sales-console::sales-reports',
    ]);
  });

  it('savingMenuGrantUsesTheSelectedAppName', () => {
    menuPermissionService.query.mockReturnValue(of(MULTI_APP_MENU_PERMISSIONS));

    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.onSelectedMenuAppChange('sales-console');
    component.onMenuSelectionKeysChange({
      'sales-console::shared-menu': { checked: true },
      'sales-console::sales-reports': { checked: true },
    });
    component.flushChanges();

    expect(menuPermissionService.create).toHaveBeenCalledWith({
      role: 'ROLE_PROOF_NONE',
      appName: 'sales-console',
      menuId: 'sales-reports',
      effect: 'ALLOW',
    });
    expect(component.menuGranted.get('sales-console::sales-reports')).toBe(456);
    expect(component.menuPendingChanges.has('sales-console::sales-reports')).toBe(false);
  });

  it('loadsSelectableAppsFromMenuDefinitionsEvenWithoutExistingGrants', () => {
    menuPermissionService.query.mockReturnValue(
      of([
        {
          id: 11,
          role: 'ROLE_PROOF_NONE',
          appName: 'jhipster-security-platform',
          menuId: 'shared-menu',
          effect: 'ALLOW',
        },
      ]),
    );

    const fixture = createFixture();
    const component = fixture.componentInstance;

    expect(component.availableMenuApps).toEqual(['jhipster-security-platform', 'sales-console']);
    component.onSelectedMenuAppChange('sales-console');
    component.onMenuSelectionKeysChange({
      'sales-console::sales-reports': { checked: true },
    });
    component.flushChanges();

    expect(menuPermissionService.create).toHaveBeenCalledWith({
      role: 'ROLE_PROOF_NONE',
      appName: 'sales-console',
      menuId: 'sales-reports',
      effect: 'ALLOW',
    });
    expect(component.menuGranted.get('sales-console::sales-reports')).toBe(456);
  });

  it('uncheckedGrantDeletesOnlyTheSelectedAppsPermissionId', () => {
    menuPermissionService.query.mockReturnValue(of(MULTI_APP_MENU_PERMISSIONS));

    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.onSelectedMenuAppChange('sales-console');
    component.onMenuSelectionKeysChange({});
    component.flushChanges();

    expect(menuPermissionService.delete).toHaveBeenCalledTimes(1);
    expect(menuPermissionService.delete).toHaveBeenCalledWith(22);
    expect(menuPermissionService.delete).not.toHaveBeenCalledWith(11);
    expect(component.menuGranted.has('jhipster-security-platform::shared-menu')).toBe(true);
    expect(component.menuGranted.has('sales-console::shared-menu')).toBe(false);
  });

  it('does not call the API when roleName is missing from the route', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [PermissionMatrixComponent],
      providers: [
        provideTranslateService({ lang: 'en', fallbackLang: 'en' }),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({}) } },
        },
        { provide: SecCatalogService, useValue: { query: () => of([]) } },
        { provide: SecPermissionService, useValue: permissionService },
        { provide: AdminMenuPermissionService, useValue: menuPermissionService },
        { provide: SecMenuDefinitionService, useValue: menuDefinitionService },
      ],
    });

    const fixture = TestBed.createComponent(PermissionMatrixComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.loading).toBe(false);
    expect(permissionService.query).not.toHaveBeenCalled();
  });
});
