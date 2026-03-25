import { HttpResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { NEVER, of, throwError } from 'rxjs';
import { vi } from 'vitest';

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
      ],
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

  it('onEntitySelect sets the selected entity and attribute rows', () => {
    const fixture = createFixture();
    const component = fixture.componentInstance;

    expect(component.selectedEntity).toBeNull();
    component.onEntitySelect(CATALOG_ENTRIES[0]);

    expect(component.selectedEntity).toBe(CATALOG_ENTRIES[0]);
    expect(component.selectedEntityAttributeRows).toEqual([
      { label: 'All attributes (*)', target: 'organization.*', isWildcard: true },
      { label: 'budget', target: 'organization.budget', isWildcard: false },
      { label: 'name', target: 'organization.name', isWildcard: false },
    ]);
  });

  it('renders the attribute panel after selecting an entity', () => {
    const fixture = createFixture();
    const component = fixture.componentInstance;

    expect(fixture.debugElement.nativeElement.textContent).toContain('Select an entity above');

    component.onEntitySelect(CATALOG_ENTRIES[0]);

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
      ],
    });

    const fixture = TestBed.createComponent(PermissionMatrixComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.loading).toBe(false);
    expect(permissionService.query).not.toHaveBeenCalled();
  });
});
