import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { provideRouter, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';

import { WorkspaceContextService } from 'app/pages/entities/shared/service/workspace-context.service';
import { IUser } from '../user-management.model';
import { UserManagementService } from '../service/user-management.service';
import UserManagementUpdateComponent from './user-management-update.component';

describe('UserManagementUpdateComponent', () => {
  let component: UserManagementUpdateComponent;
  let fixture: ComponentFixture<UserManagementUpdateComponent>;
  let router: Router;
  let routeData: { user: IUser | null; navigationNodeId: string };
  let mockUserManagementService: {
    authorities: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };

  const existingUser: IUser = {
    id: 7,
    login: 'managed-user',
    firstName: 'Managed',
    lastName: 'User',
    email: 'managed@example.com',
    activated: true,
    langKey: 'en',
    authorities: ['ROLE_USER'],
  };

  async function setup(user: IUser | null = null): Promise<void> {
    routeData = {
      user,
      navigationNodeId: 'security.users',
    };
    mockUserManagementService = {
      authorities: vi.fn().mockReturnValue(of(['ROLE_ADMIN', 'ROLE_USER'])),
      create: vi.fn().mockImplementation((payload: IUser) => of({ ...payload, login: payload.login ?? 'created-user' })),
      update: vi.fn().mockImplementation((payload: IUser) => of(payload)),
    };

    await TestBed.configureTestingModule({
      imports: [UserManagementUpdateComponent, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'admin/users', component: UserManagementUpdateComponent },
          { path: 'admin/users/:login/view', component: UserManagementUpdateComponent },
        ]),
        { provide: UserManagementService, useValue: mockUserManagementService },
        {
          provide: WorkspaceContextService,
          useValue: {
            get: vi.fn().mockReturnValue({ queryParams: { page: '3', sort: 'login,asc' } }),
          },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              get data() {
                return routeData;
              },
            },
          },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(UserManagementUpdateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function fillRequiredFields(): void {
    component.editForm.controls.login.setValue('edited-user');
    component.editForm.controls.email.setValue('edited@example.com');
    component.editForm.controls.langKey.setValue('en');
  }

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  it('calls service.create for the create flow', async () => {
    await setup();
    const navigateSpy = vi.spyOn(router, 'navigate');

    fillRequiredFields();
    component.toggleAuthority('ROLE_ADMIN', true);
    component.save();

    expect(mockUserManagementService.create).toHaveBeenCalledWith(
      expect.objectContaining({
        id: null,
        login: 'edited-user',
        email: 'edited@example.com',
        authorities: ['ROLE_ADMIN'],
      }),
    );
    expect(navigateSpy).toHaveBeenCalledWith(['/admin/users', 'edited-user', 'view']);
  });

  it('calls service.update for the edit flow', async () => {
    await setup(existingUser);

    component.editForm.controls.firstName.setValue('Updated');
    component.save();

    expect(mockUserManagementService.update).toHaveBeenCalledWith(
      expect.objectContaining({
        id: 7,
        login: 'managed-user',
        firstName: 'Updated',
      }),
    );
  });

  it('updates the saved authorities payload when a checkbox selection changes', async () => {
    await setup();

    fillRequiredFields();
    component.toggleAuthority('ROLE_ADMIN', true);
    component.toggleAuthority('ROLE_USER', true);
    component.save();

    expect(mockUserManagementService.create).toHaveBeenCalledWith(
      expect.objectContaining({
        authorities: ['ROLE_ADMIN', 'ROLE_USER'],
      }),
    );
  });

  it('navigates to the saved user detail route after save success', async () => {
    await setup(existingUser);
    const navigateSpy = vi.spyOn(router, 'navigate');

    component.save();

    expect(navigateSpy).toHaveBeenCalledWith(['/admin/users', 'managed-user', 'view']);
  });

  it('leaves the form visible when save fails', async () => {
    await setup();
    const navigateSpy = vi.spyOn(router, 'navigate');
    mockUserManagementService.create.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Server Error' })),
    );

    fillRequiredFields();
    component.save();
    fixture.detectChanges();

    expect(component.isSaving()).toBe(false);
    expect(navigateSpy).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('userManagement.form.sectionTitle');
  });
});
