import { HttpErrorResponse } from '@angular/common/http';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';

import { RegisterRequest, RegisterService } from './register.service';

const PROBLEM_BASE_URL = 'https://www.jhipster.tech/problem';
const EMAIL_ALREADY_USED_TYPE = `${PROBLEM_BASE_URL}/email-already-used`;
const LOGIN_ALREADY_USED_TYPE = `${PROBLEM_BASE_URL}/login-already-used`;
const LOGIN_PATTERN =
  '^[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$|^[_.@A-Za-z0-9-]+$';

@Component({
  selector: 'app-register',
  imports: [
    ReactiveFormsModule,
    RouterModule,
    TranslatePipe,
    CardModule,
    ButtonModule,
    InputTextModule,
    PasswordModule,
    MessageModule,
  ],
  templateUrl: './register.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class RegisterComponent implements AfterViewInit {
  loginInput = viewChild.required<ElementRef<HTMLInputElement>>('loginInput');

  readonly doNotMatch = signal(false);
  readonly error = signal(false);
  readonly errorEmailExists = signal(false);
  readonly errorUserExists = signal(false);
  readonly isSaving = signal(false);
  readonly success = signal(false);

  readonly registerForm = new FormGroup({
    login: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(1),
        Validators.maxLength(50),
        Validators.pattern(LOGIN_PATTERN),
      ],
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(5),
        Validators.maxLength(254),
        Validators.email,
      ],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(4), Validators.maxLength(100)],
    }),
    confirmPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(4), Validators.maxLength(100)],
    }),
  });

  private readonly registerService = inject(RegisterService);
  private readonly translateService = inject(TranslateService);

  ngAfterViewInit(): void {
    this.loginInput().nativeElement.focus();
  }

  register(): void {
    this.registerForm.markAllAsTouched();
    this.resetErrors();

    if (this.registerForm.invalid) {
      return;
    }

    const { password, confirmPassword } = this.registerForm.getRawValue();
    if (password !== confirmPassword) {
      this.doNotMatch.set(true);
      return;
    }

    this.isSaving.set(true);
    this.registerService
      .register(this.createRequest())
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe({
        next: () => this.success.set(true),
        error: (response) => this.processError(response),
      });
  }

  private createRequest(): RegisterRequest {
    const { login, email, password } = this.registerForm.getRawValue();

    return {
      login,
      email,
      password,
      langKey: this.translateService.currentLang || 'en',
    };
  }

  private processError(response: HttpErrorResponse): void {
    const errorType = this.extractErrorType(response);

    if (response.status === 400 && errorType === LOGIN_ALREADY_USED_TYPE) {
      this.errorUserExists.set(true);
      return;
    }
    if (response.status === 400 && errorType === EMAIL_ALREADY_USED_TYPE) {
      this.errorEmailExists.set(true);
      return;
    }
    this.error.set(true);
  }

  private extractErrorType(response: HttpErrorResponse): string | undefined {
    const { error } = response;
    if (typeof error !== 'object' || error === null || !('type' in error)) {
      return undefined;
    }

    const { type } = error as { type?: unknown };
    return typeof type === 'string' ? type : undefined;
  }

  private resetErrors(): void {
    this.doNotMatch.set(false);
    this.error.set(false);
    this.errorEmailExists.set(false);
    this.errorUserExists.set(false);
  }
}
