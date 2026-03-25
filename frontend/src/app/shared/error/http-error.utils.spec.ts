import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { TestBed } from '@angular/core/testing';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { MessageService } from 'primeng/api';

import { addTranslatedMessage, handleHttpError } from './http-error.utils';

interface FeedbackTranslations {
  feedback: {
    toast: {
      deleted: string;
    };
    httpError: {
      accessDenied: {
        summary: string;
        detail: string;
      };
      unexpected: {
        summary: string;
        detail: string;
      };
    };
    security: {
      roles: {
        loadFailed: string;
      };
    };
    entities: {
      employees: {
        deleted: string;
      };
    };
  };
}

type TranslationObject = Parameters<TranslateService['setTranslation']>[1];

describe('http-error.utils', () => {
  let translateService: TranslateService;
  let addedMessages: unknown[];
  let messageService: MessageService;
  let enTranslations: FeedbackTranslations;
  let viTranslations: FeedbackTranslations;

  beforeEach(async () => {
    addedMessages = [];

    TestBed.configureTestingModule({
      providers: [provideTranslateService({ lang: 'en', fallbackLang: 'en' })],
    });

    translateService = TestBed.inject(TranslateService);
    messageService = {
      add: (message: unknown) => addedMessages.push(message),
    } as unknown as MessageService;

    enTranslations = JSON.parse(
      readFileSync(resolve(process.cwd(), 'frontend/public/i18n/en.json'), 'utf8'),
    ) as FeedbackTranslations;
    viTranslations = JSON.parse(
      readFileSync(resolve(process.cwd(), 'frontend/public/i18n/vi.json'), 'utf8'),
    ) as FeedbackTranslations;

    translateService.setTranslation('en', enTranslations as unknown as TranslationObject);
    translateService.setTranslation('vi', viTranslations as unknown as TranslationObject);

    await firstValueFrom(translateService.use('en'));
  });

  it('ships corrected Vietnamese feedback copy in the bundle', () => {
    expect(viTranslations.feedback.toast.deleted).toBe('\u0110\u00e3 x\u00f3a');
    expect(viTranslations.feedback.httpError.accessDenied.summary).toBe(
      'T\u1eeb ch\u1ed1i truy c\u1eadp',
    );
    expect(viTranslations.feedback.entities.employees.deleted).toBe(
      '\u0110\u00e3 x\u00f3a nh\u00e2n vi\u00ean th\u00e0nh c\u00f4ng.',
    );
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
        summary: '\u004c\u1ed7i',
        detail: 'Kh\u00f4ng th\u1ec3 t\u1ea3i danh s\u00e1ch vai tr\u00f2 b\u1ea3o m\u1eadt.',
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
        summary: '\u0110\u00e3 x\u00f3a',
        detail: '\u0110\u00e3 x\u00f3a nh\u00e2n vi\u00ean th\u00e0nh c\u00f4ng.',
      },
    ]);
  });
});
