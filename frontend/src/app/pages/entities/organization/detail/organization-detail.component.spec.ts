import { HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { Subject, of } from 'rxjs';

import { ISecuredEntityCapability } from '../../shared/secured-entity-capability.model';
import { SecuredEntityCapabilityService } from '../../shared/service/secured-entity-capability.service';
import { OrganizationService } from '../service/organization.service';
import OrganizationDetailComponent from './organization-detail.component';

describe('OrganizationDetailComponent', () => {
  let capability$: Subject<ISecuredEntityCapability | null>;

  const organizationService = {
    find: () =>
      of(
        new HttpResponse({
          body: {
            id: 1,
            code: 'ORG-001',
            name: 'Platform',
            ownerLogin: 'admin',
          },
        }),
      ),
  };
  const router = {
    navigate: async () => true,
  };

  beforeEach(() => {
    capability$ = new Subject<ISecuredEntityCapability | null>();

    TestBed.configureTestingModule({
      imports: [OrganizationDetailComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({ id: '1' })),
          },
        },
        { provide: Router, useValue: router },
        { provide: OrganizationService, useValue: organizationService },
        {
          provide: SecuredEntityCapabilityService,
          useValue: {
            getEntityCapability: () => capability$.asObservable(),
          },
        },
      ],
    });
  });

  it('hides the Edit button when update capability is false', async () => {
    const fixture = TestBed.createComponent(OrganizationDetailComponent);

    fixture.detectChanges();
    await fixture.whenStable();
    capability$.next(buildCapability(false));
    fixture.detectChanges();

    expect(getEditButton(fixture.nativeElement as HTMLElement)).toBeNull();
  });

  it('shows the Edit button when update capability is true', async () => {
    const fixture = TestBed.createComponent(OrganizationDetailComponent);

    fixture.detectChanges();
    await fixture.whenStable();
    capability$.next(buildCapability(true));
    fixture.detectChanges();

    expect(getEditButton(fixture.nativeElement as HTMLElement)).not.toBeNull();
  });
});

function buildCapability(canUpdate: boolean): ISecuredEntityCapability {
  return {
    code: 'organization',
    canCreate: false,
    canRead: true,
    canUpdate,
    canDelete: false,
    attributes: [],
  };
}

function getEditButton(nativeElement: HTMLElement): HTMLButtonElement | null {
  return Array.from(nativeElement.querySelectorAll('button')).find(button => button.textContent?.includes('Edit')) ?? null;
}
