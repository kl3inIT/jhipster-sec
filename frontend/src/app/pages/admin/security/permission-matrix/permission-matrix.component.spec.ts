import { HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';

import { ISecCatalogEntry } from '../shared/sec-catalog.model';
import { ISecPermission } from '../shared/sec-permission.model';
import { SecCatalogService } from '../shared/service/sec-catalog.service';
import { SecPermissionService } from '../shared/service/sec-permission.service';
import PermissionMatrixComponent from './permission-matrix.component';

describe('PermissionMatrixComponent', () => {
  const catalogEntries: ISecCatalogEntry[] = [
    {
      code: 'organization',
      displayName: 'Organization',
      operations: ['CREATE', 'READ', 'UPDATE', 'DELETE'],
      attributes: ['budget'],
    },
  ];

  let permissionService: {
    query: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };

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
            query: () => of(catalogEntries),
          },
        },
        {
          provide: SecPermissionService,
          useValue: permissionService,
        },
      ],
    });
  });

  it('calls create with the locked GRANT UI contract for entity READ toggles', () => {
    const fixture = TestBed.createComponent(PermissionMatrixComponent);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    component.togglePermission('ENTITY', 'organization', 'READ', true);

    expect(permissionService.create).toHaveBeenCalledWith({
      authorityName: 'ROLE_PROOF_NONE',
      targetType: 'ENTITY',
      target: 'organization',
      action: 'READ',
      effect: 'GRANT',
    });
  });

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

    const fixture = TestBed.createComponent(PermissionMatrixComponent);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    component.onEntitySelect(catalogEntries[0]);

    const budgetRow = component.getAttributeRows(catalogEntries[0]).find(row => row.target === 'organization.budget');

    expect(budgetRow).toBeDefined();
    expect(component.isWildcardGranted('organization', 'VIEW')).toBe(true);
    expect(!budgetRow!.isWildcard && component.isWildcardGranted(catalogEntries[0].code, 'VIEW')).toBe(true);
  });
});

