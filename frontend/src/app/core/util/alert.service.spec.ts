import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';

import { AlertService } from './alert.service';

describe('AlertService', () => {
  let service: AlertService;
  let translateService: TranslateService;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideTranslateService({ lang: 'en', fallbackLang: 'en' }), AlertService],
    });

    service = TestBed.inject(AlertService);
    translateService = TestBed.inject(TranslateService);

    translateService.setTranslation('en', {
      userManagement: {
        created: 'User {{param}} created',
      },
      security: {
        alert: {
          secRole: {
            created: 'Security role {{param}} created',
          },
        },
      },
    });
    await firstValueFrom(translateService.use('en'));
  });

  it('resolves donor translation keys into alert messages', () => {
    const alert = service.addAlert({
      type: 'success',
      translationKey: 'userManagement.created',
      translationParams: { param: 'alice' },
      timeout: 0,
    });

    expect(alert.message).toBe('User alice created');
  });

  it('resolves app-specific security translation keys and allows dismissal', () => {
    const alert = service.addAlert({
      type: 'success',
      translationKey: 'security.alert.secRole.created',
      translationParams: { param: 'SEC_AUDITOR' },
      timeout: 0,
    });

    expect(alert.message).toBe('Security role SEC_AUDITOR created');
    expect(service.get()).toHaveLength(1);

    alert.close?.(service.get());

    expect(service.get()).toHaveLength(0);
  });
});
