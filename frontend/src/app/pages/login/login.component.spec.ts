import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom, of } from 'rxjs';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';

import { AccountService } from 'app/core/auth/account.service';
import { AuthServerProvider } from 'app/core/auth/auth-jwt.service';
import { SecuredEntityCapabilityService } from 'app/pages/entities/shared/service/secured-entity-capability.service';
import LoginComponent from './login.component';

describe('LoginComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        provideTranslateService({ lang: 'vi', fallbackLang: 'en' }),
        {
          provide: AccountService,
          useValue: {
            identity: () => of(null),
            isAuthenticated: () => false,
          },
        },
        {
          provide: AuthServerProvider,
          useValue: {
            login: vi.fn().mockReturnValue(of(void 0)),
          },
        },
        {
          provide: SecuredEntityCapabilityService,
          useValue: {
            query: vi.fn().mockReturnValue(of([])),
          },
        },
      ],
    });

    const translateService = TestBed.inject(TranslateService);
    translateService.setTranslation('vi', {
      login: {
        title: 'Dang nhap',
        form: {
          password: 'Mat khau',
          rememberme: 'Ghi nho dang nhap',
          button: 'Dang nhap',
        },
      },
      global: {
        form: {
          username: {
            label: 'Ten tai khoan',
            placeholder: 'Nhap ten tai khoan cua ban',
          },
        },
        messages: {
          info: {
            register: {
              noaccount: 'Ban van chua co tai khoan?',
              link: 'Dang ky tai khoan moi',
            },
          },
        },
      },
    });
  });

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  it('renders distinct register prompt text and CTA in the footer', async () => {
    const fixture = TestBed.createComponent(LoginComponent);
    await firstValueFrom(TestBed.inject(TranslateService).use('vi'));
    fixture.detectChanges();

    const nativeElement = fixture.nativeElement as HTMLElement;
    const footer = nativeElement.querySelector('.border-top-1');

    expect(footer?.textContent).toContain('Ban van chua co tai khoan?');
    expect(footer?.textContent).toContain('Dang ky tai khoan moi');
    expect(footer?.textContent).not.toContain('Dang kyDang ky');
  });
});
