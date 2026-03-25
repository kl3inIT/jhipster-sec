import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { SortService } from 'app/shared/sort/sort.service';
import { IOrganization } from '../organization.model';
import { OrganizationService } from '../service/organization.service';
import OrganizationListComponent from './organization-list.component';
import { ISecuredEntityCapability } from '../../shared/secured-entity-capability.model';

describe('OrganizationListComponent', () => {
  const organization: IOrganization = {
    id: 1,
    code: 'ORG-001',
    name: 'Platform',
    ownerLogin: 'admin',
  };

  const restrictiveCapability: ISecuredEntityCapability = {
    code: 'organization',
    canCreate: false,
    canRead: true,
    canUpdate: false,
    canDelete: false,
    attributes: [],
  };

  const organizationService = {
    query: () =>
      of(
        new HttpResponse<IOrganization[]>({
          body: [organization],
          headers: new HttpHeaders({ 'X-Total-Count': '1' }),
        }),
      ),
    getOrganizationIdentifier: (item: Pick<IOrganization, 'id'>) => item.id ?? 0,
    delete: () => of(new HttpResponse({ status: 204 })),
  };

  const sortService = {
    parseSortParam: () => ({ predicate: 'id', order: 'asc' as const }),
    buildSortParam: () => ['id,asc'],
  };

  const router = {
    navigate: async () => true,
  };

  let routeSnapshot: { data: { capability: ISecuredEntityCapability | null } };

  beforeEach(() => {
    routeSnapshot = { data: { capability: restrictiveCapability } };

    TestBed.configureTestingModule({
      imports: [OrganizationListComponent],
      providers: [
        provideTranslateService({ lang: 'en', fallbackLang: 'en' }),
        { provide: OrganizationService, useValue: organizationService },
        { provide: SortService, useValue: sortService },
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: routeSnapshot,
            queryParamMap: of(convertToParamMap({})),
            data: of({ defaultSort: 'id,asc' }),
          },
        },
      ],
    });
  });

  it('shows only the View action when capability denies create, edit, and delete', async () => {
    const fixture = TestBed.createComponent(OrganizationListComponent);

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const nativeElement = fixture.nativeElement as HTMLElement;
    const actionLabels = Array.from(nativeElement.querySelectorAll('button[aria-label]')).map(
      (button) => button.getAttribute('aria-label'),
    );

    expect(nativeElement.textContent).not.toContain('New Organization');
    expect(actionLabels).toContain('View');
    expect(actionLabels).not.toContain('Edit');
    expect(actionLabels).not.toContain('Delete');
  });

  it('renders no action buttons while capability is still null', async () => {
    routeSnapshot.data.capability = null;
    const fixture = TestBed.createComponent(OrganizationListComponent);

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const nativeElement = fixture.nativeElement as HTMLElement;
    expect(nativeElement.textContent).not.toContain('New Organization');
    expect(nativeElement.querySelector('button[aria-label="View"]')).toBeNull();
    expect(nativeElement.querySelector('button[aria-label="Edit"]')).toBeNull();
    expect(nativeElement.querySelector('button[aria-label="Delete"]')).toBeNull();
  });
});
