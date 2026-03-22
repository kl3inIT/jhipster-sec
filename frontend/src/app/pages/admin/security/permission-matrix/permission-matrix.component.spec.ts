import { HttpResponse } from '@angular/common/http';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { vi } from 'vitest';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { NEVER, of, throwError } from 'rxjs';

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

describe('PermissionMatrixComponent', () => {
  let permissionService: {
    query: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };

  function createFixture(): ComponentFixture<PermissionMatrixComponent> {
    const fixture = TestBed.createComponent(PermissionMatrixComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    permissionService = {
      query: vi.fn().mockReturnValue(of([])),
      create: vi.fn().mockReturnValue(of(new HttpResponse({ body: { id: 123 } }))),
      delete: vi.fn().mockReturnValue(of(new HttpResponse({ status: 204 }))),
    };

    TestBed.configureTestingModule({
      imports: [PermissionMatrixComponent],
      providers: [
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
      ],
    });
  });

  // ─── Initialization ───────────────────────────────────────────────────────

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
        { id: 10, authorityName: 'ROLE_PROOF_NONE', targetType: 'ENTITY', target: 'organization', action: 'READ', effect: 'GRANT' },
        { id: 20, authorityName: 'ROLE_PROOF_NONE', targetType: 'ENTITY', target: 'organization', action: 'UPDATE', effect: 'GRANT' },
      ]),
    );

    const fixture = createFixture();
    const component = fixture.componentInstance;

    expect(component.isGranted('organization', 'READ')).toBe(true);
    expect(component.isGranted('organization', 'UPDATE')).toBe(true);
    expect(component.isGranted('organization', 'DELETE')).toBe(false);
  });

  // ─── Entity row selection ─────────────────────────────────────────────────

  it('onEntitySelect sets selectedEntity', () => {
    const fixture = createFixture();
    const component = fixture.componentInstance;

    expect(component.selectedEntity).toBeNull();
    component.onEntitySelect(CATALOG_ENTRIES[0]);

    expect(component.selectedEntity).toBe(CATALOG_ENTRIES[0]);
  });

  it('onEntitySelect triggers detectChanges so the attribute panel is visible in the DOM', () => {
    const fixture = createFixture();
    const component = fixture.componentInstance;

    // Before selection: "Select an entity" placeholder text is shown
    const placeholder = fixture.debugElement.nativeElement.textContent;
    expect(placeholder).toContain('Select an entity above');

    // Select an entity with attributes
    component.onEntitySelect(CATALOG_ENTRIES[0]);
    // onEntitySelect calls cdr.detectChanges() internally — no manual detectChanges() needed here

    const headings = fixture.debugElement.queryAll(By.css('h2'));
    const attrHeading = headings.find(h => h.nativeElement.textContent.includes('Attribute Permissions'));
    // The attribute-panel heading must be rendered
    expect(attrHeading).toBeDefined();
    expect(attrHeading!.nativeElement.textContent).toContain('Organization');
  });

  it('onEntitySelect for entity with no attributes shows "no enumerated attributes" message', () => {
    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.onEntitySelect(CATALOG_ENTRIES[1]); // department — attributes: []
    const text = fixture.debugElement.nativeElement.textContent;
    expect(text).toContain('no enumerated attributes');
  });

  // ─── togglePermission — GRANT path ───────────────────────────────────────

  it('calls create with the locked GRANT UI contract for entity READ toggles', () => {
    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.togglePermission('ENTITY', 'organization', 'READ', true);

    expect(permissionService.create).toHaveBeenCalledWith({
      authorityName: 'ROLE_PROOF_NONE',
      targetType: 'ENTITY',
      target: 'organization',
      action: 'READ',
      effect: 'GRANT',
    });
  });

  it('stores the returned permission id in the granted map after create', () => {
    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.togglePermission('ENTITY', 'organization', 'CREATE', true);

    expect(component.isGranted('organization', 'CREATE')).toBe(true);
    // Optimistic id (-1) is replaced with the real id from the response (123)
    expect(component['granted'].get('organization:CREATE')).toBe(123);
  });

  it('clears the granted key on create error (rollback)', () => {
    permissionService.create.mockReturnValue(throwError(() => new Error('network error')));
    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.togglePermission('ENTITY', 'organization', 'CREATE', true);

    expect(component.isGranted('organization', 'CREATE')).toBe(false);
    expect(component.isPending('organization', 'CREATE')).toBe(false);
  });

  // ─── togglePermission — REVOKE path ──────────────────────────────────────

  it('calls delete with the correct permission id when unchecking a granted permission', () => {
    permissionService.query.mockReturnValue(
      of<ISecPermission[]>([
        { id: 42, authorityName: 'ROLE_PROOF_NONE', targetType: 'ENTITY', target: 'organization', action: 'DELETE', effect: 'GRANT' },
      ]),
    );

    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.togglePermission('ENTITY', 'organization', 'DELETE', false);

    expect(permissionService.delete).toHaveBeenCalledWith(42);
    expect(component.isGranted('organization', 'DELETE')).toBe(false);
  });

  it('restores the granted key on delete error (rollback)', () => {
    permissionService.query.mockReturnValue(
      of<ISecPermission[]>([
        { id: 77, authorityName: 'ROLE_PROOF_NONE', targetType: 'ENTITY', target: 'organization', action: 'UPDATE', effect: 'GRANT' },
      ]),
    );
    permissionService.delete.mockReturnValue(throwError(() => new Error('network error')));

    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.togglePermission('ENTITY', 'organization', 'UPDATE', false);

    expect(component.isGranted('organization', 'UPDATE')).toBe(true);
    expect(component['granted'].get('organization:UPDATE')).toBe(77);
  });

  it('ignores uncheck for a key that is not in the granted map', () => {
    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.togglePermission('ENTITY', 'organization', 'CREATE', false);

    expect(permissionService.delete).not.toHaveBeenCalled();
  });

  // ─── Pending guard ────────────────────────────────────────────────────────

  it('ignores a second toggle while the first is still pending', () => {
    // Use NEVER so the first create never completes — pending stays set
    permissionService.create.mockReturnValue(NEVER);
    const fixture = createFixture();
    const component = fixture.componentInstance;

    component.togglePermission('ENTITY', 'organization', 'READ', true);
    component.togglePermission('ENTITY', 'organization', 'READ', true); // blocked by pending

    expect(permissionService.create).toHaveBeenCalledTimes(1);
  });

  // ─── Attribute rows ───────────────────────────────────────────────────────

  it('getAttributeRows returns wildcard row plus one row per attribute', () => {
    const fixture = createFixture();
    const component = fixture.componentInstance;

    const rows = component.getAttributeRows(CATALOG_ENTRIES[0]);

    expect(rows.length).toBe(3); // wildcard + budget + name
    expect(rows[0]).toEqual({ label: 'All attributes (*)', target: 'organization.*', isWildcard: true });
    expect(rows[1]).toEqual({ label: 'budget', target: 'organization.budget', isWildcard: false });
    expect(rows[2]).toEqual({ label: 'name', target: 'organization.name', isWildcard: false });
  });

  // ─── Wildcard-granted check ───────────────────────────────────────────────

  it('treats budget VIEW as disabled when organization.* VIEW is already granted', () => {
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

    const budgetRow = component.getAttributeRows(CATALOG_ENTRIES[0]).find(row => row.target === 'organization.budget');

    expect(budgetRow).toBeDefined();
    expect(component.isWildcardGranted('organization', 'VIEW')).toBe(true);
    expect(!budgetRow!.isWildcard && component.isWildcardGranted(CATALOG_ENTRIES[0].code, 'VIEW')).toBe(true);
  });

  // ─── Empty role name guard ────────────────────────────────────────────────

  it('does not call the API when roleName is missing from the route', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [PermissionMatrixComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({}) } },
        },
        { provide: SecCatalogService, useValue: { query: () => of([]) } },
        { provide: SecPermissionService, useValue: permissionService },
      ],
    });

    const fixture = TestBed.createComponent(PermissionMatrixComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.loading).toBe(false);
    expect(permissionService.query).not.toHaveBeenCalled();
  });
});
