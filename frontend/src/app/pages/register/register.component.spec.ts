import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import RegisterComponent from './register.component';
import { RegisterService } from './register.service';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let mockRegisterService: {
    register: ReturnType<typeof vi.fn>;
  };

  async function setup(): Promise<void> {
    mockRegisterService = {
      register: vi.fn().mockReturnValue(of(void 0)),
    };

    await TestBed.configureTestingModule({
      imports: [RegisterComponent, TranslateModule.forRoot()],
      providers: [provideRouter([]), { provide: RegisterService, useValue: mockRegisterService }],
    }).compileComponents();

    TestBed.inject(TranslateService).use('en');
    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  it('should register a new user', async () => {
    await setup();

    component.registerForm.setValue({
      login: 'new-user',
      email: 'new-user@example.com',
      password: 'password',
      confirmPassword: 'password',
    });

    component.register();

    expect(mockRegisterService.register).toHaveBeenCalledWith({
      login: 'new-user',
      email: 'new-user@example.com',
      password: 'password',
      langKey: 'en',
    });
    expect(component.success()).toBe(true);
  });

  it('blocks submit when the form is invalid', async () => {
    await setup();

    component.register();

    expect(mockRegisterService.register).not.toHaveBeenCalled();
    expect(component.registerForm.controls.login.touched).toBe(true);
    expect(component.registerForm.invalid).toBe(true);
  });

  it('blocks submit when passwords do not match', async () => {
    await setup();

    component.registerForm.setValue({
      login: 'new-user',
      email: 'new-user@example.com',
      password: 'password',
      confirmPassword: 'different',
    });

    component.register();

    expect(mockRegisterService.register).not.toHaveBeenCalled();
    expect(component.doNotMatch()).toBe(true);
  });
});
