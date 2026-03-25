import { Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { LANGUAGE_FALLBACK } from 'app/config/language.constants';
import { IUser } from '../user-management.model';

type UserManagementFormGroupContent = {
  id: FormControl<IUser['id']>;
  login: FormControl<string>;
  firstName: FormControl<string>;
  lastName: FormControl<string>;
  email: FormControl<string>;
  activated: FormControl<boolean>;
  langKey: FormControl<string>;
  authorities: FormControl<string[]>;
};

export type UserManagementFormGroup = FormGroup<UserManagementFormGroupContent>;

const LOGIN_PATTERN = '^[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$|^[_.@A-Za-z0-9-]+$';

@Injectable({ providedIn: 'root' })
export class UserManagementFormService {
  createUserFormGroup(user: IUser | null = null): UserManagementFormGroup {
    const rawValue = this.toRawValue(user);

    return new FormGroup<UserManagementFormGroupContent>({
      id: new FormControl(rawValue.id),
      login: new FormControl(rawValue.login, {
        nonNullable: true,
        validators: [Validators.required, Validators.minLength(1), Validators.maxLength(50), Validators.pattern(LOGIN_PATTERN)],
      }),
      firstName: new FormControl(rawValue.firstName, {
        nonNullable: true,
        validators: [Validators.maxLength(50)],
      }),
      lastName: new FormControl(rawValue.lastName, {
        nonNullable: true,
        validators: [Validators.maxLength(50)],
      }),
      email: new FormControl(rawValue.email, {
        nonNullable: true,
        validators: [Validators.required, Validators.email, Validators.minLength(5), Validators.maxLength(254)],
      }),
      activated: new FormControl(rawValue.activated, {
        nonNullable: true,
      }),
      langKey: new FormControl(rawValue.langKey, {
        nonNullable: true,
        validators: [Validators.required, Validators.minLength(2), Validators.maxLength(10)],
      }),
      authorities: new FormControl(rawValue.authorities, {
        nonNullable: true,
      }),
    });
  }

  getUser(form: UserManagementFormGroup): IUser {
    const rawValue = form.getRawValue();

    return {
      id: rawValue.id,
      login: rawValue.login.trim(),
      firstName: rawValue.firstName.trim() || null,
      lastName: rawValue.lastName.trim() || null,
      email: rawValue.email.trim(),
      activated: rawValue.activated,
      langKey: rawValue.langKey.trim(),
      authorities: [...rawValue.authorities],
    };
  }

  resetForm(form: UserManagementFormGroup, user: IUser | null): void {
    form.reset(this.toRawValue(user));
  }

  private toRawValue(user: IUser | null): {
    id: number | null;
    login: string;
    firstName: string;
    lastName: string;
    email: string;
    activated: boolean;
    langKey: string;
    authorities: string[];
  } {
    return {
      id: user?.id ?? null,
      login: user?.login ?? '',
      firstName: user?.firstName ?? '',
      lastName: user?.lastName ?? '',
      email: user?.email ?? '',
      activated: user?.activated ?? true,
      langKey: user?.langKey ?? LANGUAGE_FALLBACK,
      authorities: [...(user?.authorities ?? [])],
    };
  }
}
