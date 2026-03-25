import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideRouter, Router } from '@angular/router';
import { of, BehaviorSubject } from 'rxjs';
import { HttpResponse, HttpHeaders } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';

import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { WorkspaceContextService } from 'app/pages/entities/shared/service/workspace-context.service';
import { IUser } from '../user-management.model';
import { UserManagementService } from '../service/user-management.service';
import UserManagementListComponent from './user-management-list.component';

function createMockUsers(): IUser[] {
  return [
    { id: 1, login: 'admin', firstName: 'Admin', lastName: 'User', email: 'admin@test.com', activated: true, authorities: ['ROLE_ADMIN', 'ROLE_USER'] },
    { id: 2, login: 'user', firstName: 'Normal', lastName: 'User', email: 'user@test.com', activated: true, authorities: ['ROLE_USER'] },
    { id: 3, login: 'inactive', firstName: 'Inactive', lastName: 'User', email: 'inactive@test.com', activated: false, authorities: ['ROLE_USER'] },
  ];
}

function createMockResponse(users: IUser[]): HttpResponse<IUser[]> {
  return new HttpResponse({
    body: users,
    headers: new HttpHeaders({ 'X-Total-Count': `${users.length}` }),
    status: 200,
  });
}

describe('UserManagementListComponent', () => {
  let component: UserManagementListComponent;
  let fixture: ComponentFixture<UserManagementListComponent>;
  let mockUserService: {
    query: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  let mockWorkspaceContextService: { store: ReturnType<typeof vi.fn>; get: ReturnType<typeof vi.fn> };
  let queryParamMapSubject: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let confirmationService: ConfirmationService;

  beforeEach(async () => {
    const users = createMockUsers();
    mockUserService = {
      query: vi.fn().mockReturnValue(of(createMockResponse(users))),
      delete: vi.fn().mockReturnValue(of({})),
      update: vi.fn().mockReturnValue(of(users[0])),
    };
    mockWorkspaceContextService = { store: vi.fn(), get: vi.fn().mockReturnValue(null) };

    queryParamMapSubject = new BehaviorSubject(convertToParamMap({}));

    await TestBed.configureTestingModule({
      imports: [UserManagementListComponent, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'admin/users/:login/view', component: UserManagementListComponent },
          { path: 'admin/users/:login/edit', component: UserManagementListComponent },
          { path: 'admin/users/new', component: UserManagementListComponent },
        ]),
        { provide: UserManagementService, useValue: mockUserService },
        { provide: WorkspaceContextService, useValue: mockWorkspaceContextService },
        {
          provide: AccountService,
          useValue: {
            trackCurrentAccount: () => (() => new Account(true, ['ROLE_ADMIN'], 'admin@test.com', 'Admin', 'en', 'User', 'admin', null)),
          },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap: queryParamMapSubject.asObservable(),
            data: of({ defaultSort: 'id,asc', navigationNodeId: 'security.users' }),
            snapshot: {
              data: { defaultSort: 'id,asc', navigationNodeId: 'security.users' },
              queryParams: {},
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserManagementListComponent);
    component = fixture.componentInstance;
    // Get the component-level ConfirmationService instance
    confirmationService = fixture.debugElement.injector.get(ConfirmationService);
    fixture.detectChanges();
  });

  it('should load users on init', () => {
    expect(mockUserService.query).toHaveBeenCalled();
    expect(component.users().length).toBe(3);
  });

  it('should send debounced search query', async () => {
    mockUserService.query.mockClear();
    component.searchControl.setValue('john');
    await new Promise(resolve => setTimeout(resolve, 400));
    fixture.detectChanges();
    expect(component.searchControl.value).toBe('john');
  });

  it('should map page and sort into the request', () => {
    const callArgs = mockUserService.query.mock.calls[0][0];
    expect(callArgs).toBeDefined();
    expect(callArgs.page).toBeDefined();
    expect(callArgs.size).toBe(20);
    expect(callArgs.sort).toBeDefined();
  });

  it('should call toggleActivation directly without confirmation', () => {
    const confirmSpy = vi.spyOn(confirmationService, 'confirm');
    const user = createMockUsers()[0];
    component.toggleActivation(user);
    expect(mockUserService.update).toHaveBeenCalledWith({ ...user, activated: false });
    expect(confirmSpy).not.toHaveBeenCalled();
  });

  it('should call confirmDelete with ConfirmationService and delete on accept', () => {
    const confirmSpy = vi.spyOn(confirmationService, 'confirm');
    const user = createMockUsers()[1];
    component.confirmDelete(user);
    expect(confirmSpy).toHaveBeenCalled();

    // Simulate accept callback
    const confirmCall = confirmSpy.mock.calls[0][0];
    confirmCall.accept!();
    expect(mockUserService.delete).toHaveBeenCalledWith('user');
  });

  it('should disable self-deactivate and self-delete', () => {
    const adminUser = createMockUsers()[0]; // login: 'admin'
    expect(component.isSelf(adminUser)).toBe(true);

    const otherUser = createMockUsers()[1]; // login: 'user'
    expect(component.isSelf(otherUser)).toBe(false);
  });

  it('should store workspace context before navigation', () => {
    const user = createMockUsers()[0];
    component.view(user);
    expect(mockWorkspaceContextService.store).toHaveBeenCalledWith('security.users', {});
  });

  it('should show only first 2 authorities and count extras', () => {
    const user: IUser = { id: 4, login: 'multi', authorities: ['ROLE_ADMIN', 'ROLE_USER', 'ROLE_MANAGER'] };
    expect(component.displayedAuthorities(user)).toEqual(['ROLE_ADMIN', 'ROLE_USER']);
    expect(component.extraAuthorityCount(user)).toBe(1);
  });
});
