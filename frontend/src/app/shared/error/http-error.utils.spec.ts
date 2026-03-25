import { TestBed } from '@angular/core/testing';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { MessageService } from 'primeng/api';

import { addTranslatedMessage, handleHttpError } from './http-error.utils';

describe('http-error.utils', () => {
  let translateService: TranslateService;
  let addedMessages: unknown[];
  let messageService: MessageService;

  beforeEach(async () => {
    addedMessages = [];

    TestBed.configureTestingModule({
      providers: [provideTranslateService({ lang: 'en', fallbackLang: 'en' })],
    });

    translateService = TestBed.inject(TranslateService);
    messageService = {
      add: (message: unknown) => addedMessages.push(message),
    } as unknown as MessageService;

    translateService.setTranslation('en', {
      feedback: {
        toast: {
          deleted: 'Deleted',
        },
        httpError: {
          accessDenied: {
            summary: 'Access denied',
            detail: 'You do not have permission to perform this action.',
          },
          unexpected: {
            summary: 'Error',
            detail: 'An unexpected error occurred. Please try again.',
          },
        },
        security: {
          roles: {
            loadFailed: 'Failed to load roles.',
          },
        },
        entities: {
          employees: {
            deleted: 'Employee deleted successfully.',
          },
        },
      },
    });
    translateService.setTranslation('vi', {
      feedback: {
        toast: {
          deleted: 'Da xoa',
        },
        httpError: {
          accessDenied: {
            summary: 'Tu choi truy cap',
            detail: 'Ban khong co quyen thuc hien hanh dong nay.',
          },
          unexpected: {
            summary: 'Loi',
            detail: 'Da xay ra loi khong mong muon. Vui long thu lai.',
          },
        },
        security: {
          roles: {
            loadFailed: 'Khong the tai danh sach vai tro bao mat.',
          },
        },
        entities: {
          employees: {
            deleted: 'Da xoa nhan vien thanh cong.',
          },
        },
      },
    });

    await firstValueFrom(translateService.use('en'));
  });

  it('translates access denied feedback in English', () => {
    handleHttpError(messageService, translateService, { status: 403 });

    expect(addedMessages).toEqual([
      {
        severity: 'warn',
        summary: 'Access denied',
        detail: 'You do not have permission to perform this action.',
      },
    ]);
  });

  it('translates fallback error details in Vietnamese', async () => {
    await firstValueFrom(translateService.use('vi'));

    handleHttpError(
      messageService,
      translateService,
      { status: 500 },
      'feedback.security.roles.loadFailed',
    );

    expect(addedMessages).toEqual([
      {
        severity: 'error',
        summary: 'Loi',
        detail: 'Khong the tai danh sach vai tro bao mat.',
      },
    ]);
  });

  it('builds translated success feedback with the active locale', async () => {
    await firstValueFrom(translateService.use('vi'));

    addTranslatedMessage(messageService, translateService, {
      severity: 'success',
      summary: 'feedback.toast.deleted',
      detail: 'feedback.entities.employees.deleted',
    });

    expect(addedMessages).toEqual([
      {
        severity: 'success',
        summary: 'Da xoa',
        detail: 'Da xoa nhan vien thanh cong.',
      },
    ]);
  });
});
