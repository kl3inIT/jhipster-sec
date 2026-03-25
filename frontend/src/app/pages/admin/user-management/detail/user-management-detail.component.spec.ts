import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { WorkspaceContextService } from 'app/pages/entities/shared/service/workspace-context.service';
import { IUser } from '../user-management.model';
import { UserManagementService } from '../service/user-management.service';
import UserManagementDetailComponent from './user-management-detail.component';

const mockUser: IUser = {
  id: 1,
  login: 'admin',
  firstName: 'Admin',
  lastName: 'User',
  email: 'admin@test.com',
  activated: true,
  langKey: 'en',
  authorities: ['ROLE_ADMIN', 'ROLE_USER'],
  createdBy: 'system',
  lastModifiedBy: 'system',
};

describe('UserManagementDetailComponent', () => {
  let component: UserManagementDetailComponent;
  let fixture: ComponentFixture<UserManagementDetailComponent>;
  let mockUserService: { authorities: ReturnType<typeof vi.fn> };
  let mockWorkspaceContextService: { get: ReturnType<typeof vi.fn>; store: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(async () => {
    mockUserService = {
      authorities: vi.fn().mockReturnValue(of(['ROLE_ADMIN', 'ROLE_USER'])),
    };
    mockWorkspaceContextService = {
      get: vi.fn().mockReturnValue({ queryParams: { page: '2', sort: 'login,asc' } }),
      store: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [UserManagementDetailComponent, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: UserManagementService, useValue: mockUserService },
        { provide: WorkspaceContextService, useValue: mockWorkspaceContextService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              data: { user: mockUser, navigationNodeId: 'security.users' },
            },
          },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(UserManagementDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should resolve the user from route data', () => {
    expect(component.user()).toEqual(mockUser);
  });

  it('should build disabled role checkbox rows', () => {
    const rows = component.roleRows();
    expect(rows.length).toBe(2);
    expect(rows[0].disabled).toBe(true);
    expect(rows[1].disabled).toBe(true);
    expect(rows.find(r => r.authority === 'ROLE_ADMIN')?.selected).toBe(true);
    expect(rows.find(r => r.authority === 'ROLE_USER')?.selected).toBe(true);
  });

  it('should use authority label translation with raw fallback', () => {
    const translateService = TestBed.inject(TranslateService);
    const rows = component.roleRows();
    // With no translations loaded, labels should fall back to authority code
    expect(rows[0].label).toBeTruthy();
    // The label should be either translated or the raw authority code
    expect(['ROLE_ADMIN', 'Administrator']).toContain(rows[0].label);
  });

  it('should navigate back to list restoring stored query params', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    component.back();
    expect(mockWorkspaceContextService.get).toHaveBeenCalledWith('security.users');
    expect(navigateSpy).toHaveBeenCalledWith(['/admin/users'], { queryParams: { page: '2', sort: 'login,asc' } });
  });

  it('should navigate to edit route', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    component.edit();
    expect(navigateSpy).toHaveBeenCalledWith(['/admin/users', 'admin', 'edit']);
  });
});
