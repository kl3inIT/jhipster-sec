import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';

import { AppMenu } from './app.menu';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';

const mockAdminAccount: Account = new Account(true, ['ROLE_ADMIN', 'ROLE_USER'], 'admin@test.com', 'Admin', 'en', 'User', 'admin', null);
const mockUserAccount: Account = new Account(true, ['ROLE_USER'], 'user@test.com', 'Regular', 'en', 'User', 'user', null);

describe('AppMenu', () => {
  let mockAccountService: Partial<AccountService>;

  function setupWithAccount(account: Account | null, isAdmin: boolean) {
    mockAccountService = {
      getAuthenticationState: () => of(account),
      hasAnyAuthority: (_authorities: string[] | string) => isAdmin,
    };

    TestBed.configureTestingModule({
      imports: [AppMenu],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        { provide: AccountService, useValue: mockAccountService },
      ],
    });
  }

  it('should show Security Admin section when user has ROLE_ADMIN', () => {
    setupWithAccount(mockAdminAccount, true);
    const fixture = TestBed.createComponent(AppMenu);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    const securityAdminSection = component.model.find(item => item.label === 'Security Admin');
    expect(securityAdminSection).toBeTruthy();
    expect(securityAdminSection?.items?.some(item => item.label === 'Roles & Permissions')).toBe(true);
    expect(securityAdminSection?.items?.some(item => item.label === 'Row Policies')).toBe(true);
  });

  it('should hide Security Admin section when user is not admin', () => {
    setupWithAccount(mockUserAccount, false);
    const fixture = TestBed.createComponent(AppMenu);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    const securityAdminSection = component.model.find(item => item.label === 'Security Admin');
    expect(securityAdminSection).toBeUndefined();
  });

  it('should always show Entities section with Organizations, Departments, Employees', () => {
    setupWithAccount(mockUserAccount, false);
    const fixture = TestBed.createComponent(AppMenu);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    const entitiesSection = component.model.find(item => item.label === 'Entities');
    expect(entitiesSection).toBeTruthy();
    expect(entitiesSection?.items?.some(item => item.label === 'Organizations')).toBe(true);
    expect(entitiesSection?.items?.some(item => item.label === 'Departments')).toBe(true);
    expect(entitiesSection?.items?.some(item => item.label === 'Employees')).toBe(true);
  });
});
